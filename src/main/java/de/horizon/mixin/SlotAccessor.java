package de.horizon.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes setters for the {@code final} slot positions so the storage overlay can relocate the real
 * container slots into its combined grid — letting vanilla handle every click, drag and tooltip.
 */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable
    @Accessor("x")
    void setX(int x);

    @Mutable
    @Accessor("y")
    void setY(int y);
}
