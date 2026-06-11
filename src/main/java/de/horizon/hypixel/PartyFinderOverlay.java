package de.horizon.hypixel;

import de.horizon.HorizonClient;
import de.horizon.hud.HudStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public final class PartyFinderOverlay {
    private static final String[] FLOOR_KEYS = {"0", "1", "2", "3", "4", "5", "6", "7"};
    private static final String[] FLOOR_LABELS = {"E", "F1", "F2", "F3", "F4", "F5", "F6", "F7"};

    private final HypixelProfileService profileService;
    private volatile HypixelDungeonStats cachedStats;
    private volatile long lastRefreshAt;
    private volatile boolean loading;
    private volatile String error = "";

    public PartyFinderOverlay(HypixelProfileService profileService) {
        this.profileService = profileService;
    }

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor context) {
        HorizonClient horizon = HorizonClient.getInstance();
        if (horizon == null || !horizon.getConfigManager().getConfig().isDungeonPartyFinderOverlayEnabled()) {
            return;
        }
        if (!isPartyFinder(screen)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        requestRefresh(client.player.getName().getString());

        int x = 12;
        int y = 18;
        int width = 148;
        int height = 186;
        context.fill(x, y, x + width, y + height, HudStyle.panel());
        context.outline(x, y, width, height, HudStyle.border());
        context.text(client.font, Component.literal("Party Finder"), x + 12, y + 12, HudStyle.accent());
        context.text(client.font, Component.literal("Best S+ Zeiten"), x + 12, y + 26, HudStyle.muted());

        if (loading && cachedStats == null) {
            context.text(client.font, Component.literal("Lade..."), x + 12, y + 48, HudStyle.text());
            return;
        }

        if (!error.isBlank() && cachedStats == null) {
            drawLines(context, x + 12, y + 48, error, 124, 0xFFFF9696);
            return;
        }

        if (cachedStats == null) {
            return;
        }

        context.text(client.font, Component.literal("Profil: " + cachedStats.selectedProfile()), x + 12, y + 46, HudStyle.text());
        int lineY = y + 64;
        for (int index = 0; index < FLOOR_KEYS.length; index++) {
            String label = FLOOR_LABELS[index];
            String time = formatTime(cachedStats.fastestSPlus(FLOOR_KEYS[index]));
            context.text(client.font, Component.literal(label), x + 12, lineY, HudStyle.text());
            context.text(client.font, Component.literal(time), x + 54, lineY, HudStyle.accent());
            lineY += 14;
        }
    }

    private void requestRefresh(String username) {
        long now = System.currentTimeMillis();
        if (loading || (cachedStats != null && now - lastRefreshAt < 60000L)) {
            return;
        }

        loading = true;
        CompletableFuture.runAsync(() -> {
            try {
                cachedStats = profileService.load(username);
                error = "";
                lastRefreshAt = System.currentTimeMillis();
            } catch (Exception exception) {
                error = exception.getMessage() == null ? "Hypixel Daten nicht verfuegbar" : exception.getMessage();
            } finally {
                loading = false;
            }
        });
    }

    private boolean isPartyFinder(AbstractContainerScreen<?> screen) {
        String title = screen.getTitle().getString().toLowerCase();
        return title.contains("party finder") || title.contains("group finder");
    }

    private void drawMessage(GuiGraphicsExtractor context, AbstractContainerScreen<?> screen, String message) {
        int x = 12;
        int y = 18;
        context.fill(x, y, x + 148, y + 64, HudStyle.panel());
        context.outline(x, y, 148, 64, HudStyle.border());
        context.text(Minecraft.getInstance().font, Component.literal("Party Finder"), x + 12, y + 12, HudStyle.accent());
        drawLines(context, x + 12, y + 32, message, 124, 0xFFFF9696);
    }

    private void drawLines(GuiGraphicsExtractor context, int x, int y, String text, int maxWidth, int color) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        int lineY = y;
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (client.font.width(candidate) > maxWidth && !current.isEmpty()) {
                context.text(client.font, Component.literal(current.toString()), x, lineY, color);
                current = new StringBuilder(word);
                lineY += 12;
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            context.text(client.font, Component.literal(current.toString()), x, lineY, color);
        }
    }

    private String formatTime(double seconds) {
        if (seconds < 0.0D) {
            return "--:--";
        }

        int total = (int) Math.round(seconds);
        int minutes = total / 60;
        int secs = total % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}
