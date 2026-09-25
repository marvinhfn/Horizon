package de.horizon.theme;

import de.horizon.config.HorizonConfig;

public final class ThemeManager {
    private static ThemeFamily family = ThemeFamily.ROSE;
    private static ThemeMode mode = ThemeMode.LIGHT;
    private static HorizonConfig config;

    private ThemeManager() {}

    public static void init(HorizonConfig c) {
        config = c;
        if (c != null) {
            family = ThemeFamily.fromId(c.getThemeFamily());
            mode = ThemeMode.fromId(c.getThemeMode());
        }
    }
    public static Theme current() { return family.resolve(mode); }
    public static ThemeFamily family() { return family; }
    public static ThemeMode mode() { return mode; }
    public static void setFamily(ThemeFamily f) { family = f == null ? ThemeFamily.ROSE : f; if (config != null) config.setThemeFamily(family.id()); }
    public static void setMode(ThemeMode m) { mode = m == null ? ThemeMode.LIGHT : m; if (config != null) config.setThemeMode(mode.id()); }
}
