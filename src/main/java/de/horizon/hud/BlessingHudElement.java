package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.DungeonStateService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * HUD element showing the current dungeon Power and Time blessing levels (from the tab-list footer),
 * one per line. The Time line is hidden when the Time blessing is 0.
 */
public final class BlessingHudElement implements HudElement {
    private static final String ID = "blessing";

    private final DungeonStateService stateService;

    public BlessingHudElement(DungeonStateService stateService) {
        this.stateService = stateService;
    }

    @Override public String id() { return ID; }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return 5; }
    @Override public int defaultY() { return 110; }
    @Override public int width(Minecraft mc, HudPosition pos) { return 70; }
    @Override public int height(Minecraft mc, HudPosition pos) { return 22; }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isBlessingHudEnabled();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Minecraft mc, HudPosition pos, boolean editMode) {
        int power, time;
        if (editMode) {
            power = 22;
            time = 5;
        } else {
            if (!stateService.isInDungeon()) return;
            power = stateService.getPowerBlessing();
            time = stateService.getTimeBlessing();
        }
        int x = pos.getX();
        int y = pos.getY();
        boolean showTime = time > 0;

        int rows = showTime ? 2 : 1;
        ctx.fill(x - 1, y - 1, x + 71, y + 1 + rows * 11, 0x80000000);
        if (mc.font == null) return;
        ctx.text(mc.font, "§dPower: §f" + power, x + 2, y + 1, 0xFFFFFFFF);
        if (showTime) {
            ctx.text(mc.font, "§bTime: §f" + time, x + 2, y + 12, 0xFFFFFFFF);
        }
    }
}
