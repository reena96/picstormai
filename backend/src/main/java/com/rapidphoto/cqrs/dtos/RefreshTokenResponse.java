package com.rapidphoto.cqrs.dtos;

/**
 * Response DTO for refresh token operation.
 * Contains new access token, new refresh token (rotation), and expiration info.
 */
public record RefreshTokenResponse(
    String accessToken,
    String refreshToken, // New refresh token (old one is invalidated)
    long expiresIn, // Seconds until access token expires
    String tokenType // Always "Bearer"
) {
    public RefreshTokenResponse {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }
        if (tokenType == null || tokenType.isEmpty()) {
            tokenType = "Bearer";
        }
    }

    /**
     * Factory method with default values.
     */
    public static RefreshTokenResponse of(String accessToken, String refreshToken, long expiresInMinutes) {
        return new RefreshTokenResponse(
            accessToken,
            refreshToken,
            expiresInMinutes * 60, // Convert minutes to seconds
            "Bearer"
        );
    }
}
