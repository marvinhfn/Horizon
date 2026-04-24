package de.horizon.hud;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class PerformanceHudElement implements HudElement {
    @Override
    public String id() {
        return "performance_hud";
    }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isPerformanceHudEnabled();
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
        return 132;
    }

    @Override
    public int width(MinecraftClient client, HudPosition position) {
        return (int) Math.ceil(client.textRenderer.getWidth("FPS 999 | TPS 20.0 | PING 999") * position.getScale());
    }

    @Override
    public int height(MinecraftClient client, HudPosition position) {
        return (int) Math.ceil((client.textRenderer.fontHeight + 2) * position.getScale());
    }

    @Override
    public void render(DrawContext drawContext, MinecraftClient client, HudPosition position, boolean editorMode) {
        int ping = HorizonClient.getInstance().getPingService().getPing(client);
        String pingText = ping < 0 ? "..." : String.valueOf(ping);
        String text = editorMode
            ? "FPS 420 | TPS 20.0 | PING 12"
            : "FPS " + client.getCurrentFps() + " | TPS " + String.format("%.1f", HorizonClient.getInstance().getTpsTracker().getLastKnownTps()) + " | PING " + pingText;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(position.getX(), position.getY());
        drawContext.getMatrices().scale((float) position.getScale(), (float) position.getScale());
        drawContext.drawText(client.textRenderer, text, 0, 0, HudStyle.accent(), true);
        drawContext.getMatrices().popMatrix();
    }
}
