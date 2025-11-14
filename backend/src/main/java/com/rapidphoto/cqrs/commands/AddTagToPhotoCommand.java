package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to add a tag to a photo.
 * Supports find-or-create tag behavior by accepting tag name.
 */
public record AddTagToPhotoCommand(
    UUID photoId,
    UUID userId,
    String tagName
) {}
