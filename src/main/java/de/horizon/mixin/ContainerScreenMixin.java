package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The chest panel texture is drawn by {@code ContainerScreen.extractBackground} (an override of
 * {@code Screen.extractBackground}), which the GUI framework calls directly — separately from
 * {@code extractRenderState}/{@code extractContents} that {@link HandledScreenMixin} cancels. So it
 * must be cancelled here too, otherwise the chest panel keeps showing behind the terminal overlay.
 */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void horizon$cancelPanelForCustomMode(
            GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h != null && h.shouldHideVanillaContainer((AbstractContainerScreen<?>) (Object) this)) {
            ci.cancel();
        }
    }
}
