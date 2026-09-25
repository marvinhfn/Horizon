package de.horizon.theme;

final class Palettes {
    private Palettes() {}
    private static int c(int rgb) { return 0xFF000000 | rgb; }

    static Theme light(ThemeFamily f) { return ROSE_LIGHT; } // Task 2 replaces with switch
    static Theme dark(ThemeFamily f) { return ROSE_DARK; }   // Task 2 replaces with switch

    static final Theme ROSE_LIGHT = new Theme(
        c(0xF7F5F6), c(0xFFFFFF), c(0xFBEEF1), 0x14E11D64,   // background, surface, surfaceAlt, surfaceHover (accent-tinted, low alpha)
        c(0xE11D64), c(0xF7A8C4), c(0xFFFFFF),               // accent, accentMuted, onAccent
        c(0x1F2430), c(0x6B7280), c(0x9CA3AF),               // text, textMuted, textFaint
        0x22E11D64, 0x14000000,                              // border (accent, low alpha), borderSubtle
        c(0xE11D64), c(0xE5E7EB), c(0xFFFFFF),               // toggleOn, toggleOff, toggleKnob
        c(0xF59E0B), c(0x22C55E), c(0xEF4444),               // warning, positive, danger
        0xE01A1220, 0xE0241826, 0xE02E2030, c(0xFCE9F0), c(0xC9A9B8)); // hud* (dark by default)

    static final Theme ROSE_DARK = new Theme(
        c(0x141014), c(0x1E1820), c(0x271E28), 0x22E11D64,
        c(0xF472A6), c(0x7A3B57), c(0x241019),
        c(0xF3E9EE), c(0xB79AA6), c(0x8A727C),
        0x33F472A6, 0x1AFFFFFF,
        c(0xF472A6), c(0x3A2E36), c(0xF3E9EE),
        c(0xF59E0B), c(0x34D399), c(0xF87171),
        0xE0120E12, 0xE01E1820, 0xE0271E28, c(0xF3E9EE), c(0xB79AA6));
}
