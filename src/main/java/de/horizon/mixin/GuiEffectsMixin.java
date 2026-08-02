package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the top-right potion-effect icons in the HUD when the option is enabled. */
@Mixin(Gui.class)
public abstract class GuiEffectsMixin {
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void horizon$hideStatusEffects(GuiGraphicsExtractor ctx, DeltaTracker delta, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h != null && h.getConfigManager().getConfig().isHideStatusEffects()) ci.cancel();
    }
}
