package com.rapidphoto.domain.photo;

import java.util.Objects;
import java.util.UUID;

/**
 * PhotoId value object - wraps UUID with type safety.
 * Immutable identifier for Photo aggregate.
 */
public final class PhotoId {

    private final UUID value;

    private PhotoId(UUID value) {
        this.value = value;
    }

    /**
     * Create PhotoId from existing UUID.
     */
    public static PhotoId of(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("PhotoId UUID cannot be null");
        }
        return new PhotoId(uuid);
    }

    /**
     * Generate new PhotoId with random UUID.
     */
    public static PhotoId generate() {
        return new PhotoId(UUID.randomUUID());
    }

    /**
     * Create PhotoId from string representation.
     */
    public static PhotoId fromString(String uuidString) {
        try {
            return new PhotoId(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PhotoId format: " + uuidString, e);
        }
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoId photoId = (PhotoId) o;
        return Objects.equals(value, photoId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
