package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get photos for a user with pagination.
 */
public record GetPhotosForUserQuery(
    UUID userId,
    int page,
    int size
) {
    public GetPhotosForUserQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
