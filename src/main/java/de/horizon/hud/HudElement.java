package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public interface HudElement {
    String id();

    boolean isEnabled(HorizonConfig config);

    boolean isMovable();

    int defaultX();

    int defaultY();

    int width(MinecraftClient client, HudPosition position);

    int height(MinecraftClient client, HudPosition position);

    void render(DrawContext drawContext, MinecraftClient client, HudPosition position, boolean editorMode);
}
