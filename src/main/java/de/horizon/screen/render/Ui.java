package de.horizon.screen.render;

import de.horizon.theme.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Ui {
    private Ui() {}

    public static void roundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        // center + vertical/horizontal cross
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);
        // four corner arcs (with one AA boundary pixel per row per corner)
        for (int dy = 0; dy < r; dy++) {
            double exact = Math.sqrt((double) r * r - (r - 1 - dy) * (r - 1 - dy));
            int dxFull = r - (int) Math.floor(exact);
            double frac = exact - Math.floor(exact);
            int edgeAlpha = (int) (((color >>> 24) & 0xFF) * frac);
            int edge = (edgeAlpha << 24) | (color & 0xFFFFFF);
            // full-coverage spans
            ctx.fill(x + dxFull,         y + dy,          x + r,         y + dy + 1,      color); // top-left
            ctx.fill(x + w - r,          y + dy,          x + w - dxFull, y + dy + 1,     color); // top-right
            ctx.fill(x + dxFull,         y + h - 1 - dy,  x + r,         y + h - dy,      color); // bottom-left
            ctx.fill(x + w - r,          y + h - 1 - dy,  x + w - dxFull, y + h - dy,     color); // bottom-right
            // AA boundary pixels (one pixel just outside the full span)
            if (dxFull - 1 >= 0)
                ctx.fill(x + dxFull - 1, y + dy,         x + dxFull,     y + dy + 1,      edge); // top-left AA
            if (x + w - dxFull + 1 <= x + w)
                ctx.fill(x + w - dxFull, y + dy,         x + w - dxFull + 1, y + dy + 1,  edge); // top-right AA
            if (dxFull - 1 >= 0)
                ctx.fill(x + dxFull - 1, y + h - 1 - dy, x + dxFull,    y + h - dy,       edge); // bottom-left AA
            if (x + w - dxFull + 1 <= x + w)
                ctx.fill(x + w - dxFull, y + h - 1 - dy, x + w - dxFull + 1, y + h - dy, edge); // bottom-right AA
        }
    }

    public static void outline(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int t, int color) {
        // top, bottom, left, right thin strips inset by radius on the rounded ends
        ctx.fill(x + radius, y, x + w - radius, y + t, color);
        ctx.fill(x + radius, y + h - t, x + w - radius, y + h, color);
        ctx.fill(x, y + radius, x + t, y + h - radius, color);
        ctx.fill(x + w - t, y + radius, x + w, y + h - radius, color);
    }

    public static void pill(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        roundedRect(ctx, x, y, w, h, h / 2, color);
    }

    public static void toggle(GuiGraphicsExtractor ctx, int x, int y, int w, int h, boolean on, Theme theme) {
        pill(ctx, x, y, w, h, on ? theme.toggleOn : theme.toggleOff);
        int knob = h - 4;
        int kx = on ? x + w - knob - 2 : x + 2;
        roundedRect(ctx, kx, y + 2, knob, knob, knob / 2, theme.toggleKnob);
    }

    public static void verticalGradient(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            ctx.fill(x, y + i, x + w, y + i + 1, lerp(top, bottom, t));
        }
    }

    /** Apple-style frosted panel: translucent base + top sheen + subtle accent rim. Theme-driven. */
    public static void glassPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, Theme theme) {
        glassPanel(ctx, x, y, w, h, radius, withAlpha(theme.surface, 0xD2), withAlpha(theme.accent, 0x66));
    }

    public static void glassPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius,
                                  int baseArgb, int accentArgb) {
        if (w <= 0 || h <= 0) return;
        // 1) translucent frosted base
        roundedRect(ctx, x, y, w, h, radius, baseArgb);
        // 2) top sheen — a soft highlight fading downward (the "glass" catch-light)
        int sheenH = Math.max(2, Math.min(h / 2, 18));
        int sheenTop = withAlpha(0xFFFFFF, 0x3A);
        int sheenBot = withAlpha(0xFFFFFF, 0x00);
        verticalGradient(ctx, x + radius, y + 1, w - radius * 2, sheenH, sheenTop, sheenBot);
        // 3) subtle accent rim
        outline(ctx, x, y, w, h, radius, 1, accentArgb);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static int lerp(int a, int b, float t) {
        int aa = (int) (((a >>> 24) & 0xFF) + (((b >>> 24) & 0xFF) - ((a >>> 24) & 0xFF)) * t);
        int ar = (int) (((a >>> 16) & 0xFF) + (((b >>> 16) & 0xFF) - ((a >>> 16) & 0xFF)) * t);
        int ag = (int) (((a >>> 8) & 0xFF) + (((b >>> 8) & 0xFF) - ((a >>> 8) & 0xFF)) * t);
        int ab = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }
}
