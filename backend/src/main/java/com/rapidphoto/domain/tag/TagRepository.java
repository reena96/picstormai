package com.rapidphoto.domain.tag;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for Tag aggregate.
 * Provides reactive access to tag data.
 */
@Repository
public interface TagRepository extends R2dbcRepository<Tag, UUID> {

    /**
     * Find all tags for a user.
     */
    Flux<Tag> findByUserId(UUID userId);

    /**
     * Find tag by user ID and name (case-sensitive).
     */
    Mono<Tag> findByUserIdAndName(UUID userId, String name);

    /**
     * Check if tag exists for user with given name.
     */
    Mono<Boolean> existsByUserIdAndName(UUID userId, String name);

    /**
     * Find tags for a specific photo.
     */
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN photo_tags pt ON t.id = pt.tag_id
        WHERE pt.photo_id = :photoId
        ORDER BY t.name
        """)
    Flux<Tag> findByPhotoId(UUID photoId);

    /**
     * Find tags for multiple photos (batch query).
     */
    @Query("""
        SELECT t.*, pt.photo_id FROM tags t
        INNER JOIN photo_tags pt ON t.id = pt.tag_id
        WHERE pt.photo_id IN (:photoIds)
        ORDER BY t.name
        """)
    Flux<TagWithPhotoId> findByPhotoIds(Iterable<UUID> photoIds);

    /**
     * DTO for batch tag queries with photo association.
     */
    record TagWithPhotoId(
        UUID id,
        UUID userId,
        String name,
        String color,
        java.time.Instant createdAt,
        UUID photoId
    ) {}
}
