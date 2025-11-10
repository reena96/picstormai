package com.rapidphoto.cqrs.commands;

import java.util.Map;
import java.util.UUID;

/**
 * Command to complete a photo upload.
 */
public record CompletePhotoUploadCommand(
    UUID photoId,
    String s3Key,
    String s3VersionId,
    Map<String, Object> exifMetadata
) {}
