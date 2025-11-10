package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to update user flags (e.g., hasSeenOnboarding).
 * Currently used for marking onboarding tutorial as complete.
 */
public record UpdateUserFlagCommand(
    UUID userId
) {
    public UpdateUserFlagCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }
}
