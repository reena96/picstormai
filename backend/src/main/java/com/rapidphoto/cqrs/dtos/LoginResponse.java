package com.rapidphoto.cqrs.dtos;

/**
 * Response DTO for login operation.
 * Contains access token, refresh token, and expiration info.
 */
public record LoginResponse(
    String accessToken,
    String refreshToken,
    long expiresIn, // Seconds until access token expires
    String tokenType // Always "Bearer"
) {
    public LoginResponse {
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
    public static LoginResponse of(String accessToken, String refreshToken, long expiresInMinutes) {
        return new LoginResponse(
            accessToken,
            refreshToken,
            expiresInMinutes * 60, // Convert minutes to seconds
            "Bearer"
        );
    }
}
