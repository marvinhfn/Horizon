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

    /** Filled rounded rect with a smooth 1px AA border (no pixel-steppy corners). */
    public static void card(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int fill, int border) {
        roundedRect(ctx, x, y, w, h, radius, border);
        roundedRect(ctx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
    }

    /** Filled pill with a smooth 1px AA border. */
    public static void pillBordered(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int fill, int border) {
        card(ctx, x, y, w, h, h / 2, fill, border);
    }

    /** A soft horizontal hairline that fades at both ends (modern divider). */
    public static void softDivider(GuiGraphicsExtractor ctx, int x, int y, int w, int color) {
        int mid = w / 2;
        for (int i = 0; i < w; i++) {
            float t = 1f - Math.abs(i - mid) / (float) Math.max(1, mid); // 0 at ends, 1 at middle
            int a = (int) (((color >>> 24) & 0xFF) * t);
            ctx.fill(x + i, y, x + i + 1, y + 1, (a << 24) | (color & 0xFFFFFF));
        }
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
        glassPanel(ctx, x, y, w, h, radius, withAlpha(theme.surface, 0x66), withAlpha(theme.accent, 0x99));
    }

    public static void glassPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius,
                                  int baseArgb, int accentArgb) {
        if (w <= 0 || h <= 0) return;
        // 1) translucent frosted base — low alpha so the backdrop reads through (real glass)
        roundedRect(ctx, x, y, w, h, radius, baseArgb);
        // 2) full-height frost sheen: bright at the top, fading to nothing — the Apple catch-light
        verticalGradient(ctx, x + radius, y + 1, w - radius * 2, Math.max(2, h - 2),
            withAlpha(0xFFFFFF, 0x30), withAlpha(0xFFFFFF, 0x06));
        // 3) a crisp specular highlight hugging the very top edge
        verticalGradient(ctx, x + radius, y + 1, w - radius * 2, Math.max(2, Math.min(h / 3, 14)),
            withAlpha(0xFFFFFF, 0x4D), withAlpha(0xFFFFFF, 0x00));
        // 4) subtle inner floor shadow for depth
        verticalGradient(ctx, x + radius, y + h - Math.max(2, Math.min(h / 4, 16)) - 1, w - radius * 2,
            Math.max(2, Math.min(h / 4, 16)), withAlpha(0x000000, 0x00), withAlpha(0x000000, 0x24));
        // 5) accent rim
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
