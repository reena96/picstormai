package com.rapidphoto.domain.upload;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for UploadSession aggregate.
 */
@Repository
public interface UploadSessionRepository extends ReactiveCrudRepository<UploadSession, UUID> {

    /**
     * Find all active sessions for a user.
     */
    @Query("SELECT * FROM upload_sessions WHERE user_id = :userId AND status = 'IN_PROGRESS' ORDER BY created_at DESC")
    Flux<UploadSession> findActiveSessionsByUserId(UUID userId);

    /**
     * Find all sessions for a user (any status).
     */
    @Query("SELECT * FROM upload_sessions WHERE user_id = :userId ORDER BY created_at DESC")
    Flux<UploadSession> findByUserId(UUID userId);

    /**
     * Count active sessions for a user.
     */
    @Query("SELECT COUNT(*) FROM upload_sessions WHERE user_id = :userId AND status = 'IN_PROGRESS'")
    Mono<Long> countActiveSessionsByUserId(UUID userId);
}
