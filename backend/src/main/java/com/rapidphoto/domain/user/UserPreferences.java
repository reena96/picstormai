package com.rapidphoto.domain.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * User preferences entity.
 * Part of User aggregate - manages user UI/UX preferences.
 */
@Table("user_preferences")
public class UserPreferences {

    @Id
    private UUID userId;
    private boolean animationsEnabled;
    private boolean soundEnabled;
    private Theme theme;
    private int concurrentUploads;

    // Package-private constructor for persistence
    UserPreferences() {}

    private UserPreferences(UUID userId) {
        this.userId = userId;
        this.animationsEnabled = true; // Default values
        this.soundEnabled = true;
        this.theme = Theme.SYSTEM;
        this.concurrentUploads = 3; // Default 3 concurrent uploads
    }

    /**
     * Create default preferences for a user.
     */
    public static UserPreferences createDefault(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return new UserPreferences(userId);
    }

    /**
     * Update animation preference.
     */
    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
    }

    /**
     * Update sound preference.
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    /**
     * Update theme preference.
     */
    public void setTheme(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("Theme cannot be null");
        }
        this.theme = theme;
    }

    /**
     * Update concurrent uploads limit (1-10).
     */
    public void setConcurrentUploads(int limit) {
        if (limit < 1 || limit > 10) {
            throw new IllegalArgumentException("Concurrent uploads must be between 1 and 10");
        }
        this.concurrentUploads = limit;
    }

    // Getters

    public UUID getUserId() {
        return userId;
    }

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public Theme getTheme() {
        return theme;
    }

    public int getConcurrentUploads() {
        return concurrentUploads;
    }

    public enum Theme {
        LIGHT,
        DARK,
        SYSTEM
    }
}
