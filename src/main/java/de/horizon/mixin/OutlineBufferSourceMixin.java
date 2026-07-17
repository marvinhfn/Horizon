package de.horizon.mixin;

import de.horizon.render.HorizonGlowState;
import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OutlineBufferSource.class)
public class OutlineBufferSourceMixin {

    @Inject(method = "endOutlineBatch", at = @At("RETURN"))
    private void horizon$resetDepthFlag(CallbackInfo ci) {
        HorizonGlowState.forceOutlineDepthTest = false;
    }
}
