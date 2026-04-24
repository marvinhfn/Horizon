package de.horizon.hud;

import de.horizon.feature.revive.ReviveSource;
import net.minecraft.client.gui.DrawContext;

public final class ReviveIconRenderer {
    private ReviveIconRenderer() {
    }

    public static void draw(DrawContext context, ReviveSource source, int x, int y) {
        switch (source) {
            case SPIRIT_MASK -> drawSpiritMask(context, x, y);
            case BONZO_MASK -> drawBonzoMask(context, x, y);
            case PHOENIX_PET -> drawPhoenix(context, x, y);
        }
    }

    private static void drawSpiritMask(DrawContext context, int x, int y) {
        context.fill(x + 4, y + 1, x + 12, y + 3, 0xFFE8F2FF);
        context.fill(x + 3, y + 3, x + 13, y + 12, 0xFFF6F8FC);
        context.fill(x + 5, y + 2, x + 7, y + 5, 0xFF2A2E38);
        context.fill(x + 9, y + 2, x + 11, y + 5, 0xFF2A2E38);
        context.fill(x + 4, y + 5, x + 12, y + 10, 0xFFE7EEF8);
        context.fill(x + 6, y + 6, x + 10, y + 8, 0xFF1E232C);
        context.fill(x + 7, y + 8, x + 9, y + 11, 0xFFAA8654);
        context.fill(x + 2, y + 4, x + 4, y + 10, 0xFF0E1016);
        context.fill(x + 12, y + 4, x + 14, y + 10, 0xFF0E1016);
        context.fill(x + 1, y + 6, x + 2, y + 11, 0xFF0E1016);
        context.fill(x + 14, y + 6, x + 15, y + 11, 0xFF0E1016);
        context.drawStrokedRectangle(x + 3, y + 3, 10, 9, 0xFF95A6BC);
    }

    private static void drawBonzoMask(DrawContext context, int x, int y) {
        context.fill(x + 3, y + 2, x + 13, y + 13, 0xFF4BA8FF);
        context.fill(x + 2, y + 3, x + 4, y + 12, 0xFF2E6B9E);
        context.fill(x + 12, y + 3, x + 14, y + 12, 0xFF2E6B9E);
        context.fill(x + 4, y + 1, x + 12, y + 3, 0xFFF5D64D);
        context.fill(x + 4, y + 3, x + 7, y + 5, 0xFFF7F2D7);
        context.fill(x + 9, y + 3, x + 12, y + 5, 0xFFF7F2D7);
        context.fill(x + 5, y + 4, x + 6, y + 5, 0xFF0E1016);
        context.fill(x + 10, y + 4, x + 11, y + 5, 0xFF0E1016);
        context.fill(x + 5, y + 8, x + 11, y + 10, 0xFFE74759);
        context.fill(x + 6, y + 9, x + 10, y + 11, 0xFFF7F2D7);
        context.fill(x + 7, y + 12, x + 9, y + 14, 0xFFE74759);
        context.drawStrokedRectangle(x + 3, y + 2, 10, 11, 0xFF0E1016);
    }

    private static void drawPhoenix(DrawContext context, int x, int y) {
        context.fill(x + 5, y + 3, x + 11, y + 13, 0xFFFFA126);
        context.fill(x + 3, y + 8, x + 5, y + 12, 0xFFE85F1A);
        context.fill(x + 11, y + 8, x + 13, y + 12, 0xFFE85F1A);
        context.fill(x + 6, y + 1, x + 8, y + 5, 0xFFFFD85B);
        context.fill(x + 8, y, x + 10, y + 5, 0xFFE85F1A);
        context.fill(x + 10, y + 2, x + 12, y + 6, 0xFFFFD85B);
        context.fill(x + 7, y + 5, x + 9, y + 7, 0xFF1B0F0A);
        context.fill(x + 9, y + 6, x + 12, y + 8, 0xFFFFEAD6);
        context.fill(x + 10, y + 6, x + 11, y + 7, 0xFF1B0F0A);
        context.fill(x + 4, y + 12, x + 7, y + 14, 0xFFFFD85B);
        context.fill(x + 9, y + 12, x + 12, y + 14, 0xFFFFD85B);
        context.drawStrokedRectangle(x + 4, y + 2, 8, 11, 0xFF8B2F12);
    }
}
