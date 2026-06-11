package de.horizon.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import de.horizon.HorizonClient;
import de.horizon.render.PillarboxState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

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

    // Projection matrix override removed — getProjectionMatrix no longer exists in 26.1.2.
    // The viewport-based pillarbox approach (renderLevel HEAD/RETURN) still applies.
}
