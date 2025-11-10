package com.rapidphoto.domain.user;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class UserPreferencesTest {

    @Test
    void shouldCreateDefaultPreferences() {
        UUID userId = UUID.randomUUID();
        UserPreferences prefs = UserPreferences.createDefault(userId);

        assertThat(prefs.getUserId()).isEqualTo(userId);
        assertThat(prefs.isAnimationsEnabled()).isTrue();
        assertThat(prefs.isSoundEnabled()).isTrue();
        assertThat(prefs.getTheme()).isEqualTo(UserPreferences.Theme.SYSTEM);
        assertThat(prefs.getConcurrentUploads()).isEqualTo(3);
    }

    @Test
    void shouldRejectNullUserId() {
        assertThatThrownBy(() -> UserPreferences.createDefault(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User ID cannot be null");
    }

    @Test
    void shouldUpdateAnimationsEnabled() {
        UUID userId = UUID.randomUUID();
        UserPreferences prefs = UserPreferences.createDefault(userId);

        prefs.setAnimationsEnabled(false);

        assertThat(prefs.isAnimationsEnabled()).isFalse();
    }

    @Test
    void shouldUpdateSoundEnabled() {
        UUID userId = UUID.randomUUID();
        UserPreferences prefs = UserPreferences.createDefault(userId);

        prefs.setSoundEnabled(false);

        assertThat(prefs.isSoundEnabled()).isFalse();
    }

    @Test
    void shouldUpdateTheme() {
        UUID userId = UUID.randomUUID();
        UserPreferences prefs = UserPreferences.createDefault(userId);

        prefs.setTheme(UserPreferences.Theme.DARK);

        assertThat(prefs.getTheme()).isEqualTo(UserPreferences.Theme.DARK);
    }

    @Test
    void shouldRejectNullTheme() {
        UUID userId = UUID.randomUUID();
        UserPreferences prefs = UserPreferences.createDefault(userId);

        assertThatThrownBy(() -> prefs.setTheme(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Theme cannot be null");
    }

    @Test
    void shouldUpdateConcurrentUploads() {
        UUID userId = UUID.randomUUID();
        UserPreferences prefs = UserPreferences.createDefault(userId);

        prefs.setConcurrentUploads(5);

        assertThat(prefs.getConcurrentUploads()).isEqualTo(5);
    }

    @Test
    void shouldEnforceConcurrentUploadsBounds() {
        UUID userId = UUID.randomUUID();
        UserPreferences prefs = UserPreferences.createDefault(userId);

        // Test minimum bound
        assertThatThrownBy(() -> prefs.setConcurrentUploads(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be between 1 and 10");

        // Test maximum bound
        assertThatThrownBy(() -> prefs.setConcurrentUploads(11))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be between 1 and 10");

        // Boundary values should work
        prefs.setConcurrentUploads(1);
        assertThat(prefs.getConcurrentUploads()).isEqualTo(1);

        prefs.setConcurrentUploads(10);
        assertThat(prefs.getConcurrentUploads()).isEqualTo(10);
    }
}
