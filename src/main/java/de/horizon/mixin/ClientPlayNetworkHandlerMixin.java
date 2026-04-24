package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.UpdateTickRateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/DebugHud;shouldShowPacketSizeAndPingCharts()Z"))
    private boolean horizon$alwaysCollectPingSamples(DebugHud debugHud) {
        return true;
    }

    @Inject(method = "onUpdateTickRate", at = @At("TAIL"))
    private void horizon$trackTps(UpdateTickRateS2CPacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null) {
            client.getTpsTracker().update(packet.tickRate());
        }
    }

    @Inject(method = "onWorldTimeUpdate", at = @At("TAIL"))
    private void horizon$trackWorldTimePacket(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null) {
            client.getTpsTracker().onWorldTimePacket();
        }
    }
}
