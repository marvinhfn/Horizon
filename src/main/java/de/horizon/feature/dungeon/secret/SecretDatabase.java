package de.horizon.feature.dungeon.secret;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the dungeon secret positions from {@code secrets.json}. Positions are
 * stored per room in the room-local (component) coordinate frame; callers use
 * the room detector's {@code relativeToWorld} to place them in the world.
 */
public final class SecretDatabase {

    private static final Map<String, Map<SecretType, List<BlockPos>>> BY_ROOM = load();

    private SecretDatabase() {}

    /** Secret positions (room-relative) for a room name, or an empty map when unknown. */
    public static Map<SecretType, List<BlockPos>> forRoom(String roomName) {
        if (roomName == null || roomName.isEmpty()) return Map.of();
        return BY_ROOM.getOrDefault(roomName.toLowerCase(Locale.ROOT), Map.of());
    }

    public static boolean hasData() {
        return !BY_ROOM.isEmpty();
    }

    private static Map<String, Map<SecretType, List<BlockPos>>> load() {
        Map<String, Map<SecretType, List<BlockPos>>> result = new HashMap<>();
        try (InputStream stream = SecretDatabase.class.getResourceAsStream("/assets/horizon/dungeons/secrets.json")) {
            if (stream == null) return result;
            JsonArray rooms = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : rooms) {
                if (!element.isJsonObject()) continue;
                JsonObject room = element.getAsJsonObject();
                if (!room.has("name") || !room.has("waypoints")) continue;
                String name = room.get("name").getAsString().toLowerCase(Locale.ROOT);
                JsonObject waypoints = room.getAsJsonObject("waypoints");

                Map<SecretType, List<BlockPos>> byType = new EnumMap<>(SecretType.class);
                for (Map.Entry<String, JsonElement> entry : waypoints.entrySet()) {
                    SecretType type = SecretType.fromKey(entry.getKey());
                    if (type == null || !entry.getValue().isJsonArray()) continue;
                    List<BlockPos> positions = new ArrayList<>();
                    for (JsonElement coord : entry.getValue().getAsJsonArray()) {
                        if (!coord.isJsonArray()) continue;
                        JsonArray xyz = coord.getAsJsonArray();
                        if (xyz.size() < 3) continue;
                        positions.add(new BlockPos(xyz.get(0).getAsInt(), xyz.get(1).getAsInt(), xyz.get(2).getAsInt()));
                    }
                    if (!positions.isEmpty()) byType.put(type, positions);
                }
                if (!byType.isEmpty()) result.put(name, byType);
            }
        } catch (IOException | RuntimeException ignored) {}
        return result;
    }
}
