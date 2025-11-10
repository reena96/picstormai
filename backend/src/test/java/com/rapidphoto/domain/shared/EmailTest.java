package com.rapidphoto.domain.shared;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        Email email = Email.of("user@example.com");
        assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    void shouldNormalizeEmailToLowercase() {
        Email email = Email.of("User@Example.COM");
        assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    void shouldTrimWhitespace() {
        Email email = Email.of("  user@example.com  ");
        assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    void shouldRejectNullEmail() {
        assertThatThrownBy(() -> Email.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldRejectEmptyEmail() {
        assertThatThrownBy(() -> Email.of(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        assertThatThrownBy(() -> Email.of("invalid-email"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    void shouldRejectEmailWithoutDomain() {
        assertThatThrownBy(() -> Email.of("user@"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    void shouldRejectEmailWithoutAt() {
        assertThatThrownBy(() -> Email.of("userexample.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email format");
    }

    @Test
    void shouldSupportEquality() {
        Email email1 = Email.of("user@example.com");
        Email email2 = Email.of("user@example.com");
        Email email3 = Email.of("other@example.com");

        assertThat(email1).isEqualTo(email2);
        assertThat(email1).isNotEqualTo(email3);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @Test
    void shouldConvertToString() {
        Email email = Email.of("user@example.com");
        assertThat(email.toString()).isEqualTo("user@example.com");
    }
}
