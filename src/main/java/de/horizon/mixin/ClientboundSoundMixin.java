package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla Etherwarp teleport sound with the configured custom sound at the exact moment
 * the server plays it — so the timing matches vanilla/Odin and the original tone doesn't double up.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientboundSoundMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
    private void horizon$replaceEtherwarpSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        if (h == null) return;
        // The Etherwarp sound is ENDER_DRAGON_HURT at pitch 0.53968257.
        if (packet.getSound().value() != SoundEvents.ENDER_DRAGON_HURT) return;
        if (Math.abs(packet.getPitch() - 0.53968257f) > 0.001f) return;
        if (h.replaceEtherwarpSound()) ci.cancel(); // cancel vanilla + play the custom sound
    }
}
