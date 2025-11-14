package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to delete a photo.
 * Soft deletes the photo by setting deletedAt timestamp.
 */
public record DeletePhotoCommand(
    UUID photoId,
    UUID userId // For authorization
) {}
