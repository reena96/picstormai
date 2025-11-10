package com.rapidphoto.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration loaded from environment variables.
 * JWT_SECRET must be set as environment variable for production use.
 */
@Configuration
@Getter
public class JwtConfig {

    @Value("${jwt.secret:default-secret-change-me-in-production}")
    private String secret;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration-days:30}")
    private long refreshTokenExpirationDays;

    /**
     * Validates that JWT secret is configured properly.
     * @throws IllegalStateException if secret is default value in production
     */
    public void validateConfiguration() {
        if ("default-secret-change-me-in-production".equals(secret)) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable must be set. Do not use default secret in production!"
            );
        }
    }
}
