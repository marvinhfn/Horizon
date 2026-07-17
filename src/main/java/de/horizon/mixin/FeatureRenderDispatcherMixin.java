package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.render.HorizonGlowState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherMixin {

    @Inject(method = "renderSolidFeatures", at = @At("HEAD"))
    private void horizon$copyDepthBeforeSolidFeatures(CallbackInfo ci) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) return;
        if (!horizon.getDungeonStateService().isInDungeon()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.levelRenderer == null) return;

        var outlineTarget = mc.levelRenderer.entityOutlineTarget();
        var mainTarget = mc.getMainRenderTarget();
        if (outlineTarget == null || mainTarget == null) return;

        outlineTarget.copyDepthFrom(mainTarget);
        HorizonGlowState.forceOutlineDepthTest = true;
    }
}
