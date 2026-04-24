package de.horizon.mixin;

import de.horizon.hypixel.HypixelSidebarOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExperienceBar.class)
public abstract class ExperienceBarMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Redirect(method = "renderBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/bar/ExperienceBar;getCenterY(Lnet/minecraft/client/util/Window;)I"))
    private int horizon$raiseExperienceBar(ExperienceBar instance, Window window) {
        int centerY = window.getScaledHeight() - 29;
        if (!HypixelSidebarOverlay.shouldReplaceVanillaSidebar(client)) {
            return centerY;
        }
        return Math.max(centerY - HypixelSidebarOverlay.HOTBAR_OFFSET, HypixelSidebarOverlay.BAR_HEIGHT);
    }
}
