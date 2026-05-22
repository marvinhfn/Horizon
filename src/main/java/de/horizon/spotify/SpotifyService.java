package de.horizon.spotify;

import de.horizon.HorizonMod;
import de.horizon.config.ConfigManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Controls the system media player (Spotify or any other) via OS-level APIs.
 * No Spotify developer key or OAuth login required.
 *
 * Platform support:
 *  - Linux: playerctl (must be installed)
 *  - macOS: osascript / AppleScript targeting Spotify
 *  - Windows: PowerShell SendKeys for control, SMTC for track info
 */
public final class SpotifyService {
    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean IS_WINDOWS = OS.contains("win");
    private static final boolean IS_MAC = OS.contains("mac");

    private static final long POLL_INTERVAL_MS = 8000L;
    private static final long FORCE_POLL_DELAY_MS = 400L;

    private final SpotifyAuthService authService = new SpotifyAuthService();

    private volatile SpotifyPlaybackState playbackState =
        SpotifyPlaybackState.unavailable("Initialisierung...", true);
    private volatile long lastStateFetch;
    private volatile boolean stateFetchInFlight;

    public SpotifyService(ConfigManager configManager) {
        // ConfigManager not needed for OS-based control; kept for API compatibility
    }

    public SpotifyAuthService auth() {
        return authService;
    }

    public SpotifyPlaybackState getPlaybackState() {
        return playbackState;
    }

    public List<SpotifyPlaylist> getRecentPlaylists() {
        return List.of();
    }

    public List<SpotifyDevice> getDevices() {
        return List.of();
    }

    public void requestStateRefresh(boolean force) {
        long now = Instant.now().toEpochMilli();
        if (!force && now - lastStateFetch < POLL_INTERVAL_MS) return;
        if (stateFetchInFlight) return;
        stateFetchInFlight = true;
        CompletableFuture.runAsync(() -> {
            try {
                playbackState = queryOsPlaybackState();
                lastStateFetch = Instant.now().toEpochMilli();
            } catch (Exception e) {
                HorizonMod.LOGGER.debug("OS media state query failed", e);
            } finally {
                stateFetchInFlight = false;
            }
        });
    }

    public void playPause() {
        boolean wasPlaying = playbackState.playing();
        runAsync(playPauseCommand());
        // Optimistic update so the button label flips immediately
        playbackState = new SpotifyPlaybackState(
            playbackState.connected(), playbackState.premiumLikelyRequired(),
            !wasPlaying, playbackState.hasActiveDevice(), playbackState.supportsVolume(),
            playbackState.trackName(), playbackState.artistName(),
            playbackState.deviceName(), playbackState.deviceId(),
            playbackState.volumePercent(), playbackState.statusMessage());
        scheduleRefresh();
    }

    public void skipNext() {
        runAsync(nextCommand());
        scheduleRefresh();
    }

    public void skipPrevious() {
        runAsync(prevCommand());
        scheduleRefresh();
    }

    public void changeVolume(int delta) {
        if (IS_WINDOWS) {
            int presses = Math.max(1, Math.abs(delta) / 10);
            String[] cmd = delta > 0 ? winVolumeUpCommand() : winVolumeDownCommand();
            for (int i = 0; i < presses; i++) {
                runAsync(cmd);
            }
        } else {
            int current = playbackState.volumePercent();
            int target = Math.max(0, Math.min(100, current + delta));
            setVolume(target);
        }
    }

    public void setVolume(int volumePercent) {
        int target = Math.max(0, Math.min(100, volumePercent));
        String[] cmd = setVolumeCommand(target);
        if (cmd == null) return;
        runAsync(cmd);
        playbackState = new SpotifyPlaybackState(
            playbackState.connected(), playbackState.premiumLikelyRequired(),
            playbackState.playing(), playbackState.hasActiveDevice(), playbackState.supportsVolume(),
            playbackState.trackName(), playbackState.artistName(),
            playbackState.deviceName(), playbackState.deviceId(),
            target, playbackState.statusMessage());
    }

    // ── Not supported without Spotify API ─────────────────────────────────────

    public void playPlaylist(SpotifyPlaylist playlist) {}
    public void requestDevicesRefresh(boolean force) {}
    public void requestRecentPlaylistsRefresh(boolean force) {}
    public void selectDevice(SpotifyDevice device) {}

    // ── OS query ──────────────────────────────────────────────────────────────

    private SpotifyPlaybackState queryOsPlaybackState() {
        try {
            if (IS_MAC)     return queryMac();
            if (IS_WINDOWS) return queryWindows();
            return queryLinux();
        } catch (Exception e) {
            HorizonMod.LOGGER.debug("OS playback query failed", e);
            return SpotifyPlaybackState.unavailable("OS Media Control", true);
        }
    }

