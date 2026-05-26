package de.horizon.youtube;

import de.horizon.HorizonMod;
import de.horizon.hud.HudStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.awt.Robot;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class YoutubeMusicInventoryOverlay {
    // OS media key codes (Windows VK codes passed through Java AWT Robot)
    private static final int VK_MEDIA_PREV       = 0xB1;
    private static final int VK_MEDIA_PLAY_PAUSE = 0xB3;
    private static final int VK_MEDIA_NEXT       = 0xB0;
    private static final int VK_VOLUME_DOWN      = 0xAE;
    private static final int VK_VOLUME_UP        = 0xAF;

    // Layout — identical to SpotifyInventoryOverlay
    private static final int PANEL_WIDTH           = 304;
    private static final int HEADER_HEIGHT         = 38;
    private static final int TRACK_CARD_TOP        = 50;
    private static final int TRACK_CARD_HEIGHT     = 44;
    private static final int CONTROLS_TOP          = 108;
    private static final int VOLUME_LABEL_TOP      = 130;
    private static final int VOLUME_SLIDER_TOP     = 142;
    private static final int PLAYLIST_DROPDOWN_TOP = 156;
    private static final int DROPDOWN_HEADER_HEIGHT = 22;
    private static final int ROW_HEIGHT            = 18;
    private static final int BASE_CONTENT_BOTTOM   = 208;
    private static final int BASE_EXPANDED_HEIGHT  = 224;

    // Colors — identical to SpotifyInventoryOverlay
    private static final int CARD        = 0xFFF0F1F3;
    private static final int CARD_ALT    = 0xFFF7F8FA;
    private static final int BUTTON      = 0xFFE6E8EC;
    private static final int BUTTON_HOVER = 0xFFD9DDE3;
    private static final int TEXT_COLOR  = 0xFF1E2A37;
    private static final int MUTED       = 0xFF667487;

    private final YoutubeService youtubeService;
    private final List<Button> buttons = new ArrayList<>();
    private Rect minimizeButton = new Rect(0, 0, 0, 0);
    private Rect volumeSlider   = new Rect(0, 0, 0, 0);
    private Rect playlistDropdown = new Rect(0, 0, 0, 0);
    private boolean minimized = true;
    private boolean playlistsOpen;
    private boolean draggingVolume;
    private int localVolume = 50; // local display value; system volume is unknown
    private long lastVolumeSendMillis;
    private Robot robot;

    public YoutubeMusicInventoryOverlay(YoutubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    public void render(HandledScreen<?> screen, DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        int visiblePlaylistRows = visiblePlaylistRows(screen.height);
        int panelHeight = panelHeight(visiblePlaylistRows);
        int x = panelX(screen.width);
        int y = panelY(screen.height, panelHeight);

        if (minimized) {
            int btnX = screen.width - 30;
            int btnY = screen.height - 30;
            minimizeButton = new Rect(btnX, btnY, 22, 22);
            context.fill(minimizeButton.x, minimizeButton.y, minimizeButton.right(), minimizeButton.bottom(), CARD);
            context.drawStrokedRectangle(minimizeButton.x, minimizeButton.y, minimizeButton.width, minimizeButton.height, HudStyle.border());
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("+"), minimizeButton.x + 11, minimizeButton.y + 7, TEXT_COLOR);
            buttons.clear();
            volumeSlider = new Rect(0, 0, 0, 0);
            playlistDropdown = new Rect(0, 0, 0, 0);
            return;
        }

        buttons.clear();
        context.fill(x, y, x + PANEL_WIDTH, y + panelHeight, CARD);
        context.drawStrokedRectangle(x, y, PANEL_WIDTH, panelHeight, HudStyle.border());
        context.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, CARD_ALT);
        context.fill(x + 16, y + 16, x + 172, y + 18, HudStyle.accent());
        context.drawTextWithShadow(client.textRenderer, Text.literal("YouTube Music Control"), x + 16, y + 24, TEXT_COLOR);
        minimizeButton = new Rect(x + PANEL_WIDTH - 26, y + 10, 18, 18);
        context.fill(minimizeButton.x, minimizeButton.y, minimizeButton.right(), minimizeButton.bottom(), BUTTON);
        context.drawStrokedRectangle(minimizeButton.x, minimizeButton.y, minimizeButton.width, minimizeButton.height, HudStyle.border());
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("-"), minimizeButton.x + 9, minimizeButton.y + 5, TEXT_COLOR);

        if (!youtubeService.auth().isLoggedIn()) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Nicht verbunden"), x + 16, y + 54, HudStyle.accent());
            drawWrapped(context, youtubeService.auth().getStatusMessage(), x + 16, y + 72, 268, MUTED);
            return;
        }

        // Track card — no API for current track; show service label
        context.fill(x + 16, y + TRACK_CARD_TOP, x + PANEL_WIDTH - 16, y + TRACK_CARD_TOP + TRACK_CARD_HEIGHT, CARD_ALT);
        context.drawStrokedRectangle(x + 16, y + TRACK_CARD_TOP, PANEL_WIDTH - 32, TRACK_CARD_HEIGHT, HudStyle.border());
        context.drawTextWithShadow(client.textRenderer, Text.literal("YouTube Music"), x + 28, y + 60, HudStyle.text());
        context.drawTextWithShadow(client.textRenderer, Text.literal(youtubeService.auth().getStatusMessage()), x + 28, y + 76, MUTED);

        // Playback controls (media keys)
        int btnY = y + CONTROLS_TOP;
        addButton(x + 16,  btnY, 34, 20, "<<",        this::skipPrevious);
        addButton(x + 56,  btnY, 62, 20, "Play/Pause", this::playPause);
        addButton(x + 124, btnY, 34, 20, ">>",        this::skipNext);

        // Volume slider
        int sliderX = x + 16;
        int sliderY = y + VOLUME_SLIDER_TOP;
        volumeSlider = new Rect(sliderX, sliderY, 214, 5);
        context.fill(volumeSlider.x, volumeSlider.y, volumeSlider.right(), volumeSlider.bottom(), 0xFF27313A);
        int knobX = volumeSlider.x + Math.round((volumeSlider.width * localVolume) / 100.0F);
        context.fill(volumeSlider.x, volumeSlider.y, knobX, volumeSlider.bottom(), HudStyle.accent());
        context.fill(knobX - 4, volumeSlider.y - 4, knobX + 4, volumeSlider.y + 9, TEXT_COLOR);
        context.drawTextWithShadow(client.textRenderer, Text.literal("Volume"), x + 16, y + VOLUME_LABEL_TOP, MUTED);
        context.drawTextWithShadow(client.textRenderer, Text.literal(localVolume + "%"), x + 246, y + 136, MUTED);

        // Playlist dropdown (at same position as Spotify's device dropdown)
        int playlistY = y + PLAYLIST_DROPDOWN_TOP;
        playlistDropdown = new Rect(x + 16, playlistY, PANEL_WIDTH - 32, DROPDOWN_HEADER_HEIGHT);
        context.fill(playlistDropdown.x, playlistDropdown.y, playlistDropdown.right(), playlistDropdown.bottom(), BUTTON);
        context.drawStrokedRectangle(playlistDropdown.x, playlistDropdown.y, playlistDropdown.width, playlistDropdown.height, playlistsOpen ? HudStyle.accent() : HudStyle.border());
        context.drawTextWithShadow(client.textRenderer, Text.literal("YouTube Playlisten"), playlistDropdown.x + 10, playlistDropdown.y + 7, TEXT_COLOR);
        context.drawTextWithShadow(client.textRenderer, Text.literal(playlistsOpen ? "^" : "v"), playlistDropdown.right() - 16, playlistDropdown.y + 7, MUTED);

        if (playlistsOpen) {
            youtubeService.requestPlaylistsRefresh(false);
            List<YoutubePlaylist> playlists = youtubeService.getPlaylists();
            if (playlists.isEmpty()) {
                context.drawTextWithShadow(client.textRenderer, Text.literal("Keine Playlisten gefunden"), x + 26, playlistDropdown.bottom() + 12, MUTED);
            }
            for (int i = 0; i < Math.min(visiblePlaylistRows, playlists.size()); i++) {
                YoutubePlaylist playlist = playlists.get(i);
                Rect row = playlistRowRect(x, playlistDropdown.bottom() + 6, i);
                context.fill(row.x, row.y, row.right(), row.bottom(), row.contains(mouseX, mouseY) ? BUTTON_HOVER : BUTTON);
                context.drawTextWithShadow(client.textRenderer, Text.literal(trim(client, playlist.title(), row.width - 20)), row.x + 10, row.y + 6, TEXT_COLOR);
            }
        }

        for (Button button : buttons) {
            context.fill(button.x, button.y, button.x + button.width, button.y + button.height, BUTTON);
            context.drawStrokedRectangle(button.x, button.y, button.width, button.height, button.contains(mouseX, mouseY) ? HudStyle.accent() : HudStyle.border());
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(button.label), button.x + (button.width / 2), button.y + 4, TEXT_COLOR);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        for (Button overlayButton : buttons) {
            if (overlayButton.contains(mouseX, mouseY)) {
                overlayButton.action.run();
                return true;
            }
        }
        if (minimizeButton.contains(mouseX, mouseY)) {
            minimized = !minimized;
            draggingVolume = false;
            playlistsOpen = false;
            return true;
        }
        if (volumeSlider.contains(mouseX, mouseY)) {
            draggingVolume = true;
            updateVolume(mouseX, true);
            return true;
        }
        if (playlistDropdown.contains(mouseX, mouseY)) {
            playlistsOpen = !playlistsOpen;
            if (playlistsOpen) {
                youtubeService.requestPlaylistsRefresh(true);
            }
            return true;
        }
        if (playlistsOpen) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                int visiblePlaylistRows = visiblePlaylistRows(client.getWindow().getScaledHeight());
                int panelHeight = panelHeight(visiblePlaylistRows);
                int x = panelX(client.getWindow().getScaledWidth());
                int y = panelY(client.getWindow().getScaledHeight(), panelHeight);
                int playlistBaseY = y + PLAYLIST_DROPDOWN_TOP + DROPDOWN_HEADER_HEIGHT + 6;
                List<YoutubePlaylist> playlists = youtubeService.getPlaylists();
                for (int i = 0; i < Math.min(visiblePlaylistRows, playlists.size()); i++) {
                    if (playlistRowRect(x, playlistBaseY, i).contains(mouseX, mouseY)) {
                        Util.getOperatingSystem().open(URI.create(playlists.get(i).musicUrl()));
                        playlistsOpen = false;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !draggingVolume) {
            return false;
        }
        updateVolume(mouseX, false);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || !draggingVolume) {
            return false;
        }
        updateVolume(mouseX, true);
        draggingVolume = false;
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_BRACKET) {
            skipNext();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_BRACKET) {
            skipPrevious();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_APOSTROPHE) {
            playPause();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL) {
            changeVolume(10);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS) {
            changeVolume(-10);
            return true;
        }
        return false;
    }

    private void playPause() {
        sendMediaKey(VK_MEDIA_PLAY_PAUSE);
    }

    private void skipNext() {
        sendMediaKey(VK_MEDIA_NEXT);
    }

    private void skipPrevious() {
        sendMediaKey(VK_MEDIA_PREV);
    }

    private void changeVolume(int delta) {
        int target = Math.max(0, Math.min(100, localVolume + delta));
        int steps = Math.max(1, Math.abs(target - localVolume) / 2);
        int key = target > localVolume ? VK_VOLUME_UP : VK_VOLUME_DOWN;
        for (int i = 0; i < Math.min(steps, 10); i++) {
            sendMediaKey(key);
        }
        localVolume = target;
    }

    private void updateVolume(double mouseX, boolean force) {
        int newVolume = (int) Math.round(((mouseX - volumeSlider.x) / Math.max(1, volumeSlider.width)) * 100.0D);
        newVolume = Math.max(0, Math.min(100, newVolume));
        long now = System.currentTimeMillis();
        if (force || now - lastVolumeSendMillis > 250L) {
            lastVolumeSendMillis = now;
            int delta = newVolume - localVolume;
            if (Math.abs(delta) >= 1) {
                int steps = Math.max(1, Math.min(10, Math.abs(delta) / 2));
                int key = delta > 0 ? VK_VOLUME_UP : VK_VOLUME_DOWN;
                for (int i = 0; i < steps; i++) {
                    sendMediaKey(key);
                }
            }
        }
        localVolume = newVolume;
    }

    private void sendMediaKey(int keyCode) {
        try {
            if (robot == null) {
                robot = new Robot();
            }
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } catch (Exception exception) {
            HorizonMod.LOGGER.debug("Media key failed", exception);
        }
    }

    private void addButton(int x, int y, int width, int height, String label, Runnable action) {
        buttons.add(new Button(x, y, width, height, label, action));
    }

    private Rect playlistRowRect(int panelX, int baseY, int index) {
        return new Rect(panelX + 16, baseY + (index * ROW_HEIGHT), PANEL_WIDTH - 32, 17);
    }

    private int visiblePlaylistRows(int screenHeight) {
        int y = panelY(screenHeight, panelHeight(6));
        int available = Math.max(0, screenHeight - 16 - (y + PLAYLIST_DROPDOWN_TOP + DROPDOWN_HEADER_HEIGHT + 6));
        return Math.max(0, Math.min(6, available / ROW_HEIGHT));
    }

    private int panelY(int screenHeight, int panelHeight) {
        return Math.max(16, Math.min(screenHeight - panelHeight - 16, screenHeight - panelHeight));
    }

    private int panelX(int screenWidth) {
        return Math.max(16, screenWidth - PANEL_WIDTH - 16);
    }

    private int panelHeight(int visiblePlaylistRows) {
        int playlistRowsHeight = playlistsOpen ? visiblePlaylistRows * ROW_HEIGHT + 8 : 0;
        return Math.max(BASE_EXPANDED_HEIGHT, BASE_CONTENT_BOTTOM + playlistRowsHeight + 10);
    }

    private String trim(MinecraftClient client, String text, int maxWidth) {
        if (client.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (!result.isEmpty() && client.textRenderer.getWidth(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private void drawWrapped(DrawContext context, String text, int x, int y, int maxWidth, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        int lineY = y;
        for (String line : wrap(client, text, maxWidth)) {
            context.drawTextWithShadow(client.textRenderer, Text.literal(line), x, lineY, color);
            lineY += 12;
        }
    }

    private List<String> wrap(MinecraftClient client, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (client.textRenderer.getWidth(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private record Button(int x, int y, int width, int height, String label, Runnable action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record Rect(int x, int y, int width, int height) {
        int right()  { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mx, double my) {
            return mx >= x && mx <= right() && my >= y && my <= bottom();
        }
    }
}
