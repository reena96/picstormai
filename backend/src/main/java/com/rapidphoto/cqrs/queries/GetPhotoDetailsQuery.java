package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get detailed photo information.
 */
public record GetPhotoDetailsQuery(
    UUID photoId
) {}
