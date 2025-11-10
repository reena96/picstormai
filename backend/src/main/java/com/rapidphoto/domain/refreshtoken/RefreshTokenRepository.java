package com.rapidphoto.domain.refreshtoken;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for RefreshToken aggregate.
 * Provides reactive R2DBC access to refresh_tokens table.
 */
@Repository
public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, UUID> {

    /**
     * Find all refresh tokens for a specific user.
     */
    Flux<RefreshToken> findByUserId(UUID userId);

    /**
     * Delete all refresh tokens for a specific user.
     * Useful when user logs out from all devices.
     */
    @Query("DELETE FROM refresh_tokens WHERE user_id = :userId")
    Mono<Void> deleteByUserId(UUID userId);

    /**
     * Delete refresh token by ID.
     * Used for token rotation - invalidate old token after creating new one.
     */
    Mono<Void> deleteById(UUID tokenId);

    /**
     * Find expired tokens.
     * Used for cleanup job.
     */
    @Query("SELECT * FROM refresh_tokens WHERE expires_at < :now")
    Flux<RefreshToken> findExpiredTokens(Instant now);

    /**
     * Delete expired tokens.
     * Should be run periodically as a cleanup job.
     */
    @Query("DELETE FROM refresh_tokens WHERE expires_at < :now")
    Mono<Long> deleteExpiredTokens(Instant now);

    /**
     * Count tokens for a specific user.
     * Can be used to limit number of active sessions.
     */
    Mono<Long> countByUserId(UUID userId);
}
