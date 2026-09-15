package de.horizon.mixin;

import de.horizon.HorizonClient;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Exact port of NoammAddons' MixinConnection tick clock: Hypixel sends one
 * {@link ClientboundPingPacket} with a non-zero id per server tick. We hook the raw netty inbound
 * entry point {@code Connection.channelRead0}, which fires exactly ONCE per received packet (no
 * thread-reschedule / double dispatch, unlike {@code handlePing}), and increment the server-tick
 * counter. This makes the boss timers advance on real server ticks and lag with the server.
 */
@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void horizon$countServerTick(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundPingPacket ping && ping.getId() != 0) {
            HorizonClient client = HorizonClient.getInstance();
            if (client != null) client.onServerPing();
        }
    }
}
