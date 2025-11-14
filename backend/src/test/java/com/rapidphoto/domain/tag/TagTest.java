package com.rapidphoto.domain.tag;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Tag domain model.
 * Story 3.3: Photo Tagging UI
 */
class TagTest {

    @Test
    void testCreate_ValidTag() {
        // Given
        UUID userId = UUID.randomUUID();
        String name = "vacation";
        String color = "#3B82F6";

        // When
        Tag tag = Tag.create(userId, name, color);

        // Then
        assertThat(tag).isNotNull();
        assertThat(tag.getId()).isNotNull();
        assertThat(tag.getUserId()).isEqualTo(userId);
        assertThat(tag.getName()).isEqualTo(name);
        assertThat(tag.getColor()).isEqualTo(color);
        assertThat(tag.getCreatedAt()).isNotNull();
        assertThat(tag.isNew()).isTrue();
    }

    @Test
    void testValidation_TagNameTooLong() {
        // Given
        UUID userId = UUID.randomUUID();
        String longName = "a".repeat(31); // 31 characters (max is 30)
        String color = "#3B82F6";

        // When/Then
        assertThatThrownBy(() -> Tag.create(userId, longName, color))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tag name exceeds 30 characters");
    }

    @Test
    void testValidation_InvalidColorFormat() {
        // Given
        UUID userId = UUID.randomUUID();
        String name = "vacation";

        // When/Then - Invalid color (lowercase)
        assertThatThrownBy(() -> Tag.create(userId, name, "#3b82f6"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid color format");

        // When/Then - Invalid color (missing #)
        assertThatThrownBy(() -> Tag.create(userId, name, "3B82F6"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid color format");

        // When/Then - Invalid color (wrong length)
        assertThatThrownBy(() -> Tag.create(userId, name, "#3B82"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid color format");
    }

    @Test
    void testTrim_TagNameWhitespace() {
        // Given
        UUID userId = UUID.randomUUID();
        String nameWithSpaces = "  vacation  ";
        String color = "#3B82F6";

        // When
        Tag tag = Tag.create(userId, nameWithSpaces, color);

        // Then
        assertThat(tag.getName()).isEqualTo("vacation"); // Trimmed
    }

    @Test
    void testColorFormat_Uppercase() {
        // Given
        UUID userId = UUID.randomUUID();
        String name = "vacation";
        String uppercaseColor = "#3B82F6";

        // When
        Tag tag = Tag.create(userId, name, uppercaseColor);

        // Then
        assertThat(tag.getColor()).isEqualTo(uppercaseColor);
    }

    @Test
    void testValidation_NullUserId() {
        // When/Then
        assertThatThrownBy(() -> Tag.create(null, "vacation", "#3B82F6"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User ID cannot be null");
    }

    @Test
    void testValidation_EmptyName() {
        // Given
        UUID userId = UUID.randomUUID();

        // When/Then
        assertThatThrownBy(() -> Tag.create(userId, "", "#3B82F6"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tag name cannot be empty");

        assertThatThrownBy(() -> Tag.create(userId, "   ", "#3B82F6"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tag name cannot be empty");
    }
}
