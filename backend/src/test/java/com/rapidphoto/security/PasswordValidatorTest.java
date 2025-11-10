package com.rapidphoto.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordValidatorTest {

    @Test
    void shouldAcceptValidPassword() {
        // Given
        String validPassword = "Password123";

        // When
        List<String> errors = PasswordValidator.validate(validPassword);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAcceptPasswordWithMixedCase() {
        // Given
        String validPassword = "MySecurePass123";

        // When
        List<String> errors = PasswordValidator.validate(validPassword);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    void shouldRejectTooShortPassword() {
        // Given
        String shortPassword = "Pass1"; // Only 5 characters

        // When
        List<String> errors = PasswordValidator.validate(shortPassword);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("at least 8 characters");
    }

    @Test
    void shouldRejectPasswordWithoutUppercase() {
        // Given
        String noUppercase = "password123"; // No uppercase letters

        // When
        List<String> errors = PasswordValidator.validate(noUppercase);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("uppercase letter");
    }

    @Test
    void shouldRejectPasswordWithoutNumber() {
        // Given
        String noNumber = "PasswordOnly"; // No numbers

        // When
        List<String> errors = PasswordValidator.validate(noNumber);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("number");
    }

    @Test
    void shouldRejectPasswordWithMultipleViolations() {
        // Given
        String weakPassword = "weak"; // Too short, no uppercase, no number

        // When
        List<String> errors = PasswordValidator.validate(weakPassword);

        // Then
        assertThat(errors).hasSize(3);
        assertThat(errors).anyMatch(error -> error.contains("8 characters"));
        assertThat(errors).anyMatch(error -> error.contains("uppercase"));
        assertThat(errors).anyMatch(error -> error.contains("number"));
    }

    @Test
    void shouldThrowExceptionForInvalidPassword() {
        // Given
        String invalidPassword = "weak";

        // When / Then
        assertThatThrownBy(() -> PasswordValidator.validateOrThrow(invalidPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password");
    }

    @Test
    void shouldNotThrowExceptionForValidPassword() {
        // Given
        String validPassword = "ValidPassword123";

        // When / Then
        PasswordValidator.validateOrThrow(validPassword); // Should not throw
    }

    @Test
    void shouldRejectNullPassword() {
        // When
        List<String> errors = PasswordValidator.validate(null);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).isEqualTo("Password is required");
    }

    @Test
    void shouldRejectEmptyPassword() {
        // When
        List<String> errors = PasswordValidator.validate("");

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).isEqualTo("Password is required");
    }
}
