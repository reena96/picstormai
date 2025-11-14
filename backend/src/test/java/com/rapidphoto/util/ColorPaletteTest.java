package com.rapidphoto.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ColorPalette utility.
 * Story 3.3: Photo Tagging UI
 */
class ColorPaletteTest {

    @Test
    void testGetRandomColor_ReturnsValidHex() {
        // When
        String color = ColorPalette.getRandomColor();

        // Then
        assertThat(color).isNotNull();
        assertThat(color).matches("^#[0-9A-F]{6}$"); // Valid hex color format
        assertThat(ColorPalette.isValidPaletteColor(color)).isTrue();
    }

    @Test
    void testGetRandomColor_Returns10DifferentColors() {
        // Given
        Set<String> colors = new HashSet<>();
        int iterations = 1000;

        // When - Call getRandomColor many times to get different colors
        for (int i = 0; i < iterations; i++) {
            colors.add(ColorPalette.getRandomColor());
        }

        // Then - Should have collected at least 8 different colors (allowing for randomness)
        // With 1000 iterations and 10 colors, we should see at least 8 of them
        assertThat(colors.size()).isGreaterThanOrEqualTo(8);
        assertThat(colors.size()).isLessThanOrEqualTo(10); // Can't exceed palette size
    }

    @Test
    void testColorPalette_ContainsTenColors() {
        // When
        String[] allColors = ColorPalette.getAllColors();
        int colorCount = ColorPalette.getColorCount();

        // Then
        assertThat(allColors).hasSize(10);
        assertThat(colorCount).isEqualTo(10);

        // All colors should be valid hex format
        for (String color : allColors) {
            assertThat(color).matches("^#[0-9A-F]{6}$");
        }

        // All colors should be unique
        Set<String> uniqueColors = Set.of(allColors);
        assertThat(uniqueColors).hasSize(10);
    }

    @Test
    void testIsValidPaletteColor_ChecksCorrectly() {
        // Then - Valid palette colors
        assertThat(ColorPalette.isValidPaletteColor("#3B82F6")).isTrue(); // Blue
        assertThat(ColorPalette.isValidPaletteColor("#EF4444")).isTrue(); // Red

        // Then - Invalid colors (not in palette)
        assertThat(ColorPalette.isValidPaletteColor("#FFFFFF")).isFalse(); // White
        assertThat(ColorPalette.isValidPaletteColor("#000000")).isFalse(); // Black
        assertThat(ColorPalette.isValidPaletteColor(null)).isFalse();
        assertThat(ColorPalette.isValidPaletteColor("invalid")).isFalse();
    }
}
