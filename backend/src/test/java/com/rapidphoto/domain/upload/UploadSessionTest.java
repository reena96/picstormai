package com.rapidphoto.domain.upload;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class UploadSessionTest {

    @Test
    void shouldStartNewUploadSession() {
        UUID userId = UUID.randomUUID();
        UploadSession session = UploadSession.start(userId);

        assertThat(session.getId()).isNotNull();
        assertThat(session.getUserId()).isEqualTo(userId);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getTotalPhotos()).isEqualTo(0);
        assertThat(session.getCompletedPhotos()).isEqualTo(0);
        assertThat(session.getFailedPhotos()).isEqualTo(0);
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getCompletedAt()).isNull();
        assertThat(session.isActive()).isTrue();
    }

    @Test
    void shouldRejectNullUserId() {
        assertThatThrownBy(() -> UploadSession.start(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User ID cannot be null");
    }

    @Test
    void shouldSetTotalPhotos() {
        UploadSession session = UploadSession.start(UUID.randomUUID());

        session.setTotalPhotos(10);

        assertThat(session.getTotalPhotos()).isEqualTo(10);
    }

    @Test
    void shouldRejectNegativeTotalPhotos() {
        UploadSession session = UploadSession.start(UUID.randomUUID());

        assertThatThrownBy(() -> session.setTotalPhotos(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Total photos cannot be negative");
    }

    @Test
    void shouldRejectSettingTotalPhotosOnCompletedSession() {
        UploadSession session = UploadSession.start(UUID.randomUUID());
        session.complete();

        assertThatThrownBy(() -> session.setTotalPhotos(10))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot set total photos on completed session");
    }

    @Test
    void shouldRecordPhotoUploaded() {
        UploadSession session = UploadSession.start(UUID.randomUUID());
        session.setTotalPhotos(5);

        session.recordPhotoUploaded();
        session.recordPhotoUploaded();

        assertThat(session.getCompletedPhotos()).isEqualTo(2);
    }

    @Test
    void shouldRejectRecordPhotoOnCompletedSession() {
        UploadSession session = UploadSession.start(UUID.randomUUID());
        session.complete();

        assertThatThrownBy(() -> session.recordPhotoUploaded())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot record photo on non-active session");
    }

    @Test
    void shouldRecordPhotoFailed() {
        UploadSession session = UploadSession.start(UUID.randomUUID());

        session.recordPhotoFailed();
        session.recordPhotoFailed();

        assertThat(session.getFailedPhotos()).isEqualTo(2);
    }

    @Test
    void shouldCalculateProgressPercentage() {
        UploadSession session = UploadSession.start(UUID.randomUUID());
        session.setTotalPhotos(10);

        assertThat(session.getProgressPercentage()).isEqualTo(0);

        session.recordPhotoUploaded();
        assertThat(session.getProgressPercentage()).isEqualTo(10);

        session.recordPhotoUploaded();
        session.recordPhotoUploaded();
        assertThat(session.getProgressPercentage()).isEqualTo(30);

        for (int i = 0; i < 7; i++) {
            session.recordPhotoUploaded();
        }
        assertThat(session.getProgressPercentage()).isEqualTo(100);
    }

    @Test
    void shouldReturnZeroProgressWhenTotalPhotosIsZero() {
        UploadSession session = UploadSession.start(UUID.randomUUID());

        assertThat(session.getProgressPercentage()).isEqualTo(0);
    }

    @Test
    void shouldCompleteSession() {
        UploadSession session = UploadSession.start(UUID.randomUUID());

        session.complete();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.isActive()).isFalse();
    }

    @Test
    void shouldRejectCompletingNonInProgressSession() {
        UploadSession session = UploadSession.start(UUID.randomUUID());
        session.complete();

        assertThatThrownBy(() -> session.complete())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Can only complete session from IN_PROGRESS state");
    }

    @Test
    void shouldFailSession() {
        UploadSession session = UploadSession.start(UUID.randomUUID());

        session.fail();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.FAILED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.isActive()).isFalse();
    }

    @Test
    void shouldCancelSession() {
        UploadSession session = UploadSession.start(UUID.randomUUID());

        session.cancel();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.isActive()).isFalse();
    }

    @Test
    void shouldEnforceStateTransitions() {
        UploadSession session = UploadSession.start(UUID.randomUUID());

        // Valid transition: IN_PROGRESS -> COMPLETED
        session.complete();

        // Invalid transitions from COMPLETED
        assertThatThrownBy(() -> session.complete())
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> session.fail())
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> session.cancel())
            .isInstanceOf(IllegalStateException.class);
    }
}
