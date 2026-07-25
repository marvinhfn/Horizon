package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses the vanilla floating name label on the Arrow Align device frames (the framed item's
 * name that shows when you look at a frame). The solver draws its own rotation-count overlay, so the
 * blank/space item names are just noise.
 */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/decoration/ItemFrame;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void horizon$hideArrowDeviceName(ItemFrame frame, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        HorizonClient h = HorizonClient.getInstance();
        if (h != null && h.shouldHideArrowFrameName(frame)) cir.setReturnValue(false);
    }
}
