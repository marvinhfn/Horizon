package de.horizon.screen.render;

import com.mojang.blaze3d.platform.Window;
import de.horizon.mixin.WindowAccessor;
import net.minecraft.client.Minecraft;

/**
 * Tick-driven Hi-DPI GUI scale manager.
 *
 * Static state ({@code applied}, {@code originalScale}) is authoritative and driven exclusively by
 * {@link #sync(Minecraft)}, which is called from the client tick and from the {@code init()} of
 * every fine-scale screen. The old enter/exit pair has been replaced by this single method so that
 * screen-to-screen transitions (which never call {@code onClose()}) are handled correctly.
 *
 * Screens that want the fine scale must implement {@link HiDpiScreen}; screens that must run at the
 * user's real scale (e.g. HudLayoutScreen) must NOT implement the interface — calling sync() from
 * their init() will immediately restore the user scale.
 */
public final class HiDpi {
    private static boolean applied = false;
    private static int originalScale = -1;

    private HiDpi() {}

    public static int targetScale(Window w) {
        int current = ((WindowAccessor) (Object) w).getGuiScale();
        int physicalW = w.getWidth();
        // largest scale s in [1, current] whose guiScaledWidth (physicalW/s) is >= 1000, else 1
        int target = 1;
        for (int s = current; s >= 1; s--) {
            if (physicalW / s >= 1000) { target = s; break; }
        }
        // if even scale 1 is < 1000 wide (tiny window), just use current
        if (physicalW / 1 < 1000) target = Math.min(current, 2);
        return Math.max(1, Math.min(target, current));
    }

    /**
     * Single entry-point: call from the client tick and from every relevant screen's init().
     * Applies fine scale when the active screen implements HiDpiScreen; restores user scale otherwise.
     */
    public static void sync(Minecraft mc) {
        if (mc == null || mc.getWindow() == null) return;
        boolean wantFine = mc.screen instanceof HiDpiScreen;
        Window w = mc.getWindow();
        if (wantFine && !applied) {
            int current = ((WindowAccessor) (Object) w).getGuiScale();
            int target = targetScale(w);
            originalScale = current;
            applied = true;
            if (target != current) {
                w.setGuiScale(target);
                ((WindowAccessor) (Object) w).setGuiScaledWidth((int) Math.ceil((double) w.getWidth() / target));
                ((WindowAccessor) (Object) w).setGuiScaledHeight((int) Math.ceil((double) w.getHeight() / target));
                if (mc.screen != null) mc.screen.resize(w.getGuiScaledWidth(), w.getGuiScaledHeight());
            }
        } else if (!wantFine && applied) {
            if (originalScale > 0) w.setGuiScale(originalScale);
            applied = false;
            originalScale = -1;
            mc.resizeGui();
        }
        // wantFine&&applied or !wantFine&&!applied → no-op
    }
}
