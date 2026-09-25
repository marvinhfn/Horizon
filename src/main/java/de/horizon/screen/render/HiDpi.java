package de.horizon.screen.render;

import com.mojang.blaze3d.platform.Window;
import de.horizon.mixin.WindowAccessor;
import net.minecraft.client.Minecraft;

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

    public static void enter(Minecraft mc) {
        if (applied || mc == null || mc.getWindow() == null) return;
        Window w = mc.getWindow();
        int current = ((WindowAccessor) (Object) w).getGuiScale();
        int target = targetScale(w);
        originalScale = current;
        applied = true;
        if (target == current) return;
        w.setGuiScale(target);
        ((WindowAccessor) (Object) w).setGuiScaledWidth((int) Math.ceil((double) w.getWidth() / target));
        ((WindowAccessor) (Object) w).setGuiScaledHeight((int) Math.ceil((double) w.getHeight() / target));
        if (mc.screen != null) {
            mc.screen.resize(w.getGuiScaledWidth(), w.getGuiScaledHeight());
        }
    }

    public static void exit(Minecraft mc) {
        if (!applied || mc == null || mc.getWindow() == null) { applied = false; return; }
        Window w = mc.getWindow();
        if (originalScale > 0) {
            w.setGuiScale(originalScale);
        }
        applied = false;
        originalScale = -1;
        mc.resizeGui(); // restores guiScaledWidth/Height + re-lays any active screen from options
    }
}
