package de.horizon.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int getLeftPos();

    @Accessor("leftPos")
    void setLeftPos(int leftPos);

    @Accessor("topPos")
    int getTopPos();

    @Accessor("topPos")
    void setTopPos(int topPos);

    @Accessor("imageWidth")
    int getImageWidth();

    @Accessor("imageHeight")
    int getImageHeight();

    @Accessor("hoveredSlot")
    Slot getHoveredSlot();
}
