package de.horizon.screen.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Draws a translucent cartoon cherry-blossom branch on the left (mirrored) and right of an overlay.
 * Two layers: the blossoms are tinted with the active theme accent (so they follow the HUD style)
 * and the wood stays a fixed brown. Backed by {@code cherry_branch_{petals,wood}{,_l}.png} — the
 * {@code _l} variants are pre-mirrored so the left side draws with a positive scale (a negative
 * scale would flip the triangle winding and get back-face culled, dropping the branch entirely).
 */
public final class CherryBlossom {
    private CherryBlossom() {}

    private static Identifier tex(String n) {
        return Identifier.fromNamespaceAndPath("horizon", "textures/gui/" + n + ".png");
    }
    private static final Identifier PETALS = tex("cherry_branch_petals");
    private static final Identifier WOOD = tex("cherry_branch_wood");
    private static final Identifier PETALS_L = tex("cherry_branch_petals_l");
    private static final Identifier WOOD_L = tex("cherry_branch_wood_l");

    private static final int TEX_W = 256, TEX_H = 1024;
    private static final int BRANCH_W = 96, BRANCH_H = 384; // on-screen size (keeps the 1:4 aspect)
    private static final int WOOD_TINT = 0xD8FFFFFF; // keep the baked bark colour, ~85% alpha

    /** Draws a branch flush to the left (mirrored) and right of the given region. */
    public static void renderSides(GuiGraphicsExtractor ctx, int left, int top, int right, int height) {
        int accent = de.horizon.theme.ThemeManager.current().accent;
        int petalTint = (0xC8 << 24) | (accent & 0x00FFFFFF); // theme accent, ~78% alpha
        int y = top + Math.max(0, (height - BRANCH_H) / 2);
        drawBranch(ctx, right - BRANCH_W, y, WOOD, PETALS, petalTint);   // right (texture as authored)
        drawBranch(ctx, left, y, WOOD_L, PETALS_L, petalTint);           // left (pre-mirrored)
    }

    private static void drawBranch(GuiGraphicsExtractor ctx, int x, int y, Identifier wood, Identifier petals, int petalTint) {
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale((float) BRANCH_W / TEX_W, (float) BRANCH_H / TEX_H);
        ctx.blit(RenderPipelines.GUI_TEXTURED, wood, 0, 0, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H, WOOD_TINT);
        ctx.blit(RenderPipelines.GUI_TEXTURED, petals, 0, 0, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H, petalTint);
        ctx.pose().popMatrix();
    }
}
