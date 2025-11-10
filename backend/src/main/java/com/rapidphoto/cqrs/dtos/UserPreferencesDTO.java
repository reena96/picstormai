package com.rapidphoto.cqrs.dtos;

import com.rapidphoto.domain.user.UserPreferences;

import java.util.UUID;

/**
 * DTO for UserPreferences data.
 * Never expose domain entities directly - always use DTOs.
 */
public record UserPreferencesDTO(
    UUID userId,
    boolean animationsEnabled,
    boolean soundEnabled,
    String theme,
    int concurrentUploads
) {
    /**
     * Create DTO from domain entity.
     */
    public static UserPreferencesDTO fromDomain(UserPreferences preferences) {
        return new UserPreferencesDTO(
            preferences.getUserId(),
            preferences.isAnimationsEnabled(),
            preferences.isSoundEnabled(),
            preferences.getTheme().name(),
            preferences.getConcurrentUploads()
        );
    }
}
