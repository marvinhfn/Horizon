package de.horizon.theme;

public final class Theme {
    public final int background, surface, surfaceAlt, surfaceHover;
    public final int accent, accentMuted, onAccent;
    public final int text, textMuted, textFaint;
    public final int border, borderSubtle;
    public final int toggleOn, toggleOff, toggleKnob;
    public final int warning, positive, danger;
    public final int hudBackdrop, hudSurface, hudSurfaceAlt, hudText, hudMuted;

    public Theme(int background, int surface, int surfaceAlt, int surfaceHover,
                 int accent, int accentMuted, int onAccent,
                 int text, int textMuted, int textFaint,
                 int border, int borderSubtle,
                 int toggleOn, int toggleOff, int toggleKnob,
                 int warning, int positive, int danger,
                 int hudBackdrop, int hudSurface, int hudSurfaceAlt, int hudText, int hudMuted) {
        this.background = background; this.surface = surface; this.surfaceAlt = surfaceAlt; this.surfaceHover = surfaceHover;
        this.accent = accent; this.accentMuted = accentMuted; this.onAccent = onAccent;
        this.text = text; this.textMuted = textMuted; this.textFaint = textFaint;
        this.border = border; this.borderSubtle = borderSubtle;
        this.toggleOn = toggleOn; this.toggleOff = toggleOff; this.toggleKnob = toggleKnob;
        this.warning = warning; this.positive = positive; this.danger = danger;
        this.hudBackdrop = hudBackdrop; this.hudSurface = hudSurface; this.hudSurfaceAlt = hudSurfaceAlt;
        this.hudText = hudText; this.hudMuted = hudMuted;
    }
}
