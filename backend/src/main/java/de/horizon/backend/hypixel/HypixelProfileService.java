package de.horizon.backend.hypixel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.backend.config.BackendConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class HypixelProfileService {
    private final BackendConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public HypixelProfileService(BackendConfig config) {
        this.config = config;
    }

    public JsonObject loadProfileSummary(String playerName) throws IOException, InterruptedException {
        if (config.hypixelApiKey().isBlank()) {
            throw new IOException("HYPIXEL_API_KEY ist nicht gesetzt.");
        }
        String trimmed = playerName == null ? "" : playerName.trim();
        if (trimmed.isBlank()) {
            throw new IOException("Kein Spielername angegeben.");
        }

        JsonObject mojangProfile = getJson("https://api.mojang.com/users/profiles/minecraft/" + encode(trimmed), false);
        if (mojangProfile == null || !mojangProfile.has("id")) {
            throw new IOException("Spieler konnte nicht aufgeloest werden.");
        }

        String rawUuid = mojangProfile.get("id").getAsString();
        JsonObject profilesRoot = getJson("https://api.hypixel.net/v2/skyblock/profiles?uuid=" + rawUuid, true);
        JsonArray profiles = arrayValue(profilesRoot, "profiles");

        JsonObject selectedProfile = new JsonObject();
        for (JsonElement element : profiles) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject profile = element.getAsJsonObject();
            if (booleanValue(profile, "selected")) {
                selectedProfile = profile;
                break;
            }
        }
        if (selectedProfile.isEmpty() && !profiles.isEmpty() && profiles.get(0).isJsonObject()) {
            selectedProfile = profiles.get(0).getAsJsonObject();
        }

        JsonObject response = new JsonObject();
        response.addProperty("playerName", mojangProfile.has("name") ? mojangProfile.get("name").getAsString() : trimmed);
        response.addProperty("playerUuid", formatUuid(rawUuid));
        response.add("profiles", profiles);
        response.add("selectedProfile", selectedProfile);
        response.addProperty("profileCount", profiles.size());

        if (!selectedProfile.isEmpty()) {
            response.addProperty("profileId", stringValue(selectedProfile, "profile_id"));
            response.addProperty("profileName", stringValue(selectedProfile, "cute_name"));
            response.addProperty("gameMode", stringValue(selectedProfile, "game_mode"));
            response.addProperty("bankBalance", doubleValue(objectValue(selectedProfile, "banking"), "balance"));
            response.add("member", firstMember(selectedProfile));
        }
        return response;
    }

    private JsonObject getJson(String url, boolean withApiKey) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .header("User-Agent", "HorizonBackend/0.1")
            .GET();
        if (withApiKey) {
            builder.header("API-Key", config.hypixelApiKey());
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().isBlank()) {
            throw new IOException("HTTP " + response.statusCode() + " fuer " + url);
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private JsonObject firstMember(JsonObject profile) {
        JsonObject members = objectValue(profile, "members");
        for (String key : members.keySet()) {
            JsonElement element = members.get(key);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        }
        return new JsonObject();
    }

    private JsonObject objectValue(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return object.getAsJsonObject(key);
    }

    private JsonArray arrayValue(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(key);
    }

    private boolean booleanValue(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsBoolean();
    }

    private String stringValue(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private double doubleValue(JsonObject object, String key) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsDouble() : 0.0D;
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String formatUuid(String rawUuid) {
        if (rawUuid == null || rawUuid.length() != 32) {
            return rawUuid == null ? "" : rawUuid;
        }
        return rawUuid.substring(0, 8) + "-"
            + rawUuid.substring(8, 12) + "-"
            + rawUuid.substring(12, 16) + "-"
            + rawUuid.substring(16, 20) + "-"
            + rawUuid.substring(20);
    }
}
