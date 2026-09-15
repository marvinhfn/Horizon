package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hypixel emits one {@link ClientboundPingPacket} with a non-zero id per server tick. Counting
 * these gives a true server-tick clock (Noamm's mechanism) for the boss timers.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Inject(method = "handlePing", at = @At("HEAD"))
    private void horizon$onServerPing(ClientboundPingPacket packet, CallbackInfo ci) {
        if (packet.getId() == 0) return;
        HorizonClient client = HorizonClient.getInstance();
        if (client != null) client.onServerPing();
    }
}
