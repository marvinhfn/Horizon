package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;showNetworkCharts()Z"))
    private boolean horizon$alwaysCollectPingSamples(DebugScreenOverlay debugHud) {
        return true;
    }

    @Inject(method = "handleTickingState", at = @At("TAIL"))
    private void horizon$trackTps(ClientboundTickingStatePacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null) {
            client.getTpsTracker().update(packet.tickRate());
        }
    }

    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void horizon$trackWorldTimePacket(ClientboundSetTimePacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null) {
            client.getTpsTracker().onWorldTimePacket();
        }
    }

}
