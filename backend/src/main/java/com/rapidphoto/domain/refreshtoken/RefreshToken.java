package com.rapidphoto.domain.refreshtoken;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * RefreshToken aggregate root.
 * Manages long-lived refresh tokens for JWT authentication.
 * Tokens are stored as BCrypt hash for security.
 */
@Table("refresh_tokens")
public class RefreshToken implements Persistable<UUID> {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final long DEFAULT_EXPIRATION_DAYS = 30;

    @Id
    private UUID id;
    private UUID userId;
    private String tokenHash; // BCrypt hash of the actual token
    private Instant expiresAt;
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    // Package-private constructor for persistence framework
    RefreshToken() {
        this.isNew = false;
    }

    private RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    /**
     * Factory method to create new refresh token.
     * @param userId User ID this token belongs to
     * @param plainToken The actual JWT token (not hashed yet)
     * @param expirationDays Token validity in days
     */
    public static RefreshToken create(UUID userId, String plainToken, long expirationDays) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (plainToken == null || plainToken.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        if (expirationDays <= 0) {
            throw new IllegalArgumentException("Expiration days must be positive");
        }

        String tokenHash = PASSWORD_ENCODER.encode(plainToken);
        Instant expiresAt = Instant.now().plus(expirationDays, ChronoUnit.DAYS);

        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, expiresAt);
    }

    /**
     * Factory method with default expiration (30 days).
     */
    public static RefreshToken create(UUID userId, String plainToken) {
        return create(userId, plainToken, DEFAULT_EXPIRATION_DAYS);
    }

    /**
     * Verify if provided token matches the stored hash.
     */
    public boolean verifyToken(String plainToken) {
        if (plainToken == null) {
            return false;
        }
        return PASSWORD_ENCODER.matches(plainToken, this.tokenHash);
    }

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * Check if token is valid (not expired and verified).
     */
    public boolean isValid(String plainToken) {
        return !isExpired() && verifyToken(plainToken);
    }

    // Getters
    @Override
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }

    /**
     * Mark entity as persisted (called by Spring Data after insert).
     */
    public void markNotNew() {
        this.isNew = false;
    }
}
