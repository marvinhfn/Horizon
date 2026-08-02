package de.horizon.feature.misc;

/**
 * Shared state for the scrollable/resizable tooltip feature. The scroll offset and scale are applied
 * as a pose transform in {@code GuiGraphicsExtractorTooltipMixin}; the scroll input is captured in
 * {@code HorizonClient}'s screen-scroll hook.
 */
public final class TooltipState {
    private TooltipState() {}

    /** Vertical scroll offset in pixels (positive = tooltip shifted up so lower lines show). */
    public static int scrollOffset = 0;
    /** Live tooltip scale (initialised from config, adjusted with Ctrl+scroll). */
    public static float scale = 1.0f;
    /** Millis of the last tooltip render (used to tell whether a tooltip is currently showing). */
    public static long lastShownMs = 0;

    /** Called by the tooltip mixin each time a tooltip renders. Resets scroll when a new one starts. */
    public static void markShown() {
        long now = System.currentTimeMillis();
        if (now - lastShownMs > 200) scrollOffset = 0; // gap → a different tooltip → reset scroll
        lastShownMs = now;
    }

    /** True if a tooltip rendered within the last ~100 ms (so scroll input should pan it). */
    public static boolean isShowing() {
        return System.currentTimeMillis() - lastShownMs < 100;
    }

    public static void resetScroll() {
        scrollOffset = 0;
    }
}
