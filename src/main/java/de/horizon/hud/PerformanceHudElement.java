package de.horizon.hud;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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
    public int width(Minecraft client, HudPosition position) {
        return (int) Math.ceil(client.font.width("FPS 999 | TPS 20.0 | PING 999") * position.getScale());
    }

    @Override
    public int height(Minecraft client, HudPosition position) {
        return (int) Math.ceil((client.font.lineHeight + 2) * position.getScale());
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, Minecraft client, HudPosition position, boolean editorMode) {
        int ping = HorizonClient.getInstance().getPingService().getPing(client);
        String pingText = ping < 0 ? "..." : String.valueOf(ping);
        String text = editorMode
            ? "FPS 420 | TPS 20.0 | PING 12"
            : "FPS " + client.getFps() + " | TPS " + String.format("%.1f", HorizonClient.getInstance().getTpsTracker().getLastKnownTps()) + " | PING " + pingText;

        drawContext.pose().pushMatrix();
        drawContext.pose().translate(position.getX(), position.getY());
        drawContext.pose().scale((float) position.getScale(), (float) position.getScale());
        drawContext.text(client.font, text, 0, 0, HudStyle.accent(), true);
        drawContext.pose().popMatrix();
    }
}
