package de.horizon.spotify;

/** Stub auth service — no login required with OS-based media control. */
public final class SpotifyAuthService {
    public boolean isLoggedIn() { return true; }
    public boolean isLoginInProgress() { return false; }
    public String getStatusMessage() { return "OS Media Control (kein Login noetig)"; }
    public void beginLogin() {}
    public void disconnect() {}
}
