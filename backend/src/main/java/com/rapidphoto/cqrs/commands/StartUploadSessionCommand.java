package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to start a new upload session.
 */
public record StartUploadSessionCommand(
    UUID userId,
    int totalPhotos
) {}
