package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the potion-effect icons shown beside the inventory when the option is enabled. */
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void horizon$hideInventoryEffects(GuiGraphicsExtractor ctx, int mouseX, int mouseY, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h != null && h.getConfigManager().getConfig().isHideStatusEffects()) ci.cancel();
    }
}
