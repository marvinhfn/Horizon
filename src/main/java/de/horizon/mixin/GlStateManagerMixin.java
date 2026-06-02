package de.horizon.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import de.horizon.HorizonClient;
import de.horizon.render.PillarboxState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlStateManager.class, remap = false)
public class GlStateManagerMixin {

    @Unique
    private static boolean horizon$redirecting = false;

    @Inject(method = "_viewport", at = @At("HEAD"), cancellable = true)
    private static void horizon$pillarboxViewport(int x, int y, int width, int height, CallbackInfo ci) {
        if (horizon$redirecting) return;
        if (!PillarboxState.inWorldRendering) return;

        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isPillarboxEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        Window window = mc.getWindow();
        int fbW = window.getFramebufferWidth();
        int fbH = window.getFramebufferHeight();
        if ((long) fbW * 9 <= (long) fbH * 16) return;

        if (x == 0 && y == 0 && width == fbW && height == fbH) {
            int targetW = fbH * 16 / 9;
            int barW = (fbW - targetW) / 2;
            horizon$redirecting = true;
            GlStateManager._viewport(barW, 0, targetW, fbH);
            horizon$redirecting = false;
            ci.cancel();
        }
    }
}
