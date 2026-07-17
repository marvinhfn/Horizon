package de.horizon.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuTextureView;
import de.horizon.HorizonClient;
import de.horizon.render.HorizonGlowState;
import de.horizon.render.PillarboxState;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlCommandEncoderMixin {

    @Inject(
        method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;"
               + "Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;"
               + "Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPassBackend;",
        at = @At("RETURN")
    )
    private void horizon$pillarboxViewport(
            Supplier<?> renderPassDescriptor, GpuTextureView colorTarget,
            OptionalInt clearColor, GpuTextureView depthTarget,
            OptionalDouble clearDepth, CallbackInfoReturnable<RenderPassBackend> cir) {
        horizon$applyPillarboxViewport(colorTarget);
    }

    @Inject(
        method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;"
               + "Ljava/util/OptionalInt;)Lcom/mojang/blaze3d/systems/RenderPassBackend;",
        at = @At("RETURN")
    )
    private void horizon$pillarboxViewportNoDepth(
            Supplier<?> renderPassDescriptor, GpuTextureView colorTarget,
            OptionalInt clearColor, CallbackInfoReturnable<RenderPassBackend> cir) {
        horizon$applyPillarboxViewport(colorTarget);
    }

    /**
     * Redirects the depth test disable in applyPipelineState for outline pipelines.
     * When HorizonGlowState.forceOutlineDepthTest is set, enables depth testing
     * instead of disabling it, making entity outlines respect scene depth.
     */
    @Redirect(
        method = "applyPipelineState",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_disableDepthTest()V")
    )
    private void horizon$forceOutlineDepthTest() {
        if (HorizonGlowState.forceOutlineDepthTest) {
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GL11.GL_LEQUAL);
        } else {
            GlStateManager._disableDepthTest();
        }
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
