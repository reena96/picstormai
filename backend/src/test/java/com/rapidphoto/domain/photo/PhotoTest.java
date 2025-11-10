package com.rapidphoto.domain.photo;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class PhotoTest {

    @Test
    void shouldInitiatePhotoUpload() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024000);

        assertThat(photo.getId()).isNotNull();
        assertThat(photo.getUserId()).isEqualTo(userId);
        assertThat(photo.getSessionId()).isEqualTo(sessionId);
        assertThat(photo.getFilename()).isEqualTo("photo.jpg");
        assertThat(photo.getFileSize()).isEqualTo(1024000);
        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.PENDING);
        assertThat(photo.getProgress()).isEqualTo(0);
        assertThat(photo.getCreatedAt()).isNotNull();
        assertThat(photo.isDeleted()).isFalse();
    }

    @Test
    void shouldRejectNullUserId() {
        assertThatThrownBy(() -> Photo.initiate(null, UUID.randomUUID(), "photo.jpg", 1024000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User ID cannot be null");
    }

    @Test
    void shouldRejectEmptyFilename() {
        assertThatThrownBy(() -> Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "", 1024000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Filename cannot be empty");
    }

    @Test
    void shouldRejectInvalidFileSize() {
        assertThatThrownBy(() -> Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File size must be positive");

        assertThatThrownBy(() -> Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File size must be positive");
    }

    @Test
    void shouldStartUploadWithS3Location() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg", "v123");

        photo.startUpload(location);

        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.UPLOADING);
        assertThat(photo.getS3Location()).isNotNull();
    }

    @Test
    void shouldRejectStartUploadFromNonPendingState() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);

        assertThatThrownBy(() -> photo.startUpload(location))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Can only start upload from PENDING state");
    }

    @Test
    void shouldUpdateProgress() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);

        photo.updateProgress(50);

        assertThat(photo.getProgress()).isEqualTo(50);
    }

    @Test
    void shouldEnforceProgressBounds() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);

        assertThatThrownBy(() -> photo.updateProgress(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Progress must be between 0 and 100");

        assertThatThrownBy(() -> photo.updateProgress(101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Progress must be between 0 and 100");

        // Boundary values should work
        photo.updateProgress(0);
        assertThat(photo.getProgress()).isEqualTo(0);

        photo.updateProgress(100);
        assertThat(photo.getProgress()).isEqualTo(100);
    }

    @Test
    void shouldCompleteUpload() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("camera", "Canon EOS R5");
        metadata.put("width", 6000);
        metadata.put("height", 4000);

        photo.completeUpload(metadata);

        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.COMPLETED);
        assertThat(photo.getProgress()).isEqualTo(100);
        assertThat(photo.getMetadata()).containsEntry("camera", "Canon EOS R5");
    }

    @Test
    void shouldCompleteUploadWithoutMetadata() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);

        photo.completeUpload(null);

        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.COMPLETED);
    }

    @Test
    void shouldRejectCompleteFromNonUploadingState() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);

        assertThatThrownBy(() -> photo.completeUpload(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Can only complete from UPLOADING state");
    }

    @Test
    void shouldFailUpload() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);

        photo.failUpload("Network error");

        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.FAILED);
        assertThat(photo.getMetadata()).containsEntry("errorMessage", "Network error");
    }

    @Test
    void shouldRejectFailingCompletedUpload() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);
        photo.completeUpload(null);

        assertThatThrownBy(() -> photo.failUpload("Error"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot fail already completed upload");
    }

    @Test
    void shouldRetryFailedUpload() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);
        photo.failUpload("Network error");

        photo.retry();

        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.PENDING);
        assertThat(photo.getProgress()).isEqualTo(0);
        assertThat(photo.getMetadata()).doesNotContainKey("errorMessage");
    }

    @Test
    void shouldRejectRetryFromNonFailedState() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);

        assertThatThrownBy(() -> photo.retry())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Can only retry from FAILED state");
    }

    @Test
    void shouldAddTag() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        UUID tagId = UUID.randomUUID();

        photo.addTag(tagId);

        assertThat(photo.getTagIds()).contains(tagId);
    }

    @Test
    void shouldRemoveTag() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        UUID tagId = UUID.randomUUID();
        photo.addTag(tagId);

        photo.removeTag(tagId);

        assertThat(photo.getTagIds()).doesNotContain(tagId);
    }

    @Test
    void shouldSoftDelete() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);

        assertThat(photo.isDeleted()).isFalse();

        photo.softDelete();

        assertThat(photo.isDeleted()).isTrue();
        assertThat(photo.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldBeIdempotentWhenSoftDeleting() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);

        photo.softDelete();
        photo.softDelete(); // Should not throw

        assertThat(photo.isDeleted()).isTrue();
    }

    @Test
    void shouldRestoreSoftDeletedPhoto() {
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        photo.softDelete();

        photo.restore();

        assertThat(photo.isDeleted()).isFalse();
        assertThat(photo.getDeletedAt()).isNull();
    }

    @Test
    void shouldHandleFullLifecycle() {
        // Initiate
        Photo photo = Photo.initiate(UUID.randomUUID(), UUID.randomUUID(), "photo.jpg", 1024000);
        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.PENDING);

        // Start upload
        S3Location location = S3Location.of("my-bucket", "photos/photo.jpg");
        photo.startUpload(location);
        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.UPLOADING);

        // Update progress
        photo.updateProgress(25);
        assertThat(photo.getProgress()).isEqualTo(25);

        photo.updateProgress(75);
        assertThat(photo.getProgress()).isEqualTo(75);

        // Complete upload
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("camera", "iPhone 13");
        photo.completeUpload(metadata);
        assertThat(photo.getUploadStatus()).isEqualTo(PhotoStatus.COMPLETED);
        assertThat(photo.getProgress()).isEqualTo(100);

        // Add tags
        photo.addTag(UUID.randomUUID());
        photo.addTag(UUID.randomUUID());
        assertThat(photo.getTagIds()).hasSize(2);
    }
}
