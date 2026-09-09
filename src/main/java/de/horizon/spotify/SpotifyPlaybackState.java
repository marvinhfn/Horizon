package de.horizon.spotify;

public record SpotifyPlaybackState(
    boolean connected,
    boolean premiumLikelyRequired,
    boolean playing,
    boolean hasActiveDevice,
    boolean supportsVolume,
    String trackName,
    String artistName,
    String deviceName,
    String deviceId,
    int volumePercent,
    String statusMessage,
    long progressMs,
    long durationMs,
    String albumArtUrl,
    long fetchedAtMillis
) {
    public static SpotifyPlaybackState disconnected(String message) {
        return new SpotifyPlaybackState(false, false, false, false, false, "", "", "", "", 0, message,
            0L, 0L, "", System.currentTimeMillis());
    }

    public static SpotifyPlaybackState unavailable(String message, boolean connected) {
        return new SpotifyPlaybackState(connected, false, false, false, false, "", "", "", "", 0, message,
            0L, 0L, "", System.currentTimeMillis());
    }

    /**
     * Returns a copy with the play/pause flag flipped, snapshotting the interpolated progress so the
     * bar neither jumps back to the last fetch nor keeps advancing while paused. Used for the optimistic
     * local update when the user presses play/pause.
     */
    public SpotifyPlaybackState withPlaying(boolean nowPlaying) {
        return new SpotifyPlaybackState(connected, premiumLikelyRequired, nowPlaying, hasActiveDevice, supportsVolume,
            trackName, artistName, deviceName, deviceId, volumePercent, statusMessage,
            currentProgressMs(), durationMs, albumArtUrl, System.currentTimeMillis());
    }

    /**
     * Progress interpolated to the current wall-clock time. While playing, the API-reported
     * progress advances by the elapsed time since the state was fetched, clamped to the track
     * length so the bar keeps moving smoothly between the ~10s state refreshes.
     */
    public long currentProgressMs() {
        if (durationMs <= 0L) {
            return 0L;
        }
        long progress = progressMs;
        if (playing) {
            progress += Math.max(0L, System.currentTimeMillis() - fetchedAtMillis);
        }
        return Math.max(0L, Math.min(durationMs, progress));
    }
}
