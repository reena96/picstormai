package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to initiate a photo upload.
 */
public record InitiatePhotoUploadCommand(
    UUID userId,
    UUID sessionId,
    String filename,
    long fileSize
) {}
