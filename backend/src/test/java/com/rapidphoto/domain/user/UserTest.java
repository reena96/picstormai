package com.rapidphoto.domain.user;

import com.rapidphoto.domain.shared.Email;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserWithValidCredentials() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getDisplayName()).isEqualTo("John Doe");
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    void shouldHashPasswordOnCreation() {
        Email email = Email.of("user@example.com");
        String plainPassword = "password123";
        User user = User.create(email, plainPassword, "John Doe");

        // Password should be hashed, not stored in plain text
        assertThat(user.checkPassword(plainPassword)).isTrue();
        assertThat(user.checkPassword("wrongpassword")).isFalse();
    }

    @Test
    void shouldRejectShortPassword() {
        Email email = Email.of("user@example.com");
        assertThatThrownBy(() -> User.create(email, "short", "John Doe"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password must be at least 8 characters");
    }

    @Test
    void shouldRejectNullPassword() {
        Email email = Email.of("user@example.com");
        assertThatThrownBy(() -> User.create(email, null, "John Doe"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password must be at least 8 characters");
    }

    @Test
    void shouldRejectEmptyDisplayName() {
        Email email = Email.of("user@example.com");
        assertThatThrownBy(() -> User.create(email, "password123", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Display name cannot be empty");
    }

    @Test
    void shouldTrimDisplayName() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "  John Doe  ");
        assertThat(user.getDisplayName()).isEqualTo("John Doe");
    }

    @Test
    void shouldVerifyEmail() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        assertThat(user.isEmailVerified()).isFalse();

        user.verifyEmail();

        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void shouldBeIdempotentWhenVerifyingEmail() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        user.verifyEmail();
        user.verifyEmail(); // Should not throw exception

        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void shouldRecordLoginTimestamp() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        assertThat(user.getLastLoginAt()).isNull();

        user.recordLogin();

        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isAfter(user.getCreatedAt());
    }

    @Test
    void shouldCheckPasswordCorrectly() {
        Email email = Email.of("user@example.com");
        String correctPassword = "password123";
        User user = User.create(email, correctPassword, "John Doe");

        assertThat(user.checkPassword(correctPassword)).isTrue();
        assertThat(user.checkPassword("wrongpassword")).isFalse();
        assertThat(user.checkPassword(null)).isFalse();
        assertThat(user.checkPassword("")).isFalse();
    }

    @Test
    void shouldUpdateDisplayName() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        user.updateDisplayName("Jane Smith");

        assertThat(user.getDisplayName()).isEqualTo("Jane Smith");
    }

    @Test
    void shouldRejectEmptyDisplayNameUpdate() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        assertThatThrownBy(() -> user.updateDisplayName(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Display name cannot be empty");
    }

    @Test
    void shouldTrimDisplayNameOnUpdate() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        user.updateDisplayName("  Jane Smith  ");

        assertThat(user.getDisplayName()).isEqualTo("Jane Smith");
    }

    @Test
    void shouldMarkOnboardingComplete() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        assertThat(user.isHasSeenOnboarding()).isFalse();

        user.markOnboardingComplete();

        assertThat(user.isHasSeenOnboarding()).isTrue();
    }

    @Test
    void shouldBeIdempotentWhenMarkingOnboardingComplete() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        user.markOnboardingComplete();
        user.markOnboardingComplete(); // Should not throw exception

        assertThat(user.isHasSeenOnboarding()).isTrue();
    }

    @Test
    void shouldHaveOnboardingFalseForNewUser() {
        Email email = Email.of("user@example.com");
        User user = User.create(email, "password123", "John Doe");

        assertThat(user.isHasSeenOnboarding()).isFalse();
    }
}
