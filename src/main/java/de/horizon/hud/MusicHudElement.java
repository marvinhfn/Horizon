package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.spotify.AlbumArtCache;
import de.horizon.spotify.SpotifyPlaybackState;
import de.horizon.spotify.SpotifyService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * HUD element showing the currently playing Spotify track: album cover, title, artist,
 * a progress bar with playback position, and a play/pause indicator.
 */
public final class MusicHudElement implements HudElement {
    private static final String ID = "music_hud";

    private static final int BASE_W = 162;
    private static final int BASE_H = 52;

    private static final int ART_X = 6;
    private static final int ART_Y = 6;
    private static final int ART_SIZE = 40;
    private static final int CONTENT_X = 52;

    private final SpotifyService spotifyService;
    private final AlbumArtCache albumArt;

    public MusicHudElement(SpotifyService spotifyService, AlbumArtCache albumArt) {
        this.spotifyService = spotifyService;
        this.albumArt = albumArt;
    }

    @Override public String id() { return ID; }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return 5; }
    @Override public int defaultY() { return 140; }
    @Override public int width(Minecraft mc, HudPosition pos) { return (int) Math.ceil(BASE_W * pos.getScale()); }
    @Override public int height(Minecraft mc, HudPosition pos) { return (int) Math.ceil(BASE_H * pos.getScale()); }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isMusicHudEnabled();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Minecraft mc, HudPosition pos, boolean editMode) {
        boolean playing;
        String track;
        String artist;
        long progress;
        long duration;
        String artUrl = "";

        if (editMode) {
            playing = true;
            track = "Song Title";
            artist = "Artist Name";
            progress = 83_000L;
            duration = 225_000L;
        } else {
            spotifyService.requestStateRefresh(false);
            SpotifyPlaybackState state = spotifyService.getPlaybackState();
            if (!state.connected() || state.trackName().isBlank()) {
                return;
            }
            playing = state.playing();
            track = state.trackName();
            artist = state.artistName();
            progress = state.currentProgressMs();
            duration = state.durationMs();
            artUrl = state.albumArtUrl();
            albumArt.ensure(artUrl);
        }

        float scale = (float) pos.getScale();
        ctx.pose().pushMatrix();
        ctx.pose().translate(pos.getX(), pos.getY());
        ctx.pose().scale(scale, scale);

        // Panel background
        ctx.fill(0, 0, BASE_W, BASE_H, 0xC00A0E14);
        ctx.fill(0, 0, BASE_W, 1, HudStyle.accent());

        // Album cover (or placeholder)
        if (!editMode && albumArt.isReady(artUrl)) {
            int texW = albumArt.width();
            int texH = albumArt.height();
            ctx.pose().pushMatrix();
            ctx.pose().translate(ART_X, ART_Y);
            ctx.pose().scale((float) ART_SIZE / texW, (float) ART_SIZE / texH);
            ctx.blit(RenderPipelines.GUI_TEXTURED, albumArt.textureId(), 0, 0, 0f, 0f, texW, texH, texW, texH);
            ctx.pose().popMatrix();
        } else {
            ctx.fill(ART_X, ART_Y, ART_X + ART_SIZE, ART_Y + ART_SIZE, 0xFF1B2230);
            ctx.centeredText(mc.font, "♪", ART_X + ART_SIZE / 2, ART_Y + ART_SIZE / 2 - 4, HudStyle.muted());
        }

        Font font = mc.font;
        int iconX = BASE_W - 14;
        int contentRight = iconX - 4;

        // Track title + artist
        ctx.text(font, truncate(font, track, contentRight - CONTENT_X), CONTENT_X, 7, HudStyle.text());
        ctx.text(font, truncate(font, artist, BASE_W - 8 - CONTENT_X), CONTENT_X, 20, HudStyle.muted());

        // Play / pause indicator (top-right)
        if (playing) {
            drawPauseIcon(ctx, iconX, 7);
        } else {
            drawPlayIcon(ctx, iconX, 7);
        }

        // Time labels
        String times = formatTime(progress) + " / " + formatTime(duration);
        ctx.text(font, times, CONTENT_X, 33, HudStyle.muted());

        // Progress bar
        int barX = CONTENT_X;
        int barY = 45;
        int barW = BASE_W - 8 - CONTENT_X;
        int barH = 4;
        float frac = duration > 0 ? Math.max(0f, Math.min(1f, (float) progress / duration)) : 0f;
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF2A3444);
        ctx.fill(barX, barY, barX + (int) (barW * frac), barY + barH, HudStyle.accent());

        ctx.pose().popMatrix();
    }

    // ── drawing helpers ──────────────────────────────────────────────────────────

    private void drawPauseIcon(GuiGraphicsExtractor ctx, int x, int y) {
        int color = HudStyle.text();
        ctx.fill(x, y, x + 3, y + 9, color);
        ctx.fill(x + 5, y, x + 8, y + 9, color);
    }

    private void drawPlayIcon(GuiGraphicsExtractor ctx, int x, int y) {
        int color = HudStyle.text();
        int h = 9;
        int depth = 8;
        for (int row = 0; row < h; row++) {
            float t = 1f - Math.abs((2f * row - (h - 1)) / (h - 1));
            int len = Math.max(1, Math.round(depth * t));
            ctx.fill(x, y + row, x + len, y + row + 1, color);
        }
    }

    private String truncate(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String ellipsis = "…";
        int ellipsisWidth = font.width(ellipsis);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(builder.toString() + text.charAt(i)) + ellipsisWidth > maxWidth) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder + ellipsis;
    }

    private String formatTime(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}
