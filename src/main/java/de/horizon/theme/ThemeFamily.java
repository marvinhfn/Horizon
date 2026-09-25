package de.horizon.theme;

public enum ThemeFamily {
    ROSE("rose", "Rose"), OCEAN("ocean", "Ocean"), MINT("mint", "Mint"),
    LAVENDER("lavender", "Lavender"), AMBER("amber", "Amber"), SLATE("slate", "Slate");

    private final String id;
    private final String label;
    ThemeFamily(String id, String label) { this.id = id; this.label = label; }
    public String id() { return id; }
    public String label() { return label; }

    public Theme resolve(ThemeMode mode) { return mode == ThemeMode.DARK ? dark() : light(); }
    public int chip() { return light().accent; }

    public Theme light() { return Palettes.light(this); }
    public Theme dark() { return Palettes.dark(this); }

    public static ThemeFamily fromId(String v) {
        if (v != null) for (ThemeFamily f : values()) if (f.id.equalsIgnoreCase(v.trim())) return f;
        return ROSE;
    }
}
