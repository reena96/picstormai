package com.rapidphoto.cqrs.commands;

/**
 * Command to verify user's email address using verification token.
 */
public record VerifyEmailCommand(
    String token
) {
    public VerifyEmailCommand {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Verification token cannot be null or empty");
        }
    }
}
