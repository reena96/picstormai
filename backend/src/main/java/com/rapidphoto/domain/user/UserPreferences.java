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
    private boolean uploadCompleteNotifications;
    private boolean autoRetryFailed;

    // Package-private constructor for persistence
    UserPreferences() {}

    private UserPreferences(UUID userId) {
        this.userId = userId;
        this.animationsEnabled = true; // Default values
        this.soundEnabled = true;
        this.theme = Theme.SYSTEM;
        this.concurrentUploads = 3; // Default 3 concurrent uploads
        this.uploadCompleteNotifications = true;
        this.autoRetryFailed = true;
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
     * Update concurrent uploads limit (1-20).
     */
    public void setConcurrentUploads(int limit) {
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("Concurrent uploads must be between 1 and 20");
        }
        this.concurrentUploads = limit;
    }

    /**
     * Update upload complete notifications preference.
     */
    public void setUploadCompleteNotifications(boolean enabled) {
        this.uploadCompleteNotifications = enabled;
    }

    /**
     * Update auto-retry failed uploads preference.
     */
    public void setAutoRetryFailed(boolean enabled) {
        this.autoRetryFailed = enabled;
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

    public boolean isUploadCompleteNotifications() {
        return uploadCompleteNotifications;
    }

    public boolean isAutoRetryFailed() {
        return autoRetryFailed;
    }

    public enum Theme {
        LIGHT,
        DARK,
        SYSTEM
    }
}
