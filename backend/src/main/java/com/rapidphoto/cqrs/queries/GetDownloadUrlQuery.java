package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get presigned download URL for a photo.
 * Story 3.5: Individual Photo Download
 */
public record GetDownloadUrlQuery(
    UUID photoId,
    UUID userId
) {}
