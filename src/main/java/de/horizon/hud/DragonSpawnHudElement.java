package de.horizon.hud;

import de.horizon.config.ConfigManager;
import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.boss.DragonService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shows the countdown until the priority M7 wither dragon spawns (before it materialises). */
public final class DragonSpawnHudElement implements HudElement {
    private final DragonService dragonService;
    private final ConfigManager configManager;

    public DragonSpawnHudElement(DragonService dragonService, ConfigManager configManager) {
        this.dragonService = dragonService;
        this.configManager = configManager;
    }

    @Override public String id() { return "dragon_spawn_hud"; }
    @Override public boolean isEnabled(HorizonConfig config) { return config.isDragonEnabled() && config.isDragonTimer(); }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return 20; }
    @Override public int defaultY() { return 140; }

    @Override
    public int width(Minecraft client, HudPosition position) {
        return (int) Math.ceil(client.font.width("Power 2.5s") * position.getScale());
    }

    @Override
    public int height(Minecraft client, HudPosition position) {
        return (int) Math.ceil((client.font.lineHeight + 2) * position.getScale());
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, Minecraft client, HudPosition position, boolean editorMode) {
        if (!editorMode && !dragonService.isSpawnTimerActive(configManager.getConfig())) return;

        drawContext.pose().pushMatrix();
        drawContext.pose().translate(position.getX(), position.getY());
        drawContext.pose().scale((float) position.getScale(), (float) position.getScale());

        String text = editorMode ? "§cPower §e2.5s" : dragonService.getSpawnTimerText(configManager.getConfig());
        drawContext.text(client.font, text, 0, 0, 0xFFFFFFFF, true);

        drawContext.pose().popMatrix();
    }
}
