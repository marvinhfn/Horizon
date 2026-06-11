package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface HudElement {
    String id();

    boolean isEnabled(HorizonConfig config);

    boolean isMovable();

    int defaultX();

    int defaultY();

    int width(Minecraft client, HudPosition position);

    int height(Minecraft client, HudPosition position);

    void render(GuiGraphicsExtractor drawContext, Minecraft client, HudPosition position, boolean editorMode);
}
