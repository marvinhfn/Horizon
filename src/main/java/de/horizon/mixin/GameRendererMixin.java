package de.horizon.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import de.horizon.HorizonClient;
import de.horizon.render.PillarboxState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void horizon$renderWorldHead(RenderTickCounter tickCounter, CallbackInfo ci) {
        PillarboxState.inWorldRendering = true;
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isPillarboxEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        Window window = mc.getWindow();
        int fbW = window.getFramebufferWidth();
        int fbH = window.getFramebufferHeight();
        if ((long) fbW * 9 <= (long) fbH * 16) return;
        int targetW = fbH * 16 / 9;
        int barW = (fbW - targetW) / 2;
        GlStateManager._viewport(barW, 0, targetW, fbH);
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(barW, 0, targetW, fbH);
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void horizon$renderWorldReturn(RenderTickCounter tickCounter, CallbackInfo ci) {
        PillarboxState.inWorldRendering = false;
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isPillarboxEnabled()) return;
        GlStateManager._disableScissorTest();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            Window window = mc.getWindow();
            GlStateManager._viewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
        }
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("HEAD"), cancellable = true)
    private void horizon$pillarboxProjection(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isPillarboxEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        Window window = mc.getWindow();
        int w = window.getFramebufferWidth();
        int h = window.getFramebufferHeight();
        if ((long) w * 9 <= (long) h * 16) return;

        GameRenderer self = (GameRenderer) (Object) this;
        float farPlane = self.getFarPlaneDistance();
        Matrix4f matrix = new Matrix4f().perspective(
                (float) Math.toRadians(fov),
                16.0f / 9.0f,
                0.05f,
                farPlane
        );
        cir.setReturnValue(matrix);
    }
}
