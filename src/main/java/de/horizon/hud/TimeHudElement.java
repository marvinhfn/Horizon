package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class TimeHudElement implements HudElement {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public String id() {
        return "time_hud";
    }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isTimeHudEnabled();
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
        return 110;
    }

    @Override
    public int width(MinecraftClient client, HudPosition position) {
        return (int) Math.ceil(client.textRenderer.getWidth("TIME 00:00:00") * position.getScale());
    }

    @Override
    public int height(MinecraftClient client, HudPosition position) {
        return (int) Math.ceil((client.textRenderer.fontHeight + 2) * position.getScale());
    }

    @Override
    public void render(DrawContext drawContext, MinecraftClient client, HudPosition position, boolean editorMode) {
        String text = editorMode ? "TIME 14:38:12" : "TIME " + LocalTime.now().format(FORMATTER);
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(position.getX(), position.getY());
        drawContext.getMatrices().scale((float) position.getScale(), (float) position.getScale());
        drawContext.drawText(client.textRenderer, text, 0, 0, HudStyle.accent(), true);
        drawContext.getMatrices().popMatrix();
    }
}
