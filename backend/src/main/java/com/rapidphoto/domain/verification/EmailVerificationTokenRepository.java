package com.rapidphoto.domain.verification;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for EmailVerificationToken aggregate.
 * Provides reactive R2DBC access to email_verification_tokens table.
 */
@Repository
public interface EmailVerificationTokenRepository extends ReactiveCrudRepository<EmailVerificationToken, UUID> {

    /**
     * Find token by token string.
     */
    Mono<EmailVerificationToken> findByToken(String token);

    /**
     * Find token by user ID.
     */
    Mono<EmailVerificationToken> findByUserId(UUID userId);

    /**
     * Delete token by token string.
     */
    Mono<Void> deleteByToken(String token);

    /**
     * Delete all tokens for a specific user.
     */
    Mono<Void> deleteByUserId(UUID userId);

    /**
     * Delete expired tokens.
     * Should be run periodically as a cleanup job.
     */
    @Query("DELETE FROM email_verification_tokens WHERE expires_at < :now")
    Mono<Long> deleteExpiredTokens(Instant now);
}
