package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders inventory buttons just before {@code drawMouseoverTooltip} so that
 * vanilla item tooltips appear on top of the button layer.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"))
    private void horizon$renderButtonsBeforeTooltip(
            DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h == null) return;
        h.getInventoryButtonOverlay().render(
                (HandledScreen<?>) (Object) this, context, mouseX, mouseY);
    }
}
