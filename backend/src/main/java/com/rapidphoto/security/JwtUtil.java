package com.rapidphoto.security;

import com.rapidphoto.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Utility for generating and validating JWT tokens.
 * Access tokens: 15 minutes expiration
 * Refresh tokens: 30 days expiration
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtUtil(JwtConfig jwtConfig) {
        // Convert secret string to SecretKey for HMAC-SHA256
        this.secretKey = Keys.hmacShaKeyFor(
            jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.accessTokenExpirationMinutes = jwtConfig.getAccessTokenExpirationMinutes();
        this.refreshTokenExpirationDays = jwtConfig.getRefreshTokenExpirationDays();
    }

    /**
     * Generate short-lived access token (15 minutes).
     * Contains user ID and email as claims.
     * Includes jti (JWT ID) claim to ensure uniqueness.
     */
    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .id(UUID.randomUUID().toString())  // Unique token ID
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate long-lived refresh token (30 days).
     * Only contains user ID - minimal claims for security.
     * Includes jti (JWT ID) claim to ensure uniqueness.
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())  // Unique token ID for rotation
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate token and extract claims.
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract user ID from token.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract email from access token.
     * Note: Refresh tokens don't have email claim.
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // If validation fails, consider expired
        }
    }
}
