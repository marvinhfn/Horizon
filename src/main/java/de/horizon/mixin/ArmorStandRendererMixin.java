package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.feature.dungeon.StarredMobService;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandRenderer.class)
public class ArmorStandRendererMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/decoration/ArmorStand;D)Z", at = @At("RETURN"), cancellable = true)
    private void horizon$hideNonStarredNametag(ArmorStand entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null) return;
        if (!horizon.getDungeonStateService().isInDungeon()) return;
        if (!horizon.getConfigManager().getConfig().isHideNonStarredMobsEnabled()) return;

        // Only hide dungeon mob nametags (those with ❤) that are NOT starred
        if (StarredMobService.isDungeonMobNameTag(entity) && !StarredMobService.isStarredNameTag(entity)) {
            cir.setReturnValue(false);
        }
    }
}
