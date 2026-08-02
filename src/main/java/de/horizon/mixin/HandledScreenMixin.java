package de.horizon.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import de.horizon.HorizonClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        h.getInventoryButtonOverlay().render(screen, context, mouseX, mouseY);
        // Experimentation Table overlay (Superpairs kept items etc.) — before the tooltip.
        h.renderExperimentTableOverlay(screen, context);
        // Croesus profit overlay — before the tooltip so it sits behind item tooltips.
        h.renderCroesusOverlay(screen, context);
    }

    // ── Experimentation Table: replace the displayed stack of solved slots ──
    // Rendering each face-down tile as its remembered reward (Superpairs) / number (Ultrasequencer)
    // so the placeholder glass is gone and the item + count draw natively.

    @ModifyArg(method = "extractSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V"))
    private ItemStack horizon$experimentItem(ItemStack stack, @Local(argsOnly = true) Slot slot) {
        return horizon$modifyExperimentStack(slot, stack);
    }

    @ModifyArg(method = "extractSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;III)V"))
    private ItemStack horizon$experimentFakeItem(ItemStack stack, @Local(argsOnly = true) Slot slot) {
        return horizon$modifyExperimentStack(slot, stack);
    }

    @ModifyArg(method = "extractSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private ItemStack horizon$experimentDecorations(ItemStack stack, @Local(argsOnly = true) Slot slot) {
        return horizon$modifyExperimentStack(slot, stack);
    }

    // The hovered-slot stack the tooltip is built from — swap it so the tooltip shows the reward.
    @ModifyVariable(method = "extractTooltip", at = @At("STORE"), ordinal = 0)
    private ItemStack horizon$experimentTooltipStack(ItemStack stack) {
        return horizon$modifyExperimentStack(this.hoveredSlot, stack);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void horizon$experimentSlotClick(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h == null || slot == null) return;
        h.onExperimentSlotClick((AbstractContainerScreen<?>) (Object) this, slotId, slot.getItem(), button);
    }

    private ItemStack horizon$modifyExperimentStack(Slot slot, ItemStack stack) {
        HorizonClient h = HorizonClient.getInstance();
        if (h == null) return stack;
        return h.modifyExperimentStack((AbstractContainerScreen<?>) (Object) this, slot, stack);
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

    /** Suppress the vanilla title/inventory labels while the storage overlay relocates the slots. */
    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void horizon$cancelLabelsForStorage(GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h != null && h.isStoragePageActive((AbstractContainerScreen<?>) (Object) this)) ci.cancel();
    }
}
