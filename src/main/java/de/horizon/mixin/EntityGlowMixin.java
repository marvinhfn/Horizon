package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.StarredMobService;
import de.horizon.feature.dungeon.TeammateGlowService;
import de.horizon.feature.dungeon.boss.SpiritBearService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void horizon$entityGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object) this;
        if (self instanceof ArmorStand) return;

        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) return;
        if (!horizon.getDungeonStateService().isInDungeon()) return;

        HorizonConfig config = horizon.getConfigManager().getConfig();

        // Starred mob glow
        if (!(self instanceof LocalPlayer) && config.isHighlightStarredMobsEnabled()
            && StarredMobService.isStarredMob(self)) {
            cir.setReturnValue(true);
            return;
        }

        // Bat glow
        if (config.isHighlightBatsEnabled() && StarredMobService.isDungeonBat(self)) {
            cir.setReturnValue(true);
            return;
        }

        // Fel glow
        if (config.isHighlightFelsEnabled() && StarredMobService.isFel(self)) {
            cir.setReturnValue(true);
            return;
        }

        // Spirit Bear glow
        if (config.isSpiritBearHighlightEnabled() && SpiritBearService.isSpiritBear(self)) {
            cir.setReturnValue(true);
            return;
        }

        // Teammate glow
        if (self instanceof LocalPlayer) return;
        if (config.isTeammateGlowEnabled()) {
            int color = horizon.getTeammateGlowService().getTeammateGlowColor(self, config);
            if (color != -1) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void horizon$entityGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity)(Object) this;
        if (self instanceof ArmorStand) return;

        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) return;
        if (!horizon.getDungeonStateService().isInDungeon()) return;

        HorizonConfig config = horizon.getConfigManager().getConfig();

        // Starred mob color
        if (!(self instanceof LocalPlayer) && config.isHighlightStarredMobsEnabled()
            && StarredMobService.isStarredMob(self)) {
            cir.setReturnValue(config.getStarredMobColor() & 0x00FFFFFF);
            return;
        }

        // Bat color
        if (config.isHighlightBatsEnabled() && StarredMobService.isDungeonBat(self)) {
            cir.setReturnValue(config.getBatHighlightColor() & 0x00FFFFFF);
            return;
        }

        // Fel color
        if (config.isHighlightFelsEnabled() && StarredMobService.isFel(self)) {
            cir.setReturnValue(config.getFelHighlightColor() & 0x00FFFFFF);
            return;
        }

        // Spirit Bear color
        if (config.isSpiritBearHighlightEnabled() && SpiritBearService.isSpiritBear(self)) {
            cir.setReturnValue(config.getSpiritBearHighlightColor() & 0x00FFFFFF);
            return;
        }

        // Teammate class color
        if (self instanceof LocalPlayer) return;
        if (config.isTeammateGlowEnabled()) {
            int color = horizon.getTeammateGlowService().getTeammateGlowColor(self, config);
            if (color != -1) {
                cir.setReturnValue(color & 0x00FFFFFF);
            }
        }
    }
}
