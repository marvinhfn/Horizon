package de.horizon.hypixel;

import de.horizon.HorizonClient;
import de.horizon.screen.render.Fonts;
import de.horizon.theme.Theme;
import de.horizon.theme.ThemeManager;
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
        Theme theme = ThemeManager.current();
        de.horizon.screen.render.Ui.roundedRect(context, x, y, width, height, 6, theme.surface);
        de.horizon.screen.render.Ui.outline(context, x, y, width, height, 6, 1, theme.borderSubtle);
        context.text(client.font, Fonts.of("Party Finder"), x + 12, y + 12, theme.accent, false);
        context.text(client.font, Fonts.of("Best S+ Zeiten"), x + 12, y + 26, theme.textMuted, false);

        if (loading && cachedStats == null) {
            context.text(client.font, Fonts.of("Lade..."), x + 12, y + 48, theme.text, false);
            return;
        }

        if (!error.isBlank() && cachedStats == null) {
            drawLines(context, x + 12, y + 48, error, 124, 0xFFFF9696);
            return;
        }

        if (cachedStats == null) {
            return;
        }

        context.text(client.font, Fonts.of("Profil: " + cachedStats.selectedProfile()), x + 12, y + 46, theme.text, false);
        int lineY = y + 64;
        for (int index = 0; index < FLOOR_KEYS.length; index++) {
            String label = FLOOR_LABELS[index];
            String time = formatTime(cachedStats.fastestSPlus(FLOOR_KEYS[index]));
            context.text(client.font, Fonts.of(label), x + 12, lineY, theme.text, false);
            context.text(client.font, Fonts.of(time), x + 54, lineY, theme.accent, false);
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
        Theme theme = ThemeManager.current();
        de.horizon.screen.render.Ui.roundedRect(context, x, y, 148, 64, 6, theme.surface);
        de.horizon.screen.render.Ui.outline(context, x, y, 148, 64, 6, 1, theme.borderSubtle);
        context.text(Minecraft.getInstance().font, Fonts.of("Party Finder"), x + 12, y + 12, theme.accent, false);
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
                context.text(client.font, Fonts.of(current.toString()), x, lineY, color, false);
                current = new StringBuilder(word);
                lineY += 12;
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            context.text(client.font, Fonts.of(current.toString()), x, lineY, color, false);
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
