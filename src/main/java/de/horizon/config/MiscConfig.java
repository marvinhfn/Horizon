package de.horizon.config;

import de.horizon.Lang;

public final class MiscConfig {
    Lang.Language language = Lang.Language.DE;
    String hypixelApiKey = "";
    boolean horizonBackendEnabled = false;
    String horizonBackendBaseUrl = "https://api.horizon.local";
    String horizonBackendAccessToken = "";
    long horizonBackendTokenExpiresAt = 0L;
    String horizonBackendAudience = "horizon-profile-api";
}
