package de.horizon.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Window.class)
public interface WindowAccessor {
    @Accessor("guiScaledWidth")
    void setGuiScaledWidth(int width);

    @Accessor("guiScaledHeight")
    void setGuiScaledHeight(int height);

    @Accessor("guiScale")
    int getGuiScale();
}
