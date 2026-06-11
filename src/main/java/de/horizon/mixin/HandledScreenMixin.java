package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders inventory buttons just before {@code drawMouseoverTooltip} so that
 * vanilla item tooltips appear on top of the button layer.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "extractTooltip", at = @At("HEAD"))
    private void horizon$renderButtonsBeforeTooltip(
            GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h == null) return;
        h.getInventoryButtonOverlay().render(
                (AbstractContainerScreen<?>) (Object) this, context, mouseX, mouseY);
    }
}
