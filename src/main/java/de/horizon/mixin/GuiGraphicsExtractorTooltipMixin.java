package de.horizon.mixin;

import de.horizon.HorizonClient;
import de.horizon.feature.misc.TooltipState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Applies the scrollable/resizable-tooltip transform: a pose scale (Ctrl+scroll) and vertical scroll
 * offset around the cursor, wrapped around the whole tooltip render.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorTooltipMixin {
    @Shadow public abstract org.joml.Matrix3x2fStack pose();

    private boolean horizon$scaled = false;

    @Inject(method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V",
            at = @At("HEAD"))
    private void horizon$tooltipPre(Font font, List<ClientTooltipComponent> components, int x, int y,
            ClientTooltipPositioner positioner, Identifier bg, CallbackInfo ci) {
        HorizonClient h = HorizonClient.getInstance();
        horizon$scaled = false;
        if (h == null || !h.getConfigManager().getConfig().isScrollableTooltips()) return;
        TooltipState.markShown();
        float s = TooltipState.scale;
        int off = TooltipState.scrollOffset;
        if (s == 1.0f && off == 0) return;
        horizon$scaled = true;
        pose().pushMatrix();
        // Scale around the cursor and shift vertically for scrolling.
        pose().translate((float) x, (float) y);
        pose().scale(s, s);
        pose().translate((float) -x, (float) -y - off);
    }

    @Inject(method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V",
            at = @At("RETURN"))
    private void horizon$tooltipPost(Font font, List<ClientTooltipComponent> components, int x, int y,
            ClientTooltipPositioner positioner, Identifier bg, CallbackInfo ci) {
        if (horizon$scaled) {
            pose().popMatrix();
            horizon$scaled = false;
        }
    }
}
