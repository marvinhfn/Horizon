package de.horizon.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.horizon.HorizonClient;
import de.horizon.feature.dungeon.terminal.TerminalSolverService;
import de.horizon.render.PillarboxState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow private GameRenderState gameRenderState;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void horizon$renderWorldHead(DeltaTracker tickCounter, CallbackInfo ci) {
        PillarboxState.inWorldRendering = true;
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isPillarboxEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Window window = mc.getWindow();
        int fbW = window.getWidth();
        int fbH = window.getHeight();
        if ((long) fbW * 9 <= (long) fbH * 16) return;
        int targetW = fbH * 16 / 9;
        int barW = (fbW - targetW) / 2;
        GlStateManager._viewport(barW, 0, targetW, fbH);
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(barW, 0, targetW, fbH);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void horizon$renderWorldReturn(DeltaTracker tickCounter, CallbackInfo ci) {
        PillarboxState.inWorldRendering = false;
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isPillarboxEnabled()) return;
        GlStateManager._disableScissorTest();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            Window window = mc.getWindow();
            GlStateManager._viewport(0, 0, window.getWidth(), window.getHeight());
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void horizon$scaleHurtCam(CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo ci) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) return;
        float intensity = horizon.getConfigManager().getConfig().getHurtCamIntensity();
        if (intensity >= 1.0f) return;
        ci.cancel();
        if (!cameraRenderState.entityRenderState.isLiving) return;
        float hurtTime = cameraRenderState.entityRenderState.hurtTime;
        if (cameraRenderState.entityRenderState.isDeadOrDying) {
            float deathTime = Math.min(cameraRenderState.entityRenderState.deathTime, 20.0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(40.0f - 8000.0f / (deathTime + 200.0f)));
        }
        if (hurtTime < 0) return;
        hurtTime = hurtTime / (float) cameraRenderState.entityRenderState.hurtDuration;
        hurtTime = hurtTime * hurtTime * hurtTime * hurtTime * Mth.PI;
        float hurtDir = cameraRenderState.entityRenderState.hurtDir;
        double tiltStrength = gameRenderState.optionsRenderState.damageTiltStrength * intensity;
        poseStack.mulPose(Axis.YP.rotationDegrees(-hurtDir));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-(double) hurtTime * 14.0 * tiltStrength)));
        poseStack.mulPose(Axis.YP.rotationDegrees(hurtDir));
    }

    // Terminal GUI scale is now handled entirely inside the terminal solver overlay
    // (TerminalSolverService.render scales by terminalGuiScale), so the window-level
    // guiScale hack that scaled the now-hidden vanilla chest was removed.
}