    private SpotifyPlaybackState queryLinux() throws IOException, InterruptedException {
        String out = exec("playerctl", "metadata", "--format",
            "{{status}}|{{artist}}|{{title}}|{{volume}}");
        if (out.isBlank()) {
            return SpotifyPlaybackState.unavailable("Kein Player (playerctl nicht gefunden)", true);
        }
        String[] p = out.split("\\|", 4);
        boolean playing = p.length > 0 && "Playing".equalsIgnoreCase(p[0].trim());
        String artist = p.length > 1 ? p[1].trim() : "";
        String title  = p.length > 2 ? p[2].trim() : "";
        int volume = 100;
        if (p.length > 3) {
            try { volume = Math.round(Float.parseFloat(p[3].trim()) * 100); }
            catch (NumberFormatException ignored) {}
        }
        return new SpotifyPlaybackState(true, false, playing, true, true,
            title, artist, "OS Media Control", "", volume,
            playing ? "Wiedergabe" : "Pausiert");
    }

    private SpotifyPlaybackState queryMac() throws IOException, InterruptedException {
        String out = exec("osascript", "-e",
            "tell application \"Spotify\" to return " +
            "(player state as text) & \"|\" & " +
            "(artist of current track) & \"|\" & " +
            "(name of current track) & \"|\" & " +
            "(sound volume as text)");
        if (out.isBlank() || out.contains("execution error")) {
            return SpotifyPlaybackState.unavailable("Spotify nicht geoeffnet", true);
        }
        String[] p = out.split("\\|", 4);
        boolean playing = p.length > 0 && "playing".equalsIgnoreCase(p[0].trim());
        String artist = p.length > 1 ? p[1].trim() : "";
        String title  = p.length > 2 ? p[2].trim() : "";
        int volume = 100;
        if (p.length > 3) {
            try { volume = Integer.parseInt(p[3].trim()); }
            catch (NumberFormatException ignored) {}
        }
        return new SpotifyPlaybackState(true, false, playing, true, true,
            title, artist, "Spotify (Mac)", "", volume,
            playing ? "Wiedergabe" : "Pausiert");
    }

    private SpotifyPlaybackState queryWindows() throws IOException, InterruptedException {
        // Query Spotify window title: "Artist - Track" when playing, "Spotify" when paused
        String out = exec("powershell", "-NoProfile", "-Command",
            "(Get-Process -Name Spotify -ErrorAction SilentlyContinue " +
            "| Where-Object {$_.MainWindowTitle -ne ''} " +
            "| Select-Object -First 1 -ExpandProperty MainWindowTitle)");
        if (out.isBlank() || out.equalsIgnoreCase("Spotify")) {
            return SpotifyPlaybackState.unavailable("Spotify pausiert oder nicht geoeffnet", true);
        }
        // Title format: "Artist - Track" (when playing)
        int dash = out.indexOf(" - ");
        String artist = dash >= 0 ? out.substring(0, dash).trim() : "";
        String title  = dash >= 0 ? out.substring(dash + 3).trim() : out.trim();
        return new SpotifyPlaybackState(true, false, true, true, false,
            title, artist, "Spotify (Windows)", "", 0,
            "Wiedergabe");
    }

    // ── Platform commands ─────────────────────────────────────────────────────

    private String[] playPauseCommand() {
        if (IS_MAC) return new String[]{"osascript", "-e", "tell application \"Spotify\" to playpause"};
        if (IS_WINDOWS) return psKey(179); // VK_MEDIA_PLAY_PAUSE
        return new String[]{"playerctl", "play-pause"};
    }

    private String[] nextCommand() {
        if (IS_MAC) return new String[]{"osascript", "-e", "tell application \"Spotify\" to play next track"};
        if (IS_WINDOWS) return psKey(176); // VK_MEDIA_NEXT_TRACK
        return new String[]{"playerctl", "next"};
    }

    private String[] prevCommand() {
        if (IS_MAC) return new String[]{"osascript", "-e", "tell application \"Spotify\" to play previous track"};
        if (IS_WINDOWS) return psKey(177); // VK_MEDIA_PREV_TRACK
        return new String[]{"playerctl", "previous"};
    }

    private String[] winVolumeUpCommand()   { return psKey(175); } // VK_VOLUME_UP
    private String[] winVolumeDownCommand() { return psKey(174); } // VK_VOLUME_DOWN

    private String[] setVolumeCommand(int percent) {
        if (IS_MAC) return new String[]{"osascript", "-e",
            "tell application \"Spotify\" to set sound volume to " + percent};
        if (IS_WINDOWS) return null; // no exact volume control without extra tooling
        return new String[]{"playerctl", "volume", String.format(Locale.ROOT, "%.2f", percent / 100.0f)};
    }

    private static String[] psKey(int vk) {
        return new String[]{"powershell", "-NoProfile", "-Command",
            "(New-Object -ComObject WScript.Shell).SendKeys([char]" + vk + ")"};
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void runAsync(String[] command) {
        if (command == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                new ProcessBuilder(command).redirectErrorStream(true).start().waitFor();
            } catch (Exception e) {
                HorizonMod.LOGGER.debug("Media control command failed", e);
            }
        });
    }

    private void scheduleRefresh() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(FORCE_POLL_DELAY_MS);
                lastStateFetch = 0;
                requestStateRefresh(true);
            } catch (InterruptedException ignored) {}
        });
    }

    private String exec(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        process.waitFor();
        return output;
    }
}
