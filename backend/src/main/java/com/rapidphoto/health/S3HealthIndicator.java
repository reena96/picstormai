package com.rapidphoto.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * Health indicator for S3 connectivity.
 * Checks if S3 bucket is accessible.
 */
@Component
public class S3HealthIndicator extends AbstractHealthIndicator {

    private final S3Client s3Client;
    private final String bucketName;

    public S3HealthIndicator(S3Client s3Client) {
        this.s3Client = s3Client;
        // Default bucket name - can be injected from configuration
        this.bucketName = "rapidphoto-uploads";
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Check if bucket exists and is accessible
            s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());

            builder.up()
                .withDetail("s3", "Connected")
                .withDetail("bucket", bucketName);
        } catch (Exception e) {
            builder.down()
                .withDetail("s3", "Connection failed")
                .withDetail("bucket", bucketName)
                .withDetail("error", e.getMessage());
        }
    }
}
