package de.horizon.mixin;

import de.horizon.HorizonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
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

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void horizon$onPlayerPosition(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client == null) return;
        if (!packet.relatives().isEmpty()) return;
        var change = packet.change();
        var pos = change.position();
        // Only handle absolute teleports at y=69.5 with 0.5 aligned coords (teleport pads)
        if (pos.y != 69.5 || pos.x % 0.5 != 0.0 || pos.z % 0.5 != 0.0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        client.onTeleportMaze(pos.x, pos.z, mc.player.getX(), mc.player.getZ(), change.yRot());
    }

    @Inject(method = "handleMapItemData", at = @At("TAIL"))
    private void horizon$onMapItemData(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        MapItemSavedData mapData = mc.level.getMapData(packet.mapId());
        if (mapData == null) return;
        // Pass the KEYED decoration map (icon-0, icon-1, …) so teammate heads can be matched by index.
        var decorations = ((de.horizon.mixin.MapItemSavedDataAccessor) mapData).getDecorationsMap();
        client.onMapItemData(mapData.colors, decorations, mapData.centerX, mapData.centerZ, mapData.scale);
    }

    @Inject(method = "handleTabListCustomisation", at = @At("TAIL"))
    private void horizon$onTabList(ClientboundTabListPacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null && packet.footer() != null) {
            client.onTabFooter(packet.footer().getString());
        }
    }

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void horizon$onLevelParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        // Dragon spawn detection: FLAME particles with count=20, xDist=2, yDist=3, zDist=2, maxSpeed=0
        if (packet.getParticle().getType() != ParticleTypes.FLAME) return;
        if (packet.getCount() != 20) return;
        if (packet.getXDist() != 2f || packet.getYDist() != 3f || packet.getZDist() != 2f) return;
        if (packet.getMaxSpeed() != 0f) return;
        double px = packet.getX(), pz = packet.getZ();
        if (px % 1 != 0.0 || pz % 1 != 0.0) return;
        if (packet.getY() != 19.0) return;
        HorizonClient client = HorizonClient.getInstance();
        if (client != null) {
            client.onDragonParticle((int) px, (int) pz);
        }
    }

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void horizon$onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                net.minecraft.world.level.block.state.BlockState oldState = mc.level.getBlockState(packet.getPos());
                client.onBlockUpdate(packet.getPos(), packet.getBlockState(), oldState, mc);
            }
        }
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void horizon$onChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        HorizonClient client = HorizonClient.getInstance();
        if (client == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        // Pass 1: count air blocks at x=110 for Simon Says reset, then clear if needed
        int[] airCount = {0};
        packet.runUpdates((pos, state) -> {
            if (state.isAir() && pos.getX() == 110
                && pos.getY() >= 120 && pos.getY() <= 123
                && pos.getZ() >= 92 && pos.getZ() <= 95) {
                airCount[0]++;
            }
        });
        if (airCount[0] >= 16) {
            client.onSimonSaysReset();
        }
        // Pass 2: process block updates (sea lanterns for Simon Says, puzzle changes, etc.)
        packet.runUpdates((pos, state) -> {
            client.onBlockUpdate(pos, state, null, mc);
        });
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void horizon$onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() != 3) return;
        HorizonClient client = HorizonClient.getInstance();
        if (client == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        var entity = packet.getEntity(mc.level);
        if (entity instanceof Zombie zombie) {
            de.horizon.HorizonMod.LOGGER.info("[Mimic] Zombie death event: isBaby={}", zombie.isBaby());
            if (zombie.isBaby()) {
                client.onMimicKill();
            }
        }
    }
}
