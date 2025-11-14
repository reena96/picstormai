package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to remove a tag from a photo.
 */
public record RemoveTagFromPhotoCommand(
    UUID photoId,
    UUID tagId,
    UUID userId
) {}
