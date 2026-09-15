package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hypixel emits one {@link ClientboundPingPacket} with a non-zero id per server tick. Counting these
 * gives a true server-tick clock (Noamm's mechanism) that slows exactly when the server lags, so the
 * boss timers lag with the server instead of running on the client's 20/s loop.
 *
 * <p>Injected at {@code TAIL}, NOT {@code HEAD}: {@code handlePing} starts with
 * {@code PacketUtils.ensureRunningOnSameThread(...)}, which — when the packet arrives on the netty
 * thread — throws and reschedules the handler onto the main thread. A HEAD inject therefore fires
 * TWICE per packet (netty call before the throw + the main-thread re-run) and double-counts, making
 * the timers run twice as fast. TAIL is only reached on the completed main-thread run → exactly one
 * count per server tick.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Inject(method = "handlePing", at = @At("TAIL"))
    private void horizon$onServerPing(ClientboundPingPacket packet, CallbackInfo ci) {
        if (packet.getId() == 0) return;
        HorizonClient client = HorizonClient.getInstance();
        if (client != null) client.onServerPing();
    }
}
