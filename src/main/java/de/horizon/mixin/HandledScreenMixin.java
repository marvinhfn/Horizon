package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin on AbstractContainerScreen:
 * - Renders inventory buttons before tooltip
 * - Cancels container rendering when terminal custom mode is active
 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected Slot hoveredSlot;
    @Shadow protected abstract Slot getHoveredSlot(double mouseX, double mouseY);

    @Inject(method = "extractTooltip", at = @At("HEAD"))
    private void horizon$renderButtonsBeforeTooltip(
            GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h == null) return;
        h.getInventoryButtonOverlay().render(
                (AbstractContainerScreen<?>) (Object) this, context, mouseX, mouseY);
    }

    /**
     * Cancel container content rendering (slots, items, labels) when terminal custom mode or leap menu is active.
     */
    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void horizon$cancelContainerForCustomMode(
            GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (shouldCancelContainerRendering()) {
            this.hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
            ci.cancel();
        }
    }

    /**
     * Cancel the container render state when terminal custom mode or leap menu is active.
     */
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void horizon$cancelBgForCustomMode(
            GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (shouldCancelContainerRendering()) {
            this.hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
            ci.cancel();
        }
    }

    private boolean shouldCancelContainerRendering() {
        HorizonClient h = HorizonClient.getInstance();
        return h != null && h.shouldHideVanillaContainer((AbstractContainerScreen<?>) (Object) this);
    }
}
