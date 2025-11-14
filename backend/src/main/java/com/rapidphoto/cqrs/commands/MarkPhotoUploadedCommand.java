package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to mark a photo as successfully uploaded to S3.
 * Called by frontend after S3 upload completes.
 */
public record MarkPhotoUploadedCommand(
    UUID photoId,
    String s3Key,
    UUID userId // For authorization
) {}
