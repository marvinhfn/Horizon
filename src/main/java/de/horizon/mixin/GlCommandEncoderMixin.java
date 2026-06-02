package de.horizon.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import de.horizon.HorizonClient;
import de.horizon.render.PillarboxState;
import net.minecraft.client.gl.GlCommandEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {

    @Inject(
        method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;"
               + "Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;"
               + "Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;",
        at = @At("RETURN")
    )
    private void horizon$pillarboxViewport(
            Supplier<?> renderPassDescriptor, GpuTextureView colorTarget,
            OptionalInt clearColor, GpuTextureView depthTarget,
            OptionalDouble clearDepth, CallbackInfoReturnable<RenderPass> cir) {
        horizon$applyPillarboxViewport(colorTarget);
    }

    @Inject(
        method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;"
               + "Ljava/util/OptionalInt;)Lcom/mojang/blaze3d/systems/RenderPass;",
        at = @At("RETURN")
    )
    private void horizon$pillarboxViewportNoDepth(
            Supplier<?> renderPassDescriptor, GpuTextureView colorTarget,
            OptionalInt clearColor, CallbackInfoReturnable<RenderPass> cir) {
        horizon$applyPillarboxViewport(colorTarget);
    }

    @Unique
    private static void horizon$applyPillarboxViewport(GpuTextureView colorTarget) {
        if (colorTarget == null) return;
        if (!PillarboxState.inWorldRendering) return;
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isPillarboxEnabled()) return;
        int w = colorTarget.getWidth(0);
        int h = colorTarget.getHeight(0);
        if ((long) w * 9 <= (long) h * 16) return;
        int targetW = h * 16 / 9;
        int barW    = (w - targetW) / 2;
        GlStateManager._viewport(barW, 0, targetW, h);
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(barW, 0, targetW, h);
    }
}
