package com.rapidphoto.domain.tag;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tag aggregate root.
 * User-defined tags for organizing photos.
 * Tags are shared across all user's photos.
 */
@Table("tags")
public class Tag implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("name")
    private String name;

    @Column("color")
    private String color;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    // Package-private constructor for persistence
    Tag() {
        this.isNew = false;
    }

    private Tag(UUID id, UUID userId, String name, String color) {
        this.id = id;
        this.userId = userId;
        this.name = name.trim();
        this.color = color;
        this.createdAt = Instant.now();
        this.isNew = true;
    }

    /**
     * Factory method to create new tag.
     */
    public static Tag create(UUID userId, String name, String color) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }
        if (name.trim().length() > 30) {
            throw new IllegalArgumentException("Tag name exceeds 30 characters");
        }
        if (!isValidHexColor(color)) {
            throw new IllegalArgumentException("Invalid color format. Expected #RRGGBB");
        }

        return new Tag(UUID.randomUUID(), userId, name, color);
    }

    /**
     * Validate hex color format (#RRGGBB).
     */
    private static boolean isValidHexColor(String color) {
        if (color == null) {
            return false;
        }
        return color.matches("^#[0-9A-F]{6}$");
    }

    /**
     * Validate tag constraints.
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }
        if (name.length() > 30) {
            throw new IllegalArgumentException("Tag name exceeds 30 characters");
        }
        if (!isValidHexColor(color)) {
            throw new IllegalArgumentException("Invalid color format. Expected #RRGGBB");
        }
    }

    // Getters (no setters - immutability enforced)

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Persistable interface methods
    @Override
    public boolean isNew() {
        return isNew;
    }
}
