package com.rapidphoto.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for S3 operations including pre-signed URL generation.
 * Story 2.3: S3 Pre-Signed URL Generation
 */
@Service
public class S3Service {

    private static final long MULTIPART_THRESHOLD = 5 * 1024 * 1024; // 5MB
    private static final long PART_SIZE = 5 * 1024 * 1024; // 5MB per part
    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(15);

    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3Service(
        S3Presigner s3Presigner,
        @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    /**
     * Generate pre-signed URL for photo upload.
     * For files <5MB: returns single upload URL
     * For files >=5MB: returns multipart upload URLs
     */
    public Map<String, Object> generatePresignedUploadUrl(
        UUID userId,
        UUID sessionId,
        UUID photoId,
        String filename,
        long fileSizeBytes,
        String mimeType
    ) {
        String s3Key = String.format("uploads/%s/%s/%s-%s",
            userId, sessionId, photoId, filename);

        if (fileSizeBytes < MULTIPART_THRESHOLD) {
            // Single upload for small files
            String uploadUrl = generateSingleUploadUrl(s3Key, mimeType);
            return Map.of(
                "photoId", photoId.toString(),
                "uploadUrl", uploadUrl,
                "s3Key", s3Key
            );
        } else {
            // Multipart upload for large files
            return generateMultipartUploadUrls(s3Key, fileSizeBytes, mimeType, photoId);
        }
    }

    /**
     * Generate single pre-signed PUT URL.
     */
    private String generateSingleUploadUrl(String s3Key, String mimeType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(mimeType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(PRESIGNED_URL_DURATION)
            .putObjectRequest(objectRequest)
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Generate multipart upload URLs.
     */
    private Map<String, Object> generateMultipartUploadUrls(
        String s3Key,
        long fileSizeBytes,
        String mimeType,
        UUID photoId
    ) {
        // Calculate number of parts
        int numParts = (int) Math.ceil((double) fileSizeBytes / PART_SIZE);
        List<String> partUrls = new ArrayList<>();

        // Generate URL for each part
        // Note: In production, you'd use S3's CreateMultipartUpload API
        // For MVP, we'll generate individual part URLs
        for (int i = 1; i <= numParts; i++) {
            String partKey = s3Key + ".part" + i;
            String partUrl = generateSingleUploadUrl(partKey, mimeType);
            partUrls.add(partUrl);
        }

        // Generate a unique upload ID (in production, this comes from S3)
        String uploadId = UUID.randomUUID().toString();

        return Map.of(
            "photoId", photoId.toString(),
            "uploadId", uploadId,
            "partUrls", partUrls,
            "s3Key", s3Key,
            "numParts", numParts
        );
    }
}
