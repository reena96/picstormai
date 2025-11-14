package com.rapidphoto.cqrs.queries;

import java.util.List;
import java.util.UUID;

/**
 * Query to generate batch download ZIP for multiple photos.
 * Story 3.6: Batch Photo Download (ZIP)
 *
 * @param photoIds List of photo IDs to download (max 50)
 * @param userId User ID requesting the download
 */
public record GenerateBatchDownloadQuery(
    List<UUID> photoIds,
    UUID userId
) {
    private static final int MAX_PHOTOS = 50;

    public GenerateBatchDownloadQuery {
        // Validation
        if (photoIds == null || photoIds.isEmpty()) {
            throw new IllegalArgumentException("Photo IDs list cannot be empty");
        }
        if (photoIds.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Maximum " + MAX_PHOTOS + " photos per batch download");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }
}
