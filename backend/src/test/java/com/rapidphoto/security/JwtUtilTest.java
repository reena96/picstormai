package com.rapidphoto.security;

import com.rapidphoto.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtUtil.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "secret", "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtConfig, "accessTokenExpirationMinutes", 15L);
        ReflectionTestUtils.setField(jwtConfig, "refreshTokenExpirationDays", 30L);

        jwtUtil = new JwtUtil(jwtConfig);
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        // Act
        String token = jwtUtil.generateAccessToken(userId, email);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void generateRefreshToken_shouldCreateValidToken() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        String token = jwtUtil.generateRefreshToken(userId);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void validateToken_shouldExtractClaims() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = jwtUtil.generateAccessToken(userId, email);

        // Act
        Claims claims = jwtUtil.validateToken(token);

        // Assert
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void validateToken_shouldRejectInvalidToken() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(invalidToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void getUserIdFromToken_shouldExtractUserId() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, "test@example.com");

        // Act
        UUID extractedUserId = jwtUtil.getUserIdFromToken(token);

        // Assert
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void getEmailFromToken_shouldExtractEmail() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = jwtUtil.generateAccessToken(userId, email);

        // Act
        String extractedEmail = jwtUtil.getEmailFromToken(token);

        // Assert
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    void isTokenExpired_shouldReturnFalseForValidToken() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, "test@example.com");

        // Act
        boolean expired = jwtUtil.isTokenExpired(token);

        // Assert
        assertThat(expired).isFalse();
    }

    @Test
    void isTokenExpired_shouldReturnTrueForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean expired = jwtUtil.isTokenExpired(invalidToken);

        // Assert
        assertThat(expired).isTrue();
    }
}
