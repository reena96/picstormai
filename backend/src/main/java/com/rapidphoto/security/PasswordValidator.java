package com.rapidphoto.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates password strength according to security requirements.
 * Requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter (A-Z)
 * - At least one number (0-9)
 */
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");

    /**
     * Validates password strength and returns list of violations.
     * @param password Password to validate
     * @return List of error messages (empty if valid)
     */
    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!NUMBER_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one number");
        }

        return errors;
    }

    /**
     * Checks if password is valid.
     * @param password Password to check
     * @return true if password meets all requirements
     */
    public static boolean isValid(String password) {
        return validate(password).isEmpty();
    }

    /**
     * Validates password and throws exception if invalid.
     * @param password Password to validate
     * @throws IllegalArgumentException if password is invalid
     */
    public static void validateOrThrow(String password) {
        List<String> errors = validate(password);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }
}
