package de.horizon.youtube;

public record YoutubePlaylist(String title, String playlistId) {
    public String musicUrl() {
        return "https://music.youtube.com/playlist?list=" + playlistId;
    }
}
