package com.rapidphoto.infrastructure;

import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.photo.S3Location;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test validating the entire infrastructure stack.
 * Tests complete upload flow: Database → Redis → S3 → Health Check.
 */
@AutoConfigureWebTestClient
class EndToEndIntegrationTest extends BaseIntegrationTest {

    private static final String BUCKET_NAME = "e2e-test-bucket";
    private static S3Client s3Client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setupS3() {
        s3Client = S3Client.builder()
            .endpointOverride(URI.create("http://localhost:4566"))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")
            ))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .build();

        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build());
        } catch (BucketAlreadyOwnedByYouException e) {
            // Bucket already exists
        }
    }

    @Test
    void shouldCompleteFullUploadFlowAcrossAllInfrastructure() {
        // 1. Create User in PostgreSQL
        Email email = Email.of("e2e@test.com");
        User user = User.create(email, "password123", "E2E Test User");

        StepVerifier.create(userRepository.save(user))
            .assertNext(savedUser -> {
                assertThat(savedUser.getId()).isNotNull();
                UUID userId = savedUser.getId();

                // 2. Create UploadSession in PostgreSQL
                UploadSession session = UploadSession.start(userId);
                session.setTotalPhotos(1);

                StepVerifier.create(uploadSessionRepository.save(session))
                    .assertNext(savedSession -> {
                        assertThat(savedSession.getId()).isNotNull();
                        UUID sessionId = savedSession.getId();

                        // 3. Store session ID in Redis
                        String redisKey = "session:" + sessionId;
                        String redisValue = "{\"userId\":\"" + userId + "\",\"status\":\"active\"}";

                        StepVerifier.create(
                            redisTemplate.opsForValue().set(redisKey, redisValue, Duration.ofMinutes(30))
                        )
                            .assertNext(success -> assertThat(success).isTrue())
                            .verifyComplete();

                        // 4. Create Photo in PostgreSQL
                        Photo photo = Photo.initiate(userId, sessionId, "test-photo.jpg", 1024000);
                        S3Location s3Location = S3Location.of(BUCKET_NAME, "photos/" + UUID.randomUUID() + ".jpg");
                        photo.startUpload(s3Location);

                        StepVerifier.create(photoRepository.save(photo))
                            .assertNext(savedPhoto -> {
                                assertThat(savedPhoto.getId()).isNotNull();

                                // 5. Upload file to S3 via LocalStack
                                String s3Key = s3Location.getKey();
                                String fileContent = "Test photo content";

                                PutObjectResponse s3Response = s3Client.putObject(
                                    PutObjectRequest.builder()
                                        .bucket(BUCKET_NAME)
                                        .key(s3Key)
                                        .metadata(java.util.Map.of(
                                            "photoId", savedPhoto.getId().toString(),
                                            "userId", userId.toString()
                                        ))
                                        .build(),
                                    RequestBody.fromString(fileContent)
                                );

                                assertThat(s3Response.eTag()).isNotNull();

                                // 6. Verify photo metadata in PostgreSQL includes S3 key
                                StepVerifier.create(photoRepository.findById(savedPhoto.getId()))
                                    .assertNext(retrievedPhoto -> {
                                        assertThat(retrievedPhoto.getS3Location()).isNotNull();
                                        assertThat(retrievedPhoto.getS3Location().getKey()).isEqualTo(s3Key);
                                    })
                                    .verifyComplete();

                                // 7. Retrieve session from Redis
                                StepVerifier.create(redisTemplate.opsForValue().get(redisKey))
                                    .assertNext(value -> {
                                        assertThat(value).isNotNull();
                                        assertThat(value).contains(userId.toString());
                                    })
                                    .verifyComplete();

                                // 8. Call /actuator/health to verify all components UP
                                webTestClient.get()
                                    .uri("/actuator/health")
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody()
                                    .jsonPath("$.status").isEqualTo("UP")
                                    .jsonPath("$.components.database.status").isEqualTo("UP")
                                    .jsonPath("$.components.redis.status").isEqualTo("UP")
                                    .jsonPath("$.components.s3.status").isEqualTo("UP");

                                // Cleanup: Delete S3 file
                                s3Client.deleteObject(DeleteObjectRequest.builder()
                                    .bucket(BUCKET_NAME)
                                    .key(s3Key)
                                    .build());

                                // Cleanup: Delete Redis session
                                StepVerifier.create(redisTemplate.delete(redisKey))
                                    .expectNextCount(1)
                                    .verifyComplete();

                                // Cleanup: Delete Photo
                                StepVerifier.create(photoRepository.deleteById(savedPhoto.getId()))
                                    .verifyComplete();
                            })
                            .verifyComplete();

                        // Cleanup: Delete UploadSession
                        StepVerifier.create(uploadSessionRepository.deleteById(sessionId))
                            .verifyComplete();
                    })
                    .verifyComplete();

                // Cleanup: Delete User
                StepVerifier.create(userRepository.deleteById(userId))
                    .verifyComplete();
            })
            .verifyComplete();
    }

    @Test
    void shouldVerifyAllInfrastructureComponentsIndependently() {
        // PostgreSQL check
        StepVerifier.create(
            userRepository.findAll()
                .collectList()
        )
            .assertNext(users -> assertThat(users).isNotNull())
            .verifyComplete();

        // Redis check
        String redisTestKey = "test:e2e:" + UUID.randomUUID();
        StepVerifier.create(
            redisTemplate.opsForValue().set(redisTestKey, "test")
                .then(redisTemplate.delete(redisTestKey))
        )
            .assertNext(deleteCount -> assertThat(deleteCount).isEqualTo(1L))
            .verifyComplete();

        // S3 check
        HeadBucketResponse s3Response = s3Client.headBucket(
            HeadBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build()
        );
        assertThat(s3Response).isNotNull();

        // Health check
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }
}
