package de.horizon.hud;

import de.horizon.config.ConfigManager;
import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.revive.ReviveSource;
import de.horizon.feature.revive.ReviveTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public final class RevivalStatusHudElement implements HudElement {
    public static final String ID = "revive_status";
    private static final String READY_ICON = "\u2713";

    private static final String READY_SYMBOL = "✓";
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_GAP = 6;
    private static final int BADGE_HEIGHT = 14;
    private static final int COOLDOWN_TEXT = 0xFFFCE7C5;
    private static final int COOLDOWN_BADGE = 0xCC9C5E2D;

    private final ConfigManager configManager;
    private final ReviveTracker tracker;
    private final DungeonStateService dungeonStateService;

    public RevivalStatusHudElement(ConfigManager configManager, ReviveTracker tracker, DungeonStateService dungeonStateService) {
        this.configManager = configManager;
        this.tracker = tracker;
        this.dungeonStateService = dungeonStateService;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        if (!config.isReviveHudEnabled() || activeSources(config).isEmpty()) {
            return false;
        }
        if (config.isReviveHudAlwaysVisible()) {
            return true;
        }
        if (!dungeonStateService.isInDungeon()) {
            return false;
        }
        return !config.isReviveHudOnlyInBoss() || dungeonStateService.isInBoss();
    }

    @Override
    public boolean isMovable() {
        return true;
    }

    @Override
    public int defaultX() {
        return 20;
    }

    @Override
    public int defaultY() {
        return 20;
    }

    @Override
    public int width(MinecraftClient client, HudPosition position) {
        return Math.max(1, (int) Math.ceil(baseWidth(client, configManager.getConfig()) * position.getScale()));
    }

    @Override
    public int height(MinecraftClient client, HudPosition position) {
        return Math.max(1, (int) Math.ceil(baseHeight(configManager.getConfig()) * position.getScale()));
    }

    @Override
    public void render(DrawContext drawContext, MinecraftClient client, HudPosition position, boolean editorMode) {
        HorizonConfig config = configManager.getConfig();
        List<ReviveSource> sources = activeSources(config);
        if (sources.isEmpty()) {
            return;
        }

        int width = baseWidth(client, config);
        float scale = (float) position.getScale();
        Matrix3x2fStack matrices = drawContext.getMatrices();
        TextRenderer renderer = client.textRenderer;

        matrices.pushMatrix();
        matrices.translate(position.getX(), position.getY());
        matrices.scale(scale, scale);

        int lineY = 0;
        for (ReviveSource source : sources) {
            ReviveIconRenderer.draw(drawContext, source, 0, lineY);

            String valueText;
            int textColor;
            int badgeColor;
            if (!editorMode && tracker.isReady(source)) {
                valueText = READY_ICON;
                textColor = HudStyle.readyText();
                badgeColor = HudStyle.badgeFill();
            } else if (editorMode && source == ReviveSource.BONZO_MASK) {
                valueText = "2:34";
                textColor = COOLDOWN_TEXT;
                badgeColor = COOLDOWN_BADGE;
            } else if (editorMode) {
                valueText = READY_ICON;
                textColor = HudStyle.readyText();
                badgeColor = HudStyle.badgeFill();
            } else {
                valueText = tracker.getRemainingText(source);
                textColor = COOLDOWN_TEXT;
                badgeColor = COOLDOWN_BADGE;
            }

            boolean ready = (!editorMode && tracker.isReady(source)) || (editorMode && !valueText.contains(":"));
            int badgeWidth = ready ? 24 : Math.max(40, renderer.getWidth(valueText) + 14);
            int badgeX = 24;
            int badgeY = lineY + ((ROW_HEIGHT - BADGE_HEIGHT) / 2);
            if (!ready) {
                drawContext.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + BADGE_HEIGHT, badgeColor);
            }
            if (ready) {
                drawContext.drawTextWithShadow(renderer, Text.literal(valueText), badgeX + 3, badgeY - 1, textColor);
                drawContext.drawTextWithShadow(renderer, Text.literal(valueText), badgeX + 4, badgeY - 1, textColor);
                drawContext.drawTextWithShadow(renderer, Text.literal(valueText), badgeX + 3, badgeY, textColor);
                drawContext.drawTextWithShadow(renderer, Text.literal(valueText), badgeX + 4, badgeY, textColor);
            } else {
                drawContext.drawCenteredTextWithShadow(renderer, valueText, badgeX + (badgeWidth / 2), badgeY + 2, textColor);
            }
            lineY += ROW_HEIGHT + ROW_GAP;
        }

        matrices.popMatrix();
    }

    private int baseWidth(MinecraftClient client, HorizonConfig config) {
        TextRenderer renderer = client.textRenderer;
        int max = 0;
        for (ReviveSource source : activeSources(config)) {
            int badgeWidth = Math.max(40, renderer.getWidth("00:00") + 14);
            max = Math.max(max, 16 + 8 + badgeWidth);
        }
        return max;
    }

    private int baseHeight(HorizonConfig config) {
        int count = activeSources(config).size();
        return (count * ROW_HEIGHT) + (Math.max(0, count - 1) * ROW_GAP);
    }

    private List<ReviveSource> activeSources(HorizonConfig config) {
        List<ReviveSource> sources = new ArrayList<>();
        for (ReviveSource source : ReviveSource.values()) {
            if (source.isEnabled(config)) {
                sources.add(source);
            }
        }
        return sources;
    }
}
