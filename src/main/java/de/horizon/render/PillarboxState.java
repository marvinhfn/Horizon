package de.horizon.render;

import de.horizon.HorizonClient;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;

public final class PillarboxState {
    /** True while GameRenderer.renderWorld is executing. */
    public static boolean inWorldRendering = false;

    private PillarboxState() {}

    /**
     * Returns the pillarbox bar width in scaled (GUI) pixels, or 0 if pillarbox
     * is inactive or the monitor is already 16:9 or narrower.
     */
    public static int scaledBarWidth() {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isPillarboxEnabled()) return 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return 0;
        Window window = mc.getWindow();
        int fbW = window.getWidth();
        int fbH = window.getHeight();
        if ((long) fbW * 9 <= (long) fbH * 16) return 0;
        int scaledH = window.getGuiScaledHeight();
        int sf = Math.max(1, Math.round((float) fbH / scaledH));
        int targetFbW = fbH * 16 / 9;
        int barFbW = (fbW - targetFbW) / 2;
        return (int) Math.ceil((double) barFbW / sf);
    }
}
