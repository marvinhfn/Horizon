package de.horizon.hypixel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.config.ConfigManager;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HypixelProfileService {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();
    private static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("profile_id:\"([0-9a-f\\-]{36})\"");
    private static final Pattern PROFILE_NAME_PATTERN = Pattern.compile("profile_cute_name:\"([^\"]*)\"");

    @SuppressWarnings("unused")
    private final ConfigManager configManager;

    public HypixelProfileService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public HypixelDungeonStats load(String username) throws IOException, InterruptedException {
        String trimmedName = username == null ? "" : username.trim();
        if (trimmedName.isBlank()) {
            throw new IOException("Kein Spielername angegeben.");
        }

        JsonObject player = requestJson("https://api.mojang.com/users/profiles/minecraft/" + encode(trimmedName), false);
        if (player == null || !player.has("id")) {
            throw new IOException("Spieler konnte nicht aufgeloest werden.");
        }

        String rawUuid = player.get("id").getAsString();
        String resolvedName = player.has("name") ? player.get("name").getAsString() : trimmedName;
        SelectedProfile selectedProfile = resolveSelectedProfile(resolvedName);
        if (selectedProfile == null || selectedProfile.profileId().isBlank()) {
            throw new IOException("SkyBlock Profil konnte nicht bestimmt werden.");
        }

        JsonObject stats = requestJson("https://sky.shiiyu.moe/api/stats/" + rawUuid + "/" + selectedProfile.profileId(), true);
        if (stats == null) {
            throw new IOException("SkyCrypt Stats konnten nicht geladen werden.");
        }

        JsonObject networth = requestJson("https://sky.shiiyu.moe/api/networth/" + rawUuid + "/" + selectedProfile.profileId(), true);
        return parseStats(rawUuid, selectedProfile, stats, networth);
    }

    private HypixelDungeonStats parseStats(String rawUuid, SelectedProfile selectedProfile, JsonObject stats, JsonObject networth) {
        String username = stringValue(stats, "displayName", stringValue(stats, "username", selectedProfile.username()));
        String profileName = stringValue(stats, "profile_cute_name", selectedProfile.profileName());

        JsonObject skills = objectValue(stats, "skills");
        JsonObject skillsMap = objectValue(skills, "skills");
        Map<String, Integer> skillLevels = new HashMap<>();
        Map<String, Float> skillProgress = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : skillsMap.entrySet()) {
            JsonObject skill = entry.getValue().getAsJsonObject();
            skillLevels.put(entry.getKey(), intValue(skill, "level", 0));
            skillProgress.put(entry.getKey(), floatValue(skill, "progress", 0.0F));
        }

        JsonObject slayers = objectValue(stats, "slayers");
        JsonObject slayerLevelsObject = objectValue(slayers, "slayers");
        Map<String, Integer> slayerLevels = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : slayerLevelsObject.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                slayerLevels.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }

        List<String> profileNames = new ArrayList<>();
        JsonArray profiles = arrayValue(stats, "profiles");
        for (JsonElement element : profiles) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject profile = element.getAsJsonObject();
            String cuteName = stringValue(profile, "cute_name", "");
            String mode = stringValue(profile, "game_mode", "");
            profileNames.add(mode.isBlank() ? cuteName : cuteName + " (" + mode + ")");
        }

        Map<String, Double> networthByType = new HashMap<>();
        double totalNetworth = 0.0D;
        double nonCosmeticNetworth = 0.0D;
        if (networth != null) {
            JsonObject normal = objectValue(networth, "normal");
            totalNetworth = doubleValue(normal, "networth", 0.0D);
            JsonObject nonCosmetic = objectValue(networth, "nonCosmetic");
            nonCosmeticNetworth = doubleValue(nonCosmetic, "networth", 0.0D);
            JsonObject types = objectValue(normal, "types");
            for (Map.Entry<String, JsonElement> entry : types.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject typeData = entry.getValue().getAsJsonObject();
                networthByType.put(entry.getKey(), doubleValue(typeData, "total", 0.0D));
            }
        }

        return new HypixelDungeonStats(
            username,
            formatUuid(rawUuid),
            profileName,
            intValue(objectValue(stats, "dungeons"), "dungeoneering", 0),
            0,
            0,
            Map.of(),
            intValue(objectValue(stats, "skyblock_level"), "level", 0),
            doubleValue(skills, "averageSkillLevel", 0.0D),
            longValue(slayers, "xp", 0L),
            doubleValue(stats, "purse", 0.0D),
            doubleValue(stats, "bank", 0.0D),
            doubleValue(stats, "personalBank", 0.0D),
            totalNetworth,
            nonCosmeticNetworth,
            skillLevels,
            skillProgress,
            slayerLevels,
            networthByType,
            profileNames
        );
    }

    private SelectedProfile resolveSelectedProfile(String username) throws IOException, InterruptedException {
        String html = requestText("https://sky.shiiyu.moe/stats/" + encode(username), true);
        if (html == null || html.isBlank()) {
            return null;
        }
        Matcher idMatcher = PROFILE_ID_PATTERN.matcher(html);
        Matcher nameMatcher = PROFILE_NAME_PATTERN.matcher(html);
        String profileId = idMatcher.find() ? idMatcher.group(1) : "";
        String profileName = nameMatcher.find() ? nameMatcher.group(1) : "";
        return new SelectedProfile(username, profileId, profileName);
    }

    private JsonObject requestJson(String url, boolean browserHeaders) throws IOException, InterruptedException {
        String body = requestText(url, browserHeaders);
        if (body == null || body.isBlank()) {
            return null;
        }
        return JsonParser.parseString(body).getAsJsonObject();
    }

    private String requestText(String url, boolean browserHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", browserHeaders ? BROWSER_USER_AGENT : "HorizonMod/1.0")
            .header("Accept", browserHeaders ? "text/html,application/json;q=0.9,*/*;q=0.8" : "application/json")
            .GET();

        HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().isBlank()) {
            return null;
        }
        return response.body();
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

    private String stringValue(JsonObject object, String key, String fallback) {
        if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
            return object.get(key).getAsString();
        }
        return fallback;
    }

    private int intValue(JsonObject object, String key, int fallback) {
        if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
            try {
                return object.get(key).getAsInt();
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private long longValue(JsonObject object, String key, long fallback) {
        if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
            try {
                return object.get(key).getAsLong();
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private double doubleValue(JsonObject object, String key, double fallback) {
        if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
            try {
                return object.get(key).getAsDouble();
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private float floatValue(JsonObject object, String key, float fallback) {
        if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
            try {
                return object.get(key).getAsFloat();
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
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

    private record SelectedProfile(String username, String profileId, String profileName) {
    }
}
