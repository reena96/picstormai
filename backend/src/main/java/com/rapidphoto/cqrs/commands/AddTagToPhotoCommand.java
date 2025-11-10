package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to add a tag to a photo.
 */
public record AddTagToPhotoCommand(
    UUID photoId,
    UUID tagId
) {}
