package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to initiate a photo upload.
 * Story 2.3: S3 Pre-Signed URL Generation
 */
public record InitiatePhotoUploadCommand(
    UUID sessionId,
    UUID userId,
    String filename,
    long fileSizeBytes,
    String mimeType
) {}
