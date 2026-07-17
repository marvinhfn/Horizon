package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.boss.RelicTimerService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RelicTimerHudElement implements HudElement {
    private final RelicTimerService timerService;

    public RelicTimerHudElement(RelicTimerService timerService) {
        this.timerService = timerService;
    }

    @Override public String id() { return "relic_timer_hud"; }
    @Override public boolean isEnabled(HorizonConfig config) { return config.isRelicTimerEnabled(); }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return 20; }
    @Override public int defaultY() { return 120; }

    @Override
    public int width(Minecraft client, HudPosition position) {
        return (int) Math.ceil(client.font.width("2.25s") * position.getScale());
    }

    @Override
    public int height(Minecraft client, HudPosition position) {
        return (int) Math.ceil((client.font.lineHeight + 2) * position.getScale());
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, Minecraft client, HudPosition position, boolean editorMode) {
        if (!editorMode && !timerService.isActive()) return;

        drawContext.pose().pushMatrix();
        drawContext.pose().translate(position.getX(), position.getY());
        drawContext.pose().scale((float) position.getScale(), (float) position.getScale());

        if (editorMode) {
            drawContext.text(client.font, "\u00a7a2.15s", 0, 0, HudStyle.accent(), true);
        } else {
            String text = "\u00a7a" + String.format("%.2fs", timerService.getSecondsRemaining());
            drawContext.text(client.font, text, 0, 0, 0xFFFFFFFF, true);
        }

        drawContext.pose().popMatrix();
    }
}
