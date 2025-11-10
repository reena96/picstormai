package com.rapidphoto.domain.verification;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * EmailVerificationToken aggregate root.
 * Manages email verification tokens with expiration.
 * Tokens are 32-character hex strings that expire in 24 hours.
 */
@Table("email_verification_tokens")
public class EmailVerificationToken implements Persistable<UUID> {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;
    private static final long DEFAULT_EXPIRATION_HOURS = 24;

    @Id
    private UUID id;
    private UUID userId;
    private String token; // 32-character hex string
    private Instant expiresAt;
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    // Package-private constructor for persistence framework
    EmailVerificationToken() {
        this.isNew = false;
    }

    private EmailVerificationToken(UUID id, UUID userId, String token, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    /**
     * Factory method to create new email verification token.
     * @param userId User ID this token belongs to
     * @return EmailVerificationToken with secure random token
     */
    public static EmailVerificationToken create(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        String token = generateSecureToken();
        Instant expiresAt = Instant.now().plus(DEFAULT_EXPIRATION_HOURS, ChronoUnit.HOURS);

        return new EmailVerificationToken(UUID.randomUUID(), userId, token, expiresAt);
    }

    /**
     * Generates cryptographically secure random token.
     * @return 32-character hex string
     */
    private static String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH / 2]; // 16 bytes = 32 hex chars
        SECURE_RANDOM.nextBytes(randomBytes);
        return bytesToHex(randomBytes);
    }

    /**
     * Converts byte array to hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * Check if token is valid (not expired).
     */
    public boolean isValid() {
        return !isExpired();
    }

    // Getters
    @Override
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
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
