package de.horizon.screen.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Draws a translucent cartoon cherry-blossom branch on the left (mirrored) and right of an overlay.
 * Two layers: the blossoms are tinted with the active theme accent (so they follow the HUD style)
 * and the wood stays a fixed brown. Backed by {@code cherry_branch_petals.png} / {@code cherry_branch_wood.png}.
 */
public final class CherryBlossom {
    private CherryBlossom() {}

    private static final Identifier PETALS =
        Identifier.fromNamespaceAndPath("horizon", "textures/gui/cherry_branch_petals.png");
    private static final Identifier WOOD =
        Identifier.fromNamespaceAndPath("horizon", "textures/gui/cherry_branch_wood.png");
    private static final int TEX_W = 128, TEX_H = 512;
    private static final int BRANCH_W = 90, BRANCH_H = 360; // on-screen size
    private static final int WOOD_TINT = 0xC063402B; // fixed warm brown, ~75% alpha

    /** Draws a branch flush to the left (mirrored) and right of the given region. */
    public static void renderSides(GuiGraphicsExtractor ctx, int left, int top, int right, int height) {
        int accent = de.horizon.theme.ThemeManager.current().accent;
        int petalTint = (0xB4 << 24) | (accent & 0x00FFFFFF); // theme accent, ~70% alpha
        int y = top + Math.max(0, (height - BRANCH_H) / 2);
        drawBranch(ctx, right - BRANCH_W, y, false, petalTint); // right (texture as authored)
        drawBranch(ctx, left, y, true, petalTint);              // left (mirrored)
    }

    private static void drawBranch(GuiGraphicsExtractor ctx, int x, int y, boolean mirror, int petalTint) {
        float sx = (float) BRANCH_W / TEX_W;
        float sy = (float) BRANCH_H / TEX_H;
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        if (mirror) {
            ctx.pose().translate(BRANCH_W, 0);
            ctx.pose().scale(-sx, sy);
        } else {
            ctx.pose().scale(sx, sy);
        }
        ctx.blit(RenderPipelines.GUI_TEXTURED, WOOD, 0, 0, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H, WOOD_TINT);
        ctx.blit(RenderPipelines.GUI_TEXTURED, PETALS, 0, 0, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H, petalTint);
        ctx.pose().popMatrix();
    }
}
