package de.horizon.hud;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.misc.SystemStatsService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class SystemStatsHudElement implements HudElement {
    @Override
    public String id() {
        return "system_hud";
    }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isSystemHudEnabled();
    }

    @Override
    public boolean isMovable() {
        return true;
    }

    @Override
    public int defaultX() {
        return 20;
    }

    @Override
    public int defaultY() {
        return 154;
    }

    @Override
    public int width(MinecraftClient client, HudPosition position) {
        int widest = Math.max(client.textRenderer.getWidth("CPU 100% | 99C"), client.textRenderer.getWidth("GPU 100% | 99C"));
        return (int) Math.ceil(widest * position.getScale());
    }

    @Override
    public int height(MinecraftClient client, HudPosition position) {
        return (int) Math.ceil(((client.textRenderer.fontHeight * 2) + 4) * position.getScale());
    }

    @Override
    public void render(DrawContext drawContext, MinecraftClient client, HudPosition position, boolean editorMode) {
        SystemStatsService service = HorizonClient.getInstance().getSystemStatsService();
        service.requestUpdate();
        String cpuText = editorMode
            ? "CPU 38% | 62C"
            : "CPU " + Math.round(service.getCpuLoad()) + "% | " + formatTemp(service.getCpuTemp());
        String gpuText = editorMode
            ? "GPU 72% | 65C"
            : "GPU " + formatPercent(service.getGpuUsage()) + " | " + formatTemp(service.getGpuTemp());

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(position.getX(), position.getY());
        drawContext.getMatrices().scale((float) position.getScale(), (float) position.getScale());
        drawContext.drawText(client.textRenderer, cpuText, 0, 0, HudStyle.accent(), true);
        drawContext.drawText(client.textRenderer, gpuText, 0, client.textRenderer.fontHeight + 4, HudStyle.muted(), true);
        drawContext.getMatrices().popMatrix();
    }

    private String formatTemp(Double value) {
        return value == null || value.isNaN() ? "n/a" : Math.round(value) + "C";
    }

    private String formatPercent(Double value) {
        return value == null ? "n/a" : Math.round(value) + "%";
    }
}
