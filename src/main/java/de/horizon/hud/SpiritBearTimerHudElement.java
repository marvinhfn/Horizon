package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.boss.SpiritBearService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SpiritBearTimerHudElement implements HudElement {
    private final SpiritBearService bearService;

    public SpiritBearTimerHudElement(SpiritBearService bearService) {
        this.bearService = bearService;
    }

    @Override public String id() { return "spirit_bear_timer"; }
    @Override public boolean isEnabled(HorizonConfig config) { return config.isSpiritBearTimerEnabled(); }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return 20; }
    @Override public int defaultY() { return 110; }

    @Override
    public int width(Minecraft client, HudPosition position) {
        return (int) Math.ceil(client.font.width("25/30") * position.getScale());
    }

    @Override
    public int height(Minecraft client, HudPosition position) {
        return (int) Math.ceil((client.font.lineHeight + 2) * position.getScale());
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, Minecraft client, HudPosition position, boolean editorMode) {
        if (!editorMode && !bearService.isInF4Boss()) return;

        drawContext.pose().pushMatrix();
        drawContext.pose().translate(position.getX(), position.getY());
        drawContext.pose().scale((float) position.getScale(), (float) position.getScale());

        String text;
        if (editorMode) {
            text = "§d3.40";
        } else if (bearService.hasCountdown()) {
            float seconds = bearService.getCountdownSeconds(client);
            text = "§d" + String.format("%.2f", seconds);
        } else {
            text = "§d" + bearService.getCount() + "/" + bearService.getTotal();
        }

        drawContext.text(client.font, text, 0, 0, 0xFFFFFFFF, true);
        drawContext.pose().popMatrix();
    }
}
