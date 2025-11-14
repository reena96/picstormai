package com.rapidphoto.util;

import java.security.SecureRandom;

/**
 * Color palette for tag visualization.
 * Provides 10 predefined colors with random selection.
 */
public class ColorPalette {

    private static final String[] COLORS = {
        "#3B82F6", // Blue
        "#EF4444", // Red
        "#10B981", // Green
        "#F59E0B", // Amber
        "#8B5CF6", // Purple
        "#EC4899", // Pink
        "#14B8A6", // Teal
        "#F97316", // Orange
        "#6366F1", // Indigo
        "#84CC16"  // Lime
    };

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Get a random color from the palette.
     * Uses SecureRandom for better distribution.
     *
     * @return Hex color code in format #RRGGBB
     */
    public static String getRandomColor() {
        return COLORS[RANDOM.nextInt(COLORS.length)];
    }

    /**
     * Get all available colors in the palette.
     *
     * @return Array of hex color codes
     */
    public static String[] getAllColors() {
        return COLORS.clone();
    }

    /**
     * Get number of colors in palette.
     *
     * @return Number of colors
     */
    public static int getColorCount() {
        return COLORS.length;
    }

    /**
     * Check if a color exists in the palette.
     *
     * @param color Hex color code to check
     * @return true if color exists in palette
     */
    public static boolean isValidPaletteColor(String color) {
        if (color == null) {
            return false;
        }
        for (String paletteColor : COLORS) {
            if (paletteColor.equalsIgnoreCase(color)) {
                return true;
            }
        }
        return false;
    }
}
