package de.horizon.mixin;

import de.horizon.hypixel.HypixelSidebarOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExperienceBarRenderer.class)
public abstract class ExperienceBarMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Redirect(method = "extractBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ExperienceBarRenderer;top(Lcom/mojang/blaze3d/platform/Window;)I"))
    private int horizon$raiseExperienceBar(ExperienceBarRenderer instance, Window window) {
        int centerY = window.getGuiScaledHeight() - 29;
        if (!HypixelSidebarOverlay.shouldReplaceVanillaSidebar(minecraft)) {
            return centerY;
        }
        return Math.max(centerY - HypixelSidebarOverlay.HOTBAR_OFFSET, HypixelSidebarOverlay.BAR_HEIGHT);
    }
}
