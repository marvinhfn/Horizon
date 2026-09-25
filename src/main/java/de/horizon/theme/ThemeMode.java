package de.horizon.theme;

public enum ThemeMode {
    LIGHT("light"), DARK("dark");
    private final String id;
    ThemeMode(String id) { this.id = id; }
    public String id() { return id; }
    public static ThemeMode fromId(String v) {
        if (v != null) for (ThemeMode m : values()) if (m.id.equalsIgnoreCase(v.trim())) return m;
        return LIGHT;
    }
}
