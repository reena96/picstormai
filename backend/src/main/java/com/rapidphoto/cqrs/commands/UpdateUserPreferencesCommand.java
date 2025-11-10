package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to update user preferences.
 */
public record UpdateUserPreferencesCommand(
    UUID userId,
    boolean animationsEnabled,
    boolean soundEnabled,
    String theme,
    int concurrentUploads,
    boolean uploadCompleteNotifications,
    boolean autoRetryFailed
) {}
