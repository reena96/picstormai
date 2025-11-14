package com.rapidphoto.domain.photo;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Photo aggregate.
 */
@Repository
public interface PhotoRepository extends ReactiveCrudRepository<Photo, UUID> {

    /**
     * Find all photos for a user (excluding soft-deleted).
     */
    @Query("SELECT * FROM photos WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<Photo> findByUserId(UUID userId);

    /**
     * Find all photos in a session.
     */
    @Query("SELECT * FROM photos WHERE session_id = :sessionId ORDER BY created_at ASC")
    Flux<Photo> findBySessionId(UUID sessionId);

    /**
     * Find photos by status.
     */
    @Query("SELECT * FROM photos WHERE user_id = :userId AND upload_status = :status AND deleted_at IS NULL ORDER BY created_at DESC")
    Flux<Photo> findByUserIdAndStatus(UUID userId, String status);

    /**
     * Count photos in a session by status.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE session_id = :sessionId AND upload_status = :status")
    Mono<Long> countBySessionIdAndStatus(UUID sessionId, String status);

    /**
     * Find photos by tag (requires join with photo_tags table).
     */
    @Query("SELECT p.* FROM photos p " +
           "INNER JOIN photo_tags pt ON p.id = pt.photo_id " +
           "WHERE pt.tag_id = :tagId AND p.deleted_at IS NULL " +
           "ORDER BY p.created_at DESC")
    Flux<Photo> findByTagId(UUID tagId);

    /**
     * Find photos that have ALL specified tags (AND logic).
     * Story 3.4: Tag Filter & Search
     *
     * Uses INNER JOIN with photo_tags table and GROUP BY/HAVING to ensure
     * photos match ALL selected tags, not just ANY tag (OR logic).
     *
     * @param userId User ID to filter by
     * @param tagIds List of tag IDs that photos must have ALL of
     * @param tagCount Number of tags in tagIds (must match HAVING clause)
     * @return Flux of photos that have all specified tags
     */
    @Query("""
        SELECT DISTINCT p.* FROM photos p
        INNER JOIN photo_tags pt ON p.id = pt.photo_id
        WHERE p.user_id = :userId
          AND p.deleted_at IS NULL
          AND pt.tag_id IN (:tagIds)
        GROUP BY p.id, p.user_id, p.session_id, p.filename, p.file_size,
                 p.s3_key, p.storage_url, p.upload_status, p.progress,
                 p.metadata, p.created_at, p.updated_at, p.deleted_at
        HAVING COUNT(DISTINCT pt.tag_id) = :tagCount
        ORDER BY p.created_at DESC
    """)
    Flux<Photo> findByUserIdAndAllTags(UUID userId, List<UUID> tagIds, int tagCount);
}
