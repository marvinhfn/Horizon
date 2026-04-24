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
    String statusMessage
) {
    public static SpotifyPlaybackState disconnected(String message) {
        return new SpotifyPlaybackState(false, false, false, false, false, "", "", "", "", 0, message);
    }

    public static SpotifyPlaybackState unavailable(String message, boolean connected) {
        return new SpotifyPlaybackState(connected, false, false, false, false, "", "", "", "", 0, message);
    }
}
