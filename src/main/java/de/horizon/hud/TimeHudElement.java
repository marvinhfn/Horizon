package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.screen.render.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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
    public int width(Minecraft client, HudPosition position) {
        return (int) Math.ceil(client.font.width("TIME 00:00:00") * position.getScale());
    }

    @Override
    public int height(Minecraft client, HudPosition position) {
        return (int) Math.ceil((client.font.lineHeight + 2) * position.getScale());
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, Minecraft client, HudPosition position, boolean editorMode) {
        String text = editorMode ? "TIME 14:38:12" : "TIME " + LocalTime.now().format(FORMATTER);
        int guiW = client.getWindow().getGuiScaledWidth();
        int guiH = client.getWindow().getGuiScaledHeight();
        position.migrateIfNeeded(guiW, guiH);
        drawContext.pose().pushMatrix();
        drawContext.pose().translate(position.resolveX(guiW), position.resolveY(guiH));
        drawContext.pose().scale((float) position.getScale(), (float) position.getScale());
        drawContext.text(client.font, Fonts.hud(text), 0, 0, HudStyle.accent(), true);
        drawContext.pose().popMatrix();
    }
}
