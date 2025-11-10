package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get photos by tag with pagination.
 */
public record GetPhotosByTagQuery(
    UUID userId,
    UUID tagId,
    int page,
    int size
) {
    public GetPhotosByTagQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
