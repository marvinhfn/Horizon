package de.horizon.spotify;

import de.horizon.config.HorizonConfig;
import de.horizon.screen.render.Fonts;
import de.horizon.theme.Theme;
import de.horizon.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class SpotifyInventoryOverlay {
    private static final int PANEL_WIDTH = 304;
    private static final int HEADER_HEIGHT = 38;
    private static final int TRACK_CARD_TOP = 50;
    private static final int TRACK_CARD_HEIGHT = 44;
    private static final int CONTROLS_TOP = 108;
    private static final int VOLUME_LABEL_TOP = 130;
    private static final int VOLUME_SLIDER_TOP = 142;
    private static final int DEVICE_DROPDOWN_TOP = 156;
    private static final int DROPDOWN_HEADER_HEIGHT = 22;
    private static final int ROW_HEIGHT = 18;
    private static final int BASE_CONTENT_BOTTOM = 208;
    private static final int BASE_EXPANDED_HEIGHT = 224;

    private final SpotifyService spotifyService;
    private final AlbumArtCache albumArt;
    private final List<Button> buttons = new ArrayList<>();
    private Rect minimizeButton = new Rect(0, 0, 0, 0);
    private Rect volumeSlider = new Rect(0, 0, 0, 0);
    private Rect deviceDropdown = new Rect(0, 0, 0, 0);
    private Rect playlistDropdown = new Rect(0, 0, 0, 0);
    private boolean minimized = true;
    private boolean devicesOpen;
    private boolean playlistsOpen;
    private boolean draggingVolume;
    private int localVolume = -1;
    private long lastVolumeSendMillis;

    public SpotifyInventoryOverlay(SpotifyService spotifyService, AlbumArtCache albumArt) {
        this.spotifyService = spotifyService;
        this.albumArt = albumArt;
    }

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        HorizonConfig config = de.horizon.HorizonClient.getInstance().getConfigManager().getConfig();
        if (!config.isSpotifyInventoryControlsEnabled()) {
            return;
        }

        spotifyService.requestStateRefresh(false);
        Theme theme = ThemeManager.current();
        SpotifyPlaybackState state = spotifyService.getPlaybackState();
        int panelWidth = PANEL_WIDTH;
        int visibleDeviceRows = visibleDeviceRows(screen.height);
        int playlistHeaderY = playlistDropdownY(visibleDeviceRows);
        int visiblePlaylistRows = visiblePlaylistRows(screen.height, playlistHeaderY);
        int expandedHeight = panelHeight(visibleDeviceRows, visiblePlaylistRows);
        int panelHeight = expandedHeight;
        int x = panelX(screen.width, panelWidth);
        int y = panelY(screen.height, panelHeight);

        if (minimized) {
            int btnX = screen.width - 30;
            int btnY = screen.height - 30;
            minimizeButton = new Rect(btnX, btnY, 22, 22);
            context.fill(minimizeButton.x, minimizeButton.y, minimizeButton.right(), minimizeButton.bottom(), theme.surface);
            context.outline(minimizeButton.x, minimizeButton.y, minimizeButton.width, minimizeButton.height, theme.borderSubtle);
            context.centeredText(client.font, Fonts.of("+"), minimizeButton.x + 11, minimizeButton.y + 7, theme.text);
            buttons.clear();
            volumeSlider = new Rect(0, 0, 0, 0);
            deviceDropdown = new Rect(0, 0, 0, 0);
            playlistDropdown = new Rect(0, 0, 0, 0);
            return;
        }

        if (!draggingVolume && localVolume < 0 && state.supportsVolume()) {
            localVolume = state.volumePercent();
        }
        buttons.clear();
        // frosted glass panel (fade to every edge + complete accent rim) — same look as the other overlays
        de.horizon.screen.render.Ui.glassPanel(context, x, y, panelWidth, panelHeight, 10, theme);
        // subtle header separator (rounded-clipped so it never pokes the rounded top corners)
        de.horizon.screen.render.Ui.roundedVGradient(context, x + 1, y + 1, panelWidth - 2, HEADER_HEIGHT - 1, 9,
            (0x33 << 24) | (theme.accent & 0xFFFFFF), (0x00 << 24) | (theme.accent & 0xFFFFFF));
        context.fill(x + 16, y + 16, x + 108, y + 18, theme.accent);
        context.text(client.font, Fonts.of("Spotify Control"), x + 16, y + 24, theme.text, false);
        minimizeButton = new Rect(x + panelWidth - 26, y + 10, 18, 18);
        context.fill(minimizeButton.x, minimizeButton.y, minimizeButton.right(), minimizeButton.bottom(), theme.surfaceAlt);
        context.outline(minimizeButton.x, minimizeButton.y, minimizeButton.width, minimizeButton.height, theme.borderSubtle);
        context.centeredText(client.font, Fonts.of("-"), minimizeButton.x + 9, minimizeButton.y + 5, theme.text);

        if (!state.connected()) {
            context.text(client.font, Fonts.of("Nicht verbunden"), x + 16, y + 54, theme.accent, false);
            drawWrapped(context, state.statusMessage(), x + 16, y + 72, 268, theme.textMuted);
            return;
        }

        context.fill(x + 16, y + TRACK_CARD_TOP, x + panelWidth - 16, y + TRACK_CARD_TOP + TRACK_CARD_HEIGHT, theme.surfaceAlt);
        context.outline(x + 16, y + TRACK_CARD_TOP, panelWidth - 32, TRACK_CARD_HEIGHT, theme.borderSubtle);

        int coverSize = 36;
        int coverX = x + 20;
        int coverY = y + TRACK_CARD_TOP + 4;
        albumArt.ensure(state.albumArtUrl());
        drawCover(context, state.albumArtUrl(), coverX, coverY, coverSize);
        int textX = coverX + coverSize + 8;
        int textWidth = (x + panelWidth - 16) - textX - 6;
        drawWrapped(context, state.trackName().isBlank() ? "Kein Track" : state.trackName(), textX, y + 60, textWidth, theme.text);
        drawWrapped(context, state.artistName().isBlank() ? state.statusMessage() : state.artistName(), textX, y + 76, textWidth, theme.textMuted);

        int buttonY = y + CONTROLS_TOP;
        addButton(x + 16, buttonY, 34, 20, "<<", spotifyService::skipPrevious);
        addButton(x + 56, buttonY, 62, 20, state.playing() ? "Pause" : "Play", spotifyService::playPause);
        addButton(x + 124, buttonY, 34, 20, ">>", spotifyService::skipNext);
        context.text(client.font, Fonts.of(state.deviceName().isBlank() ? "Kein Geraet" : trim(client, state.deviceName(), 118)), x + 174, buttonY + 6, theme.textMuted, false);

        int sliderX = x + 16;
        int sliderY = y + VOLUME_SLIDER_TOP;
        volumeSlider = new Rect(sliderX, sliderY, 214, 5);
        context.fill(volumeSlider.x, volumeSlider.y, volumeSlider.right(), volumeSlider.bottom(), theme.surfaceAlt);
        int displayVolume = state.supportsVolume() ? displayVolume(state) : 0;
        int knobX = volumeSlider.x + Math.round((volumeSlider.width * displayVolume) / 100.0F);
        context.fill(volumeSlider.x, volumeSlider.y, knobX, volumeSlider.bottom(), theme.accent);
        context.fill(knobX - 4, volumeSlider.y - 4, knobX + 4, volumeSlider.y + 9, state.supportsVolume() ? theme.text : theme.textMuted);
        context.text(client.font, Fonts.of("Volume"), x + 16, y + VOLUME_LABEL_TOP, theme.textMuted, false);
        context.text(client.font, Fonts.of(state.supportsVolume() ? displayVolume + "%" : "n/a"), x + 246, y + 136, theme.textMuted, false);

        deviceDropdown = new Rect(x + 16, y + DEVICE_DROPDOWN_TOP, panelWidth - 32, DROPDOWN_HEADER_HEIGHT);
        context.fill(deviceDropdown.x, deviceDropdown.y, deviceDropdown.right(), deviceDropdown.bottom(), theme.surfaceAlt);
        context.outline(deviceDropdown.x, deviceDropdown.y, deviceDropdown.width, deviceDropdown.height, devicesOpen ? theme.accent : theme.borderSubtle);
        context.text(client.font, Fonts.of(trim(client, "Geraet: " + (state.deviceName().isBlank() ? "keins" : state.deviceName()), deviceDropdown.width - 24)), deviceDropdown.x + 10, deviceDropdown.y + 7, theme.text, false);
        context.text(client.font, Fonts.of(devicesOpen ? "^" : "v"), deviceDropdown.right() - 16, deviceDropdown.y + 7, theme.textMuted, false);
        if (devicesOpen) {
            spotifyService.requestDevicesRefresh(false);
            List<SpotifyDevice> devices = spotifyService.getDevices();
            if (devices.isEmpty()) {
                context.text(client.font, Fonts.of("Keine Geraete gefunden"), x + 26, y + 190, theme.textMuted, false);
            }
            for (int index = 0; index < Math.min(visibleDeviceRows, devices.size()); index++) {
                SpotifyDevice device = devices.get(index);
                Rect row = deviceRowRect(x, y, panelWidth, index);
                context.fill(row.x, row.y, row.right(), row.bottom(), row.contains(mouseX, mouseY) ? theme.surfaceHover : theme.surfaceAlt);
                String prefix = device.active() ? "* " : "";
                context.text(client.font, Fonts.of(trim(client, prefix + device.name(), row.width - 20)), row.x + 10, row.y + 6, device.restricted() ? theme.textMuted : theme.text, false);
            }
        }

        int playlistY = y + playlistHeaderY;
        playlistDropdown = new Rect(x + 16, playlistY, panelWidth - 32, DROPDOWN_HEADER_HEIGHT);
        context.fill(playlistDropdown.x, playlistDropdown.y, playlistDropdown.right(), playlistDropdown.bottom(), theme.surfaceAlt);
        context.outline(playlistDropdown.x, playlistDropdown.y, playlistDropdown.width, playlistDropdown.height, playlistsOpen ? theme.accent : theme.borderSubtle);
        context.text(client.font, Fonts.of("Spotify Playlisten"), playlistDropdown.x + 10, playlistDropdown.y + 7, theme.text, false);
        context.text(client.font, Fonts.of(playlistsOpen ? "^" : "v"), playlistDropdown.right() - 16, playlistDropdown.y + 7, theme.textMuted, false);

        if (playlistsOpen) {
            spotifyService.requestRecentPlaylistsRefresh(false);
            List<SpotifyPlaylist> playlists = spotifyService.getRecentPlaylists();
            if (playlists.isEmpty()) {
                context.text(client.font, Fonts.of("Keine Playlisten gefunden"), x + 26, playlistDropdown.bottom() + 12, theme.textMuted, false);
            }
            for (int index = 0; index < Math.min(visiblePlaylistRows, playlists.size()); index++) {
                SpotifyPlaylist playlist = playlists.get(index);
                Rect row = playlistRowRect(x, playlistDropdown.bottom() + 6, panelWidth, index);
                context.fill(row.x, row.y, row.right(), row.bottom(), row.contains(mouseX, mouseY) ? theme.surfaceHover : theme.surfaceAlt);
                context.text(client.font, Fonts.of(trim(client, playlist.name(), row.width - 20)), row.x + 10, row.y + 6, theme.text, false);
            }
        }

        for (Button button : buttons) {
            context.fill(button.x, button.y, button.x + button.width, button.y + button.height, theme.surfaceAlt);
            context.outline(button.x, button.y, button.width, button.height, button.contains(mouseX, mouseY) ? theme.accent : theme.borderSubtle);
            context.centeredText(client.font, Fonts.of(button.label), button.x + (button.width / 2), button.y + 4, theme.text);
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
            devicesOpen = false;
            playlistsOpen = false;
            return true;
        }
        if (volumeSlider.contains(mouseX, mouseY)) {
            draggingVolume = true;
            updateVolume(mouseX, true);
            return true;
        }
        if (deviceDropdown.contains(mouseX, mouseY)) {
            devicesOpen = !devicesOpen;
            playlistsOpen = false;
            if (devicesOpen) {
                spotifyService.requestDevicesRefresh(true);
            }
            return true;
        }
        if (playlistDropdown.contains(mouseX, mouseY)) {
            playlistsOpen = !playlistsOpen;
            devicesOpen = false;
            if (playlistsOpen) {
                spotifyService.requestRecentPlaylistsRefresh(true);
            }
            return true;
        }
        if (devicesOpen) {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                int panelWidth = PANEL_WIDTH;
                int visibleDeviceRows = visibleDeviceRows(client.getWindow().getGuiScaledHeight());
                int playlistHeaderY = playlistDropdownY(visibleDeviceRows);
                int visiblePlaylistRows = visiblePlaylistRows(client.getWindow().getGuiScaledHeight(), playlistHeaderY);
                int expandedHeight = panelHeight(visibleDeviceRows, visiblePlaylistRows);
                int x = panelX(client.getWindow().getGuiScaledWidth(), panelWidth);
                int y = panelY(client.getWindow().getGuiScaledHeight(), expandedHeight);
                List<SpotifyDevice> devices = spotifyService.getDevices();
                for (int index = 0; index < Math.min(visibleDeviceRows, devices.size()); index++) {
                    if (deviceRowRect(x, y, panelWidth, index).contains(mouseX, mouseY)) {
                        if (!devices.get(index).restricted()) {
                            spotifyService.selectDevice(devices.get(index));
                        }
                        devicesOpen = false;
                        return true;
                    }
                }
            }
        }
        if (playlistsOpen) {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                int panelWidth = PANEL_WIDTH;
                int visibleDeviceRows = visibleDeviceRows(client.getWindow().getGuiScaledHeight());
                int playlistHeaderY = playlistDropdownY(visibleDeviceRows);
                int visiblePlaylistRows = visiblePlaylistRows(client.getWindow().getGuiScaledHeight(), playlistHeaderY);
                int expandedHeight = panelHeight(visibleDeviceRows, visiblePlaylistRows);
                int x = panelX(client.getWindow().getGuiScaledWidth(), panelWidth);
                int y = panelY(client.getWindow().getGuiScaledHeight(), expandedHeight);
                int playlistBaseY = y + playlistHeaderY + DROPDOWN_HEADER_HEIGHT + 6;
                List<SpotifyPlaylist> playlists = spotifyService.getRecentPlaylists();
                for (int index = 0; index < Math.min(visiblePlaylistRows, playlists.size()); index++) {
                    if (playlistRowRect(x, playlistBaseY, panelWidth, index).contains(mouseX, mouseY)) {
                        spotifyService.playPlaylist(playlists.get(index));
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
            spotifyService.skipNext();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_BRACKET) {
            spotifyService.skipPrevious();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_APOSTROPHE) {
            spotifyService.playPause();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL) {
            spotifyService.changeVolume(10);
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS) {
            spotifyService.changeVolume(-10);
            return true;
        }
        return false;
    }

    /**
     * Draws the album cover at ({@code px},{@code py}) scaled into a {@code size}×{@code size} box, or a
     * neutral placeholder with a ♪ glyph while the art is still loading.
     */
    private void drawCover(GuiGraphicsExtractor context, String url, int px, int py, int size) {
        if (albumArt.isReady(url)) {
            int texW = albumArt.width();
            int texH = albumArt.height();
            context.pose().pushMatrix();
            context.pose().translate(px, py);
            context.pose().scale((float) size / texW, (float) size / texH);
            context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, albumArt.textureId(),
                0, 0, 0f, 0f, texW, texH, texW, texH);
            context.pose().popMatrix();
        } else {
            Theme theme = ThemeManager.current();
            context.fill(px, py, px + size, py + size, theme.surfaceAlt);
            context.outline(px, py, size, size, theme.borderSubtle);
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                context.centeredText(client.font, Fonts.of("♪"), px + size / 2, py + size / 2 - 4, theme.textMuted);
            }
        }
    }

    private void addButton(int x, int y, int width, int height, String label, Runnable action) {
        buttons.add(new Button(x, y, width, height, label, action));
    }

    private int displayVolume(SpotifyPlaybackState state) {
        if (localVolume >= 0) {
            return localVolume;
        }
        return Math.max(0, Math.min(100, state.volumePercent()));
    }

    private void updateVolume(double mouseX, boolean force) {
        int volume = (int) Math.round(((mouseX - volumeSlider.x) / Math.max(1, volumeSlider.width)) * 100.0D);
        localVolume = Math.max(0, Math.min(100, volume));
        long now = System.currentTimeMillis();
        if (force || now - lastVolumeSendMillis > 250L) {
            lastVolumeSendMillis = now;
            spotifyService.setVolume(localVolume);
        }
    }

    private Rect deviceRowRect(int panelX, int panelY, int panelWidth, int index) {
        return new Rect(panelX + 16, panelY + 184 + (index * ROW_HEIGHT), panelWidth - 32, 17);
    }

    private Rect playlistRowRect(int panelX, int baseY, int panelWidth, int index) {
        return new Rect(panelX + 16, baseY + (index * ROW_HEIGHT), panelWidth - 32, 17);
    }

    private int dropdownHeight(boolean open, int size, int visibleRows) {
        return open ? Math.min(visibleRows, size) * ROW_HEIGHT + 8 : 0;
    }

    private int visibleDeviceRows(int screenHeight) {
        int y = panelY(screenHeight, BASE_EXPANDED_HEIGHT);
        int available = Math.max(0, screenHeight - 16 - (y + 184));
        return Math.max(0, Math.min(5, available / ROW_HEIGHT));
    }

    private int visiblePlaylistRows(int screenHeight, int playlistHeaderY) {
        int deviceRows = visibleDeviceRows(screenHeight);
        // Use the maximum possible playlist rows (6) to compute panel height and y,
        // so the panel is positioned high enough to actually fit the rows.
        int y = panelY(screenHeight, panelHeight(deviceRows, 6));
        int available = Math.max(0, screenHeight - 16 - (y + playlistHeaderY + DROPDOWN_HEADER_HEIGHT + 6));
        return Math.max(0, Math.min(6, available / ROW_HEIGHT));
    }

    private int playlistDropdownY(int visibleDeviceRows) {
        return DEVICE_DROPDOWN_TOP + DROPDOWN_HEADER_HEIGHT + (devicesOpen ? visibleDeviceRows * ROW_HEIGHT + 8 : 8);
    }

    private int panelY(int screenHeight, int panelHeight) {
        return Math.max(16, Math.min(screenHeight - panelHeight - 16, screenHeight - panelHeight));
    }

    private int panelX(int screenWidth, int panelWidth) {
        return Math.max(16, screenWidth - panelWidth - 16);
    }

    private int panelHeight(int visibleDeviceRows, int visiblePlaylistRows) {
        int playlistHeaderY = playlistDropdownY(visibleDeviceRows);
        int playlistRowsHeight = playlistsOpen ? visiblePlaylistRows * ROW_HEIGHT + 8 : 0;
        int deviceRowsHeight = devicesOpen ? visibleDeviceRows * ROW_HEIGHT + 8 : 0;
        int contentBottom = playlistHeaderY + DROPDOWN_HEADER_HEIGHT + playlistRowsHeight + 10;
        return Math.max(BASE_EXPANDED_HEIGHT, BASE_CONTENT_BOTTOM + deviceRowsHeight + playlistRowsHeight + 10);
    }

    private String trim(Minecraft client, String text, int maxWidth) {
        if (client.font.width(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (!result.isEmpty() && client.font.width(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private void drawWrapped(GuiGraphicsExtractor context, String text, int x, int y, int maxWidth, int color) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        int lineY = y;
        for (String line : wrap(client, text, maxWidth)) {
            context.text(client.font, Fonts.of(line), x, lineY, color, false);
            lineY += 12;
        }
    }

    private List<String> wrap(Minecraft client, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (client.font.width(candidate) > maxWidth && !current.isEmpty()) {
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
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= bottom();
        }
    }
}
