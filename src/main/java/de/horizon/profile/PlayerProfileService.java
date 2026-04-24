package de.horizon.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class PlayerProfileService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public PlayerProfileData load(String requestedName) throws IOException, InterruptedException {
        String trimmedName = requestedName == null ? "" : requestedName.trim();
        if (trimmedName.isEmpty()) {
            throw new IOException("Kein Spielername angegeben.");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + encode(trimmedName)))
            .timeout(Duration.ofSeconds(8))
            .header("User-Agent", "HorizonMod/1.0")
            .GET()
            .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().isBlank()) {
            throw new IOException("Spielerprofil konnte nicht geladen werden.");
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        String username = root.get("name").getAsString();
        String uuid = root.get("id").getAsString();
        return new PlayerProfileData(
            username,
            formatUuid(uuid),
            "https://sky.shiiyu.moe/stats/" + encode(username),
            "https://namemc.com/profile/" + encode(username)
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String formatUuid(String rawUuid) {
        if (rawUuid.length() != 32) {
            return rawUuid;
        }

        return rawUuid.substring(0, 8) + "-"
            + rawUuid.substring(8, 12) + "-"
            + rawUuid.substring(12, 16) + "-"
            + rawUuid.substring(16, 20) + "-"
            + rawUuid.substring(20);
    }
}
