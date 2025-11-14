package com.rapidphoto.cqrs.dtos;

import java.time.Instant;

/**
 * DTO for photo download URL response.
 * Story 3.5: Individual Photo Download
 */
public record DownloadUrlDTO(
    String url,              // Presigned S3 URL
    String filename,         // Original filename
    Long fileSize,           // File size in bytes
    String expiresAt         // ISO-8601 timestamp (5 minutes from now)
) {
    /**
     * Create DTO with expiration timestamp.
     */
    public static DownloadUrlDTO create(String url, String filename, Long fileSize, Instant expiresAt) {
        return new DownloadUrlDTO(url, filename, fileSize, expiresAt.toString());
    }
}
