package com.rapidphoto.cqrs.queries;

import java.util.List;
import java.util.UUID;

/**
 * Query to get photos for a user with pagination and optional tag filtering.
 * Story 3.4: Tag Filter & Search - Added tagIds for filtering
 */
public record GetPhotosForUserQuery(
    UUID userId,
    int page,
    int size,
    List<UUID> tagIds  // Optional tag filter (AND logic - photos must have ALL tags)
) {
    /**
     * Constructor for backward compatibility (no tag filter).
     */
    public GetPhotosForUserQuery(UUID userId, int page, int size) {
        this(userId, page, size, null);
    }

    public GetPhotosForUserQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
