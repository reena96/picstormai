package com.rapidphoto.domain.integration;

import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import com.rapidphoto.domain.user.UserPreferences;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import com.rapidphoto.domain.upload.SessionStatus;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.photo.PhotoStatus;
import com.rapidphoto.domain.photo.S3Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for domain aggregates with R2DBC repositories.
 * Uses Testcontainers to spin up PostgreSQL database.
 */
@DataR2dbcTest
@Testcontainers
class DomainIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Test
    void shouldSaveAndRetrieveUser() {
        // Create user
        Email email = Email.of("test@example.com");
        User user = User.create(email, "password123", "Test User");

        // Save and verify
        StepVerifier.create(userRepository.save(user))
            .assertNext(savedUser -> {
                assertThat(savedUser.getId()).isNotNull();
                assertThat(savedUser.getEmail()).isEqualTo(email);
                assertThat(savedUser.getDisplayName()).isEqualTo("Test User");
                assertThat(savedUser.isEmailVerified()).isFalse();
            })
            .verifyComplete();

        // Retrieve and verify
        StepVerifier.create(userRepository.findByEmail(email.getValue()))
            .assertNext(retrievedUser -> {
                assertThat(retrievedUser.getId()).isEqualTo(user.getId());
                assertThat(retrievedUser.getEmail()).isEqualTo(email);
            })
            .verifyComplete();
    }

    @Test
    void shouldVerifyUserEmailAndPersist() {
        // Create and save user
        Email email = Email.of("verify@example.com");
        User user = User.create(email, "password123", "Verify Test");

        StepVerifier.create(userRepository.save(user))
            .assertNext(savedUser -> {
                assertThat(savedUser.isEmailVerified()).isFalse();

                // Verify email
                savedUser.verifyEmail();
                assertThat(savedUser.isEmailVerified()).isTrue();
            })
            .verifyComplete();

        // Save updated user and verify persistence
        StepVerifier.create(userRepository.save(user).then(userRepository.findById(user.getId())))
            .assertNext(retrievedUser -> {
                assertThat(retrievedUser.isEmailVerified()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void shouldSaveUserPreferences() {
        // Create user first
        Email email = Email.of("prefs@example.com");
        User user = User.create(email, "password123", "Prefs User");

        StepVerifier.create(userRepository.save(user))
            .assertNext(savedUser -> {
                // Create preferences for user
                UserPreferences prefs = UserPreferences.createDefault(savedUser.getId());
                prefs.setConcurrentUploads(5);
                prefs.setTheme(UserPreferences.Theme.DARK);

                // Save preferences
                StepVerifier.create(userPreferencesRepository.save(prefs))
                    .assertNext(savedPrefs -> {
                        assertThat(savedPrefs.getUserId()).isEqualTo(savedUser.getId());
                        assertThat(savedPrefs.getConcurrentUploads()).isEqualTo(5);
                        assertThat(savedPrefs.getTheme()).isEqualTo(UserPreferences.Theme.DARK);
                    })
                    .verifyComplete();
            })
            .verifyComplete();
    }

    @Test
    void shouldSaveAndRetrieveUploadSession() {
        // Create user
        UUID userId = UUID.randomUUID();

        // Create upload session
        UploadSession session = UploadSession.start(userId);
        session.setTotalPhotos(10);
        session.recordPhotoUploaded();
        session.recordPhotoUploaded();

        // Save and verify
        StepVerifier.create(uploadSessionRepository.save(session))
            .assertNext(savedSession -> {
                assertThat(savedSession.getId()).isNotNull();
                assertThat(savedSession.getUserId()).isEqualTo(userId);
                assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
                assertThat(savedSession.getTotalPhotos()).isEqualTo(10);
                assertThat(savedSession.getCompletedPhotos()).isEqualTo(2);
                assertThat(savedSession.getProgressPercentage()).isEqualTo(20);
            })
            .verifyComplete();

        // Find active sessions
        StepVerifier.create(uploadSessionRepository.findActiveSessionsByUserId(userId))
            .assertNext(activeSession -> {
                assertThat(activeSession.getId()).isEqualTo(session.getId());
                assertThat(activeSession.isActive()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void shouldCompleteUploadSessionAndPersist() {
        UUID userId = UUID.randomUUID();
        UploadSession session = UploadSession.start(userId);

        StepVerifier.create(uploadSessionRepository.save(session))
            .assertNext(savedSession -> {
                // Complete session
                savedSession.complete();

                // Save completed session
                StepVerifier.create(uploadSessionRepository.save(savedSession)
                    .then(uploadSessionRepository.findById(savedSession.getId())))
                    .assertNext(retrievedSession -> {
                        assertThat(retrievedSession.getStatus()).isEqualTo(SessionStatus.COMPLETED);
                        assertThat(retrievedSession.getCompletedAt()).isNotNull();
                        assertThat(retrievedSession.isActive()).isFalse();
                    })
                    .verifyComplete();
            })
            .verifyComplete();
    }

    @Test
    void shouldSaveAndRetrievePhoto() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        // Create photo
        Photo photo = Photo.initiate(userId, sessionId, "test-photo.jpg", 1024000);
        S3Location location = S3Location.of("test-bucket", "photos/test.jpg");
        photo.startUpload(location);
        photo.updateProgress(50);

        // Save and verify
        StepVerifier.create(photoRepository.save(photo))
            .assertNext(savedPhoto -> {
                assertThat(savedPhoto.getId()).isNotNull();
                assertThat(savedPhoto.getUserId()).isEqualTo(userId);
                assertThat(savedPhoto.getSessionId()).isEqualTo(sessionId);
                assertThat(savedPhoto.getFilename()).isEqualTo("test-photo.jpg");
                assertThat(savedPhoto.getUploadStatus()).isEqualTo(PhotoStatus.UPLOADING);
                assertThat(savedPhoto.getProgress()).isEqualTo(50);
            })
            .verifyComplete();

        // Find by session
        StepVerifier.create(photoRepository.findBySessionId(sessionId))
            .assertNext(foundPhoto -> {
                assertThat(foundPhoto.getId()).isEqualTo(photo.getId());
            })
            .verifyComplete();
    }

    @Test
    void shouldCompletePhotoUploadAndPersist() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Photo photo = Photo.initiate(userId, sessionId, "complete-test.jpg", 2048000);
        S3Location location = S3Location.of("test-bucket", "photos/complete.jpg");
        photo.startUpload(location);

        StepVerifier.create(photoRepository.save(photo))
            .assertNext(savedPhoto -> {
                // Complete upload
                savedPhoto.completeUpload(null);

                // Save completed photo
                StepVerifier.create(photoRepository.save(savedPhoto)
                    .then(photoRepository.findById(savedPhoto.getId())))
                    .assertNext(retrievedPhoto -> {
                        assertThat(retrievedPhoto.getUploadStatus()).isEqualTo(PhotoStatus.COMPLETED);
                        assertThat(retrievedPhoto.getProgress()).isEqualTo(100);
                    })
                    .verifyComplete();
            })
            .verifyComplete();
    }

    @Test
    void shouldFindPhotosByUserId() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        // Create multiple photos
        Photo photo1 = Photo.initiate(userId, sessionId, "photo1.jpg", 1000);
        Photo photo2 = Photo.initiate(userId, sessionId, "photo2.jpg", 2000);

        StepVerifier.create(
            photoRepository.save(photo1)
                .then(photoRepository.save(photo2))
                .thenMany(photoRepository.findByUserId(userId))
        )
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void shouldNotFindSoftDeletedPhotos() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Photo photo = Photo.initiate(userId, sessionId, "delete-test.jpg", 1000);

        StepVerifier.create(photoRepository.save(photo))
            .assertNext(savedPhoto -> {
                // Soft delete photo
                savedPhoto.softDelete();

                // Save deleted photo
                StepVerifier.create(photoRepository.save(savedPhoto)
                    .thenMany(photoRepository.findByUserId(userId)))
                    .expectNextCount(0) // Should not find soft-deleted photos
                    .verifyComplete();
            })
            .verifyComplete();
    }
}
