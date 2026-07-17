package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

    @Redirect(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z"))
    private boolean horizon$suppressFireOverlay(LocalPlayer player) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon != null && horizon.getConfigManager().getConfig().isFireOverlayDisabled()) {
            return false;
        }
        return player.isOnFire();
    }
}
