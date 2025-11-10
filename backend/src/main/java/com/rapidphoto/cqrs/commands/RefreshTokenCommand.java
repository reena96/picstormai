package com.rapidphoto.cqrs.commands;

/**
 * Command to refresh access token using refresh token.
 * Implements token rotation - old refresh token is invalidated,
 * new refresh token is issued.
 */
public record RefreshTokenCommand(
    String refreshToken
) {
    public RefreshTokenCommand {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }
    }
}
