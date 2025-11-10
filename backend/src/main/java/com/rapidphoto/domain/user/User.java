package com.rapidphoto.domain.user;

import com.rapidphoto.domain.shared.Email;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

/**
 * User aggregate root.
 * Manages user identity, authentication, and preferences.
 * No setters - all modifications through business methods.
 */
@Table("users")
public class User {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Id
    private UUID id;
    private String email; // Stored as string in DB, wrapped in Email VO when needed
    private String passwordHash;
    private String displayName;
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;

    // Package-private constructor for persistence framework
    User() {}

    private User(UUID id, Email email, String passwordHash, String displayName) {
        this.id = id;
        this.email = email.getValue();
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.emailVerified = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Factory method to create new user with email and password.
     * Password is automatically hashed using BCrypt.
     */
    public static User create(Email email, String plainPassword, String displayName) {
        if (plainPassword == null || plainPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }

        String passwordHash = PASSWORD_ENCODER.encode(plainPassword);
        return new User(UUID.randomUUID(), email, passwordHash, displayName.trim());
    }

    /**
     * Verify user's email address.
     */
    public void verifyEmail() {
        if (this.emailVerified) {
            return; // Already verified, idempotent operation
        }
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Record user login timestamp.
     */
    public void recordLogin() {
        this.lastLoginAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Check if provided password matches the stored hash.
     */
    public boolean checkPassword(String plainPassword) {
        if (plainPassword == null) {
            return false;
        }
        return PASSWORD_ENCODER.matches(plainPassword, this.passwordHash);
    }

    /**
     * Update display name.
     */
    public void updateDisplayName(String newDisplayName) {
        if (newDisplayName == null || newDisplayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }
        this.displayName = newDisplayName.trim();
        this.updatedAt = Instant.now();
    }

    // Getters (no setters - immutability enforced)

    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return Email.of(email);
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
