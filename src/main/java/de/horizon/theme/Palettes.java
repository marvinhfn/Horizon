package de.horizon.theme;

final class Palettes {
    private Palettes() {}
    private static int c(int rgb) { return 0xFF000000 | rgb; }

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

    static final Theme OCEAN_LIGHT    = rose(ROSE_LIGHT, c(0x2563EB), c(0xA9C5F7), 0x142563EB, 0x222563EB); // blue
    static final Theme OCEAN_DARK     = roseDark(ROSE_DARK,  c(0x60A5FA), c(0x2E4B7A), 0x2260A5FA, 0x3360A5FA);
    static final Theme MINT_LIGHT     = rose(ROSE_LIGHT, c(0x10B981), c(0x9BE3CE), 0x1410B981, 0x2210B981); // green
    static final Theme MINT_DARK      = roseDark(ROSE_DARK,  c(0x34D399), c(0x1F4E42), 0x2234D399, 0x3334D399);
    static final Theme LAVENDER_LIGHT = rose(ROSE_LIGHT, c(0x7C3AED), c(0xC3AEF3), 0x147C3AED, 0x227C3AED); // purple
    static final Theme LAVENDER_DARK  = roseDark(ROSE_DARK,  c(0xA78BFA), c(0x40337A), 0x22A78BFA, 0x33A78BFA);
    static final Theme AMBER_LIGHT    = rose(ROSE_LIGHT, c(0xF59E0B), c(0xF3D9A0), 0x14F59E0B, 0x22F59E0B); // amber
    static final Theme AMBER_DARK     = roseDark(ROSE_DARK,  c(0xFBBF24), c(0x6B5218), 0x22FBBF24, 0x33FBBF24);
    static final Theme SLATE_LIGHT    = rose(ROSE_LIGHT, c(0x475569), c(0xB4BECC), 0x14475569, 0x22475569); // neutral
    static final Theme SLATE_DARK     = roseDark(ROSE_DARK,  c(0x94A3B8), c(0x3A4452), 0x2294A3B8, 0x3394A3B8);

    static Theme light(ThemeFamily f) {
        return switch (f) {
            case ROSE -> ROSE_LIGHT; case OCEAN -> OCEAN_LIGHT; case MINT -> MINT_LIGHT;
            case LAVENDER -> LAVENDER_LIGHT; case AMBER -> AMBER_LIGHT; case SLATE -> SLATE_LIGHT;
        };
    }
    static Theme dark(ThemeFamily f) {
        return switch (f) {
            case ROSE -> ROSE_DARK; case OCEAN -> OCEAN_DARK; case MINT -> MINT_DARK;
            case LAVENDER -> LAVENDER_DARK; case AMBER -> AMBER_DARK; case SLATE -> SLATE_DARK;
        };
    }

    private static Theme rose(Theme base, int accent, int accentMuted, int surfaceHover, int border) {
        return new Theme(base.background, base.surface, base.surfaceAlt, surfaceHover,
            accent, accentMuted, base.onAccent, base.text, base.textMuted, base.textFaint,
            border, base.borderSubtle, accent, base.toggleOff, base.toggleKnob,
            base.warning, base.positive, base.danger,
            base.hudBackdrop, base.hudSurface, base.hudSurfaceAlt, base.hudText, base.hudMuted);
    }
    private static Theme roseDark(Theme base, int accent, int accentMuted, int surfaceHover, int border) {
        return rose(base, accent, accentMuted, surfaceHover, border);
    }
}
