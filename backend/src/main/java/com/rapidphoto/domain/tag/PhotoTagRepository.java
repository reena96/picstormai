package com.rapidphoto.domain.tag;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for PhotoTag junction entity.
 * Manages many-to-many relationship between photos and tags.
 *
 * Note: Uses UUID as ID type (placeholder) since PhotoTag doesn't have a single @Id field.
 * All actual operations use custom query methods with @Query annotations.
 */
@Repository
public interface PhotoTagRepository extends R2dbcRepository<PhotoTag, UUID> {

    /**
     * Find all photo-tag relationships for a photo.
     */
    Flux<PhotoTag> findByPhotoId(UUID photoId);

    /**
     * Find all photo-tag relationships for a tag.
     */
    Flux<PhotoTag> findByTagId(UUID tagId);

    /**
     * Count tags for a specific photo.
     */
    Mono<Long> countByPhotoId(UUID photoId);

    /**
     * Check if a photo has a specific tag.
     */
    Mono<Boolean> existsByPhotoIdAndTagId(UUID photoId, UUID tagId);

    /**
     * Delete specific photo-tag relationship.
     */
    @Modifying
    @Query("DELETE FROM photo_tags WHERE photo_id = :photoId AND tag_id = :tagId")
    Mono<Void> deleteByPhotoIdAndTagId(UUID photoId, UUID tagId);

    /**
     * Delete all tags from a photo.
     */
    @Modifying
    @Query("DELETE FROM photo_tags WHERE photo_id = :photoId")
    Mono<Void> deleteByPhotoId(UUID photoId);

    /**
     * Delete all photos using a tag.
     */
    @Modifying
    @Query("DELETE FROM photo_tags WHERE tag_id = :tagId")
    Mono<Void> deleteByTagId(UUID tagId);
}
