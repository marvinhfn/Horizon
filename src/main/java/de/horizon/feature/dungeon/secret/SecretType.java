package de.horizon.feature.dungeon.secret;

import java.util.Locale;

/** A category of dungeon secret with a display label and default waypoint colour. */
public enum SecretType {
    CHEST("Chest", 0xFFFFAA00),
    ITEM("Item", 0xFF55FFFF),
    ESSENCE("Wither Essence", 0xFFAA00AA),
    BAT("Bat", 0xFFFF5555),
    REDSTONE("Redstone Key", 0xFFAA0000),
    LEVER("Lever", 0xFFFFFF55);

    private final String label;
    private final int defaultColor;

    SecretType(String label, int defaultColor) {
        this.label = label;
        this.defaultColor = defaultColor;
    }

    public String label() { return label; }
    public int defaultColor() { return defaultColor; }

    /** Maps a data-file key ("chest", "essence", …) to a type, or null when unknown. */
    public static SecretType fromKey(String key) {
        if (key == null) return null;
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "chest" -> CHEST;
            case "item" -> ITEM;
            case "essence" -> ESSENCE;
            case "bat" -> BAT;
            case "redstone" -> REDSTONE;
            case "lever" -> LEVER;
            default -> null;
        };
    }
}
