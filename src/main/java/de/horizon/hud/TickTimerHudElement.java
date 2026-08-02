package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.TickTimerService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TickTimerHudElement implements HudElement {
    private final TickTimerService timerService;
    private final DungeonStateService dungeonState;

    public TickTimerHudElement(TickTimerService timerService, DungeonStateService dungeonState) {
        this.timerService = timerService;
        this.dungeonState = dungeonState;
    }

    @Override public String id() { return "tick_timer_hud"; }
    @Override public boolean isEnabled(HorizonConfig config) { return config.isTickTimerEnabled(); }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return 20; }
    @Override public int defaultY() { return 80; }

    @Override
    public int width(Minecraft client, HudPosition position) {
        String sample = timerService.isActive() ? timerService.getDisplayText() : "Goldor: 3.00s";
        return (int) Math.ceil(client.font.width(sample) * position.getScale());
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
            drawContext.text(client.font, "\u00a7bGoldor: \u00a7a1.95s", 0, 0, HudStyle.accent(), true);
        } else {
            drawContext.text(client.font, timerService.getDisplayText(), 0, 0, 0xFFFFFFFF, true);
        }

        drawContext.pose().popMatrix();
    }
}
