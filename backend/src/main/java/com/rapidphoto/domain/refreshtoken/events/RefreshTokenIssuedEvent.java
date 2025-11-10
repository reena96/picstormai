package com.rapidphoto.domain.refreshtoken.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a refresh token is issued.
 * Can be used for security monitoring, analytics, or notifications.
 */
public record RefreshTokenIssuedEvent(
    UUID tokenId,
    UUID userId,
    Instant expiresAt,
    Instant issuedAt
) {
    public RefreshTokenIssuedEvent {
        if (tokenId == null || userId == null || expiresAt == null) {
            throw new IllegalArgumentException("Token ID, User ID, and expiration cannot be null");
        }
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }
}
