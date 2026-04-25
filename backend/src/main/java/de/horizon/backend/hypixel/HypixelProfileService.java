package de.horizon.backend.hypixel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.backend.config.BackendConfig;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public final class HypixelProfileService {
    private static final String USER_AGENT = "HorizonBackend/0.1";
    private static final Pattern RARITY_PATTERN = Pattern.compile("(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL|SUPREME)");
    private static final double[] CATACOMBS_XP = {
        0, 50, 75, 110, 160, 230, 330, 470, 670, 950, 1340, 1890, 2665, 3760, 5260, 7380, 10300,
        14400, 20000, 27600, 38000, 52500, 71500, 97000, 132000, 180000, 243000, 328000, 445000,
        600000, 800000, 1065000, 1410000, 1900000, 2500000, 3300000, 4300000, 5600000, 7200000,
        9200000, 12000000, 15000000, 19000000, 24000000, 30000000, 38000000, 48000000, 60000000,
        75000000, 93000000, 116250000
    };

    private final BackendConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public HypixelProfileService(BackendConfig config) {
        this.config = config;
    }

    public JsonObject loadProfile(String playerName) throws IOException, InterruptedException {
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
        String playerUuid = formatUuid(rawUuid);
        JsonObject profilesRoot = getJson("https://api.hypixel.net/v2/skyblock/profiles?uuid=" + rawUuid, true);
        JsonArray profiles = arrayValue(profilesRoot, "profiles");
        JsonObject selectedProfile = selectProfile(profiles, rawUuid);
        if (selectedProfile.entrySet().isEmpty()) {
            throw new IOException("SkyBlock Profil konnte nicht bestimmt werden.");
        }

        JsonObject member = memberFor(selectedProfile, rawUuid);
        if (member.entrySet().isEmpty()) {
            throw new IOException("Profilmitglied konnte nicht bestimmt werden.");
        }

        String profileId = stringValue(selectedProfile, "profile_id");
        JsonObject stats = requestOptionalJson("https://sky.shiiyu.moe/api/stats/" + rawUuid + "/" + profileId);
        JsonObject networth = requestOptionalJson("https://sky.shiiyu.moe/api/networth/" + rawUuid + "/" + profileId);

        JsonObject profile = new JsonObject();
        String resolvedName = mojangProfile.has("name") ? mojangProfile.get("name").getAsString() : trimmed;
        JsonArray storagePages = parseStorages(member);

        profile.addProperty("playerName", resolvedName);
        profile.addProperty("playerUuid", playerUuid);
        profile.addProperty("profileId", profileId);
        profile.addProperty("profileName", stringValue(selectedProfile, "cute_name"));
        profile.addProperty("gameMode", stringValue(selectedProfile, "game_mode"));
        profile.addProperty("skyblockLevel", resolveSkyBlockLevel(stats, member));
        profile.addProperty("catacombsLevel", resolveCatacombsLevel(member));
        profile.addProperty("purse", doubleValue(stats, "purse", doubleValue(member, "coin_purse", 0.0D)));
        profile.addProperty("bank", doubleValue(objectValue(selectedProfile, "banking"), "balance", doubleValue(stats, "bank", 0.0D)));
        profile.addProperty("networth", resolveNetworth(networth));
        profile.add("profileNames", parseProfileNames(profiles));
        profile.add("storages", storagePages);
        profile.add("accessories", parseAccessories(storagePages));
        profile.add("pets", parsePets(member));
        profile.add("skills", parseSkills(stats, member));
        profile.add("slayers", parseSlayers(member));
        profile.add("metadata", buildMetadata(selectedProfile, member, stats, networth, profiles.size()));

        JsonObject response = new JsonObject();
        response.add("profile", profile);
        return response;
    }

    private JsonObject selectProfile(JsonArray profiles, String rawUuid) {
        JsonObject selected = new JsonObject();
        long latestLastSave = Long.MIN_VALUE;
        for (JsonElement element : profiles) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject profile = element.getAsJsonObject();
            JsonObject member = memberFor(profile, rawUuid);
            long lastSave = longValue(member, "last_save", 0L);
            if (booleanValue(profile, "selected")) {
                return profile;
            }
            if (lastSave > latestLastSave) {
                latestLastSave = lastSave;
                selected = profile;
            }
        }
        if (selected.entrySet().isEmpty() && !profiles.isEmpty() && profiles.get(0).isJsonObject()) {
            return profiles.get(0).getAsJsonObject();
        }
        return selected;
    }

    private JsonObject memberFor(JsonObject profile, String rawUuid) {
        JsonObject members = objectValue(profile, "members");
        if (members.has(rawUuid) && members.get(rawUuid).isJsonObject()) {
            return members.getAsJsonObject(rawUuid);
        }
        String formattedUuid = formatUuid(rawUuid);
        if (members.has(formattedUuid) && members.get(formattedUuid).isJsonObject()) {
            return members.getAsJsonObject(formattedUuid);
        }
        for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                return entry.getValue().getAsJsonObject();
            }
        }
        return new JsonObject();
    }

    private JsonArray parseProfileNames(JsonArray profiles) {
        JsonArray values = new JsonArray();
        for (JsonElement element : profiles) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject profile = element.getAsJsonObject();
            String cuteName = stringValue(profile, "cute_name");
            String mode = stringValue(profile, "game_mode");
            values.add(mode.isBlank() ? cuteName : cuteName + " (" + mode + ")");
        }
        return values;
    }

    private JsonArray parseStorages(JsonObject member) {
        JsonArray storages = new JsonArray();
        addInventoryPage(storages, member, "inv_contents", "inventory", "Inventory", 9, 4);
        addInventoryPage(storages, member, "ender_chest_contents", "ender_chest", "Ender Chest", 9, 6);
        addInventoryPage(storages, member, "inv_armor", "armor", "Armor", 4, 1);
        addInventoryPage(storages, member, "equipment_contents", "equipment", "Equipment", 4, 1);
        addInventoryPage(storages, member, "wardrobe_contents", "wardrobe", "Wardrobe", 9, 4);
        addInventoryPage(storages, member, "personal_vault_contents", "personal_vault", "Personal Vault", 9, 6);
        addInventoryPage(storages, member, "talisman_bag", "talisman_bag", "Accessory Bag", 9, 6);
        addInventoryPage(storages, member, "accessory_bag_storage", "accessory_bag", "Accessory Bag", 9, 6);
        addInventoryPage(storages, member, "fishing_bag", "fishing_bag", "Fishing Bag", 9, 6);
        addInventoryPage(storages, member, "quiver", "quiver", "Quiver", 9, 5);
        addInventoryPage(storages, member, "potion_bag", "potion_bag", "Potion Bag", 9, 6);
        addInventoryPage(storages, member, "candy_inventory_contents", "candy_inventory", "Candy Inventory", 9, 6);
        addPagedInventories(storages, objectValue(member, "backpack_contents"), "backpack", "Backpack");
        addPagedInventories(storages, objectValue(member, "storage_contents"), "storage", "Storage");
        addPagedInventories(storages, objectValue(member, "backpack_icons"), "backpack_icon", "Backpack Icon");
        return storages;
    }

    private void addInventoryPage(JsonArray storages, JsonObject member, String sourceKey, String id, String title, int columns, int rows) {
        String encoded = encodedInventory(member, sourceKey);
        JsonObject page = decodeInventoryPage(id, title, encoded, columns, rows);
        if (page != null) {
            storages.add(page);
        }
    }

    private void addPagedInventories(JsonArray storages, JsonObject pages, String idPrefix, String titlePrefix) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(pages.entrySet());
        entries.sort(Comparator.comparingInt(entry -> parsePageIndex(entry.getKey())));
        for (Map.Entry<String, JsonElement> entry : entries) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject pageObject = entry.getValue().getAsJsonObject();
            String encoded = stringValue(pageObject, "data");
            if (encoded.isBlank()) {
                continue;
            }
            int pageIndex = parsePageIndex(entry.getKey()) + 1;
            JsonObject page = decodeInventoryPage(idPrefix + "_" + pageIndex, titlePrefix + " " + pageIndex, encoded, 9, 6);
            if (page != null) {
                storages.add(page);
            }
        }
    }

    private int parsePageIndex(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE / 2;
        }
    }

    private String encodedInventory(JsonObject member, String key) {
        JsonElement element = member.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonObject()) {
            return stringValue(element.getAsJsonObject(), "data");
        }
        return "";
    }

    private JsonObject decodeInventoryPage(String id, String title, String encoded, int columns, int rows) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            CompoundTag root = decodeInventory(encoded);
            ListTag<?> items = root.getListTag("i");
            if (items == null) {
                return null;
            }
            JsonArray slots = new JsonArray();
            int maxSlot = -1;
            for (Tag<?> entry : items) {
                if (!(entry instanceof CompoundTag itemTag)) {
                    continue;
                }
                int slot = Byte.toUnsignedInt(itemTag.getByte("Slot").orElse((byte) 0));
                maxSlot = Math.max(maxSlot, slot);
                JsonObject slotObject = parseSlot(itemTag, slot, columns);
                if (slotObject != null) {
                    slots.add(slotObject);
                }
            }
            int resolvedColumns = Math.max(1, columns);
            int resolvedRows = rows > 0 ? rows : Math.max(1, ((maxSlot + 1) + resolvedColumns - 1) / resolvedColumns);

            JsonObject page = new JsonObject();
            page.addProperty("id", id);
            page.addProperty("title", title);
            page.addProperty("columns", resolvedColumns);
            page.addProperty("rows", resolvedRows);
            page.add("slots", slots);
            return page;
        } catch (Exception ignored) {
            return null;
        }
    }

    private CompoundTag decodeInventory(String encoded) throws IOException {
        byte[] compressed = Base64.getDecoder().decode(encoded);
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            NamedTag namedTag = new NBTDeserializer(false).fromStream(input);
            if (namedTag.getTag() instanceof CompoundTag compoundTag) {
                return compoundTag;
            }
            throw new IOException("Inventory root is not a compound tag.");
        }
    }

    private JsonObject parseSlot(CompoundTag itemTag, int slot, int columns) {
        CompoundTag tag = itemTag.getCompoundTag("tag");
        CompoundTag display = tag == null ? null : tag.getCompoundTag("display");
        String displayName = display == null ? "" : cleanText(display.getString("Name").orElse(""));
        String lore = loreText(display);
        String itemId = resolveItemId(itemTag, tag);
        int count = Byte.toUnsignedInt(itemTag.getByte("Count").orElse((byte) 0));
        boolean enchanted = tag != null && tag.containsKey("ench");

        if (displayName.isBlank() && itemId.isBlank()) {
            return null;
        }

        JsonObject item = new JsonObject();
        item.addProperty("itemId", itemId);
        item.addProperty("displayName", displayName.isBlank() ? humanize(itemId) : displayName);
        item.addProperty("rarity", resolveRarity(lore));
        item.addProperty("count", Math.max(1, count));
        item.addProperty("lore", lore);
        item.addProperty("iconTexture", "");
        item.addProperty("enchanted", enchanted);

        JsonObject slotObject = new JsonObject();
        slotObject.addProperty("index", slot);
        slotObject.addProperty("x", slot % Math.max(1, columns));
        slotObject.addProperty("y", slot / Math.max(1, columns));
        slotObject.add("item", item);
        return slotObject;
    }

    private String resolveItemId(CompoundTag itemTag, CompoundTag tag) {
        if (tag != null) {
            CompoundTag extraAttributes = tag.getCompoundTag("ExtraAttributes");
            if (extraAttributes != null) {
                String internalId = extraAttributes.getString("id").orElse("");
                if (internalId != null && !internalId.isBlank()) {
                    return internalId;
                }
            }
        }
        return itemTag.getString("id").orElse("");
    }

    private String loreText(CompoundTag display) {
        if (display == null) {
            return "";
        }
        ListTag<?> loreList = display.getListTag("Lore");
        if (loreList == null || loreList.size() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Tag<?> entry : loreList) {
            if (entry instanceof StringTag stringTag) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(cleanText(stringTag.getValue()));
            }
        }
        return builder.toString();
    }

    private JsonArray parseAccessories(JsonArray storages) {
        JsonArray accessories = new JsonArray();
        for (JsonElement element : storages) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject page = element.getAsJsonObject();
            String id = stringValue(page, "id");
            if (!(id.contains("accessory") || id.contains("talisman"))) {
                continue;
            }
            for (JsonElement slotElement : arrayValue(page, "slots")) {
                if (!slotElement.isJsonObject()) {
                    continue;
                }
                JsonObject slot = slotElement.getAsJsonObject();
                JsonObject item = objectValue(slot, "item");
                if (item.entrySet().isEmpty()) {
                    continue;
                }
                JsonObject accessory = new JsonObject();
                accessory.addProperty("id", stringValue(item, "itemId"));
                accessory.addProperty("displayName", stringValue(item, "displayName"));
                accessory.addProperty("rarity", stringValue(item, "rarity"));
                accessory.addProperty("enrichment", resolveEnrichment(stringValue(item, "lore")));
                accessory.addProperty("active", true);
                accessories.add(accessory);
            }
        }
        return accessories;
    }

    private String resolveEnrichment(String lore) {
        for (String line : lore.split("\\R")) {
            if (line.toLowerCase(Locale.ROOT).contains("enrichment")) {
                return line.trim();
            }
        }
        return "";
    }

    private JsonArray parsePets(JsonObject member) {
        JsonArray values = new JsonArray();
        for (JsonElement element : arrayValue(member, "pets")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject pet = element.getAsJsonObject();
            JsonObject parsed = new JsonObject();
            parsed.addProperty("type", stringValue(pet, "type"));
            parsed.addProperty("displayName", humanize(stringValue(pet, "type")));
            parsed.addProperty("tier", stringValue(pet, "tier"));
            parsed.addProperty("level", intValue(pet, "level", 0));
            parsed.addProperty("active", booleanValue(pet, "active"));
            parsed.addProperty("heldItem", stringValue(pet, "heldItem"));
            values.add(parsed);
        }
        return values;
    }

    private JsonArray parseSkills(JsonObject stats, JsonObject member) {
        JsonArray values = new JsonArray();
        JsonObject skillsRoot = objectValue(objectValue(stats, "skills"), "skills");
        if (!skillsRoot.entrySet().isEmpty()) {
            for (Map.Entry<String, JsonElement> entry : skillsRoot.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject skill = entry.getValue().getAsJsonObject();
                JsonObject parsed = new JsonObject();
                parsed.addProperty("id", entry.getKey());
                parsed.addProperty("displayName", humanize(entry.getKey()));
                parsed.addProperty("level", intValue(skill, "level", 0));
                parsed.addProperty("progress", doubleValue(skill, "progress", 0.0D));
                parsed.addProperty("experience", doubleValue(skill, "xp", 0.0D));
                values.add(parsed);
            }
            return values;
        }

        for (Map.Entry<String, JsonElement> entry : member.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("experience_skill_") || !entry.getValue().isJsonPrimitive()) {
                continue;
            }
            String id = key.substring("experience_skill_".length());
            JsonObject parsed = new JsonObject();
            parsed.addProperty("id", id);
            parsed.addProperty("displayName", humanize(id));
            parsed.addProperty("level", 0);
            parsed.addProperty("progress", 0.0D);
            parsed.addProperty("experience", entry.getValue().getAsDouble());
            values.add(parsed);
        }
        return values;
    }

    private JsonArray parseSlayers(JsonObject member) {
        JsonArray values = new JsonArray();
        JsonObject slayers = objectValue(member, "slayer_bosses");
        for (Map.Entry<String, JsonElement> entry : slayers.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject boss = entry.getValue().getAsJsonObject();
            JsonObject parsed = new JsonObject();
            parsed.addProperty("id", entry.getKey());
            parsed.addProperty("displayName", humanize(entry.getKey()));
            parsed.addProperty("level", slayerLevel(boss));
            parsed.addProperty("experience", longValue(boss, "xp", 0L));
            parsed.addProperty("kills", slayerKills(boss));
            values.add(parsed);
        }
        return values;
    }

    private int slayerLevel(JsonObject boss) {
        JsonObject claimedLevels = objectValue(boss, "claimed_levels");
        int highest = 0;
        for (String key : claimedLevels.keySet()) {
            Matcher matcher = Pattern.compile("(\\d+)$").matcher(key);
            if (matcher.find()) {
                highest = Math.max(highest, Integer.parseInt(matcher.group(1)));
            }
        }
        return highest;
    }

    private int slayerKills(JsonObject boss) {
        int kills = 0;
        for (Map.Entry<String, JsonElement> entry : boss.entrySet()) {
            if (entry.getKey().startsWith("boss_kills_tier_") && entry.getValue().isJsonPrimitive()) {
                kills += entry.getValue().getAsInt();
            }
        }
        return kills;
    }

    private JsonObject buildMetadata(JsonObject profile, JsonObject member, JsonObject stats, JsonObject networth, int profileCount) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("profileCount", String.valueOf(profileCount));
        metadata.put("lastSave", formatTimestamp(longValue(member, "last_save", 0L)));
        metadata.put("joined", formatTimestamp(longValue(member, "first_join", 0L)));
        metadata.put("gameMode", stringValue(profile, "game_mode"));
        metadata.put("selected", String.valueOf(booleanValue(profile, "selected")));
        metadata.put("fairySouls", String.valueOf(intValue(stats, "fairySouls", intValue(member, "fairy_souls_collected", 0))));
        metadata.put("apiEnabled", resolveApiEnabled(stats));
        metadata.put("unsoulboundNetworth", formatDecimal(doubleValue(objectValue(networth, "normal"), "unsoulboundNetworth", 0.0D)));

        JsonObject response = new JsonObject();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (!entry.getValue().isBlank()) {
                response.addProperty(entry.getKey(), entry.getValue());
            }
        }
        return response;
    }

    private String resolveApiEnabled(JsonObject stats) {
        JsonObject apiSettings = objectValue(stats, "apiSettings");
        if (apiSettings.entrySet().isEmpty()) {
            return "";
        }
        List<String> enabled = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : apiSettings.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsBoolean()) {
                enabled.add(humanize(entry.getKey()));
            }
        }
        return String.join(", ", enabled);
    }

    private int resolveSkyBlockLevel(JsonObject stats, JsonObject member) {
        return intValue(objectValue(stats, "skyblock_level"), "level", intValue(objectValue(member, "leveling"), "experience", 0));
    }

    private int resolveCatacombsLevel(JsonObject member) {
        double experience = doubleValue(
            objectValue(
                objectValue(
                    objectValue(member, "dungeons"),
                    "dungeon_types"
                ),
                "catacombs"
            ),
            "experience",
            0.0D
        );
        return levelFromExperience(CATACOMBS_XP, experience);
    }

    private double resolveNetworth(JsonObject networth) {
        JsonObject normal = objectValue(networth, "normal");
        return doubleValue(normal, "networth", 0.0D);
    }

    private int levelFromExperience(double[] requirements, double experience) {
        if (experience <= 0.0D) {
            return 0;
        }
        double remaining = experience;
        int level = 0;
        for (int index = 1; index < requirements.length; index++) {
            if (remaining < requirements[index]) {
                break;
            }
            remaining -= requirements[index];
            level = index;
        }
        return level;
    }

    private JsonObject getJson(String url, boolean withApiKey) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
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

    private JsonObject requestOptionalJson(String url) {
        try {
            return getJson(url, false);
        } catch (Exception ignored) {
            return new JsonObject();
        }
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

    private int intValue(JsonObject object, String key, int fallback) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long longValue(JsonObject object, String key, long fallback) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsLong() : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double doubleValue(JsonObject object, String key, double fallback) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsDouble() : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
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

    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0L) {
            return "";
        }
        return Instant.ofEpochMilli(epochMillis).toString();
    }

    private String formatDecimal(double value) {
        if (value <= 0.0D) {
            return "";
        }
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("§.", "").replace('\u00A0', ' ').trim();
    }

    private String resolveRarity(String lore) {
        String[] lines = lore.split("\\R");
        for (int index = lines.length - 1; index >= 0; index--) {
            Matcher matcher = RARITY_PATTERN.matcher(lines[index].toUpperCase(Locale.ROOT));
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    private String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace(':', ' ').replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (char character : normalized.toCharArray()) {
            if (Character.isWhitespace(character)) {
                capitalize = true;
                builder.append(character);
            } else if (capitalize) {
                builder.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }
}
