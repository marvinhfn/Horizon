package de.horizon.mixin;

import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the {@code area} field of the private inner class
 * {@code ChatInputSuggestor$SuggestionWindow} so we can reposition
 * the window after it is created.
 */
@Mixin(targets = "net.minecraft.client.gui.components.CommandSuggestions$SuggestionsList")
public interface SuggestionWindowAccessor {

    @Accessor("area")
    Rect2i getArea();

    @Accessor("area")
    void setArea(Rect2i area);
}
