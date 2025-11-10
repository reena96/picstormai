package com.rapidphoto.domain.shared;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email value object - immutable and self-validating.
 * Follows RFC 5322 simplified validation pattern.
 */
public final class Email {

    // Simplified RFC 5322 email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    /**
     * Factory method to create Email value object with validation.
     *
     * @param email the email string to validate
     * @return Email value object
     * @throws IllegalArgumentException if email is invalid
     */
    public static Email of(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        String normalized = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        return new Email(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
