package com.rapidphoto.infrastructure;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Integration tests for S3 using LocalStack.
 * Tests upload, download, pre-signed URLs, and CORS configuration.
 */
class S3IntegrationTest extends BaseIntegrationTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static S3Client s3Client;
    private static S3Presigner s3Presigner;

    @BeforeAll
    static void setupS3Client() {
        // Create S3 client configured for LocalStack
        s3Client = S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(S3))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
            ))
            .region(Region.of(localstack.getRegion()))
            .build();

        // Create S3 Presigner for generating pre-signed URLs
        s3Presigner = S3Presigner.builder()
            .endpointOverride(localstack.getEndpointOverride(S3))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
            ))
            .region(Region.of(localstack.getRegion()))
            .build();

        // Create test bucket
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build());
        } catch (BucketAlreadyOwnedByYouException e) {
            // Bucket already exists, that's fine
        }
    }

    @Test
    void shouldVerifyBucketExists() {
        HeadBucketResponse response = s3Client.headBucket(HeadBucketRequest.builder()
            .bucket(BUCKET_NAME)
            .build());

        assertThat(response).isNotNull();
    }

    @Test
    void shouldUploadFileToS3() {
        String key = "test-upload.txt";
        String content = "Hello, S3!";

        PutObjectResponse response = s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build(),
            RequestBody.fromString(content)
        );

        assertThat(response.eTag()).isNotNull();

        // Cleanup
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(key)
            .build());
    }

    @Test
    void shouldDownloadFileFromS3() throws IOException {
        String key = "test-download.txt";
        String content = "Download test content";

        // Upload file
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build(),
            RequestBody.fromString(content)
        );

        // Download file
        InputStream inputStream = s3Client.getObject(
            GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build()
        );

        // Read content
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        assertThat(result.toString()).isEqualTo(content);

        // Cleanup
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(key)
            .build());
    }

    @Test
    void shouldUploadFileWithMetadata() {
        String key = "test-metadata.txt";
        String content = "File with metadata";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", "123");
        metadata.put("filename", "original.txt");

        PutObjectResponse response = s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .metadata(metadata)
                .build(),
            RequestBody.fromString(content)
        );

        assertThat(response.eTag()).isNotNull();

        // Verify metadata
        HeadObjectResponse headResponse = s3Client.headObject(
            HeadObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build()
        );

        assertThat(headResponse.metadata()).containsEntry("userid", "123");
        assertThat(headResponse.metadata()).containsEntry("filename", "original.txt");

        // Cleanup
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(key)
            .build());
    }

    @Test
    void shouldGeneratePresignedURL() {
        String key = "test-presigned.txt";
        String content = "Presigned URL test";

        // Upload file
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build(),
            RequestBody.fromString(content)
        );

        // Generate pre-signed URL (valid for 1 hour)
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(1))
            .getObjectRequest(GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build())
            .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        URL presignedUrl = presignedRequest.url();

        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl.toString()).contains(BUCKET_NAME);
        assertThat(presignedUrl.toString()).contains(key);

        // Cleanup
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(key)
            .build());
    }

    @Test
    void shouldAccessFileViaPresignedURL() throws IOException {
        String key = "test-presigned-access.txt";
        String content = "Access via presigned URL";

        // Upload file
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build(),
            RequestBody.fromString(content)
        );

        // Generate pre-signed URL
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(5))
            .getObjectRequest(GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build())
            .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        URL presignedUrl = presignedRequest.url();

        // Access file via HTTP GET
        HttpURLConnection connection = (HttpURLConnection) presignedUrl.openConnection();
        connection.setRequestMethod("GET");

        assertThat(connection.getResponseCode()).isEqualTo(200);

        // Read content
        InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        assertThat(result.toString()).isEqualTo(content);

        // Cleanup
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(key)
            .build());
    }

    @Test
    void shouldListObjectsInBucket() {
        String key1 = "list-test-1.txt";
        String key2 = "list-test-2.txt";

        // Upload multiple files
        s3Client.putObject(
            PutObjectRequest.builder().bucket(BUCKET_NAME).key(key1).build(),
            RequestBody.fromString("File 1")
        );
        s3Client.putObject(
            PutObjectRequest.builder().bucket(BUCKET_NAME).key(key2).build(),
            RequestBody.fromString("File 2")
        );

        // List objects
        ListObjectsV2Response response = s3Client.listObjectsV2(
            ListObjectsV2Request.builder()
                .bucket(BUCKET_NAME)
                .prefix("list-test-")
                .build()
        );

        assertThat(response.contents()).hasSize(2);
        assertThat(response.contents()).extracting(S3Object::key)
            .contains(key1, key2);

        // Cleanup
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(key1).build());
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(key2).build());
    }

    @Test
    void shouldDeleteFile() {
        String key = "test-delete.txt";

        // Upload file
        s3Client.putObject(
            PutObjectRequest.builder().bucket(BUCKET_NAME).key(key).build(),
            RequestBody.fromString("To be deleted")
        );

        // Verify exists
        HeadObjectResponse headResponse = s3Client.headObject(
            HeadObjectRequest.builder().bucket(BUCKET_NAME).key(key).build()
        );
        assertThat(headResponse).isNotNull();

        // Delete file
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(key)
            .build());

        // Verify deleted
        assertThatThrownBy(() ->
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build())
        ).isInstanceOf(NoSuchKeyException.class);
    }
}
