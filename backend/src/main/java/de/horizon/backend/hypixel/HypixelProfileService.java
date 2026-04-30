package de.horizon.backend.hypixel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.backend.config.BackendConfig;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.ByteTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongTag;
import net.querz.nbt.tag.ShortTag;
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
import java.util.HashMap;
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
    private static final Pattern PET_TYPE_PATTERN = Pattern.compile("^\\s*([A-Z0-9_]+):\\s*\\{$", Pattern.MULTILINE);
    private static final Pattern PET_HEAD_PATTERN = Pattern.compile("\\bhead:\\s*(?:\\{\\s*default:\\s*)?\"([^\"]+)\"");
    private static final Pattern PET_MAX_LEVEL_PATTERN = Pattern.compile("\\bmaxLevel:\\s*(\\d+)");
    private static final Pattern PET_HATCHING_LEVEL_PATTERN = Pattern.compile("\\bhatching:\\s*\\{.*?\\blevel:\\s*(\\d+)", Pattern.DOTALL);
    private static final Pattern PET_HATCHING_HEAD_PATTERN = Pattern.compile("\\bhatching:\\s*\\{.*?\\bhead:\\s*\"([^\"]+)\"", Pattern.DOTALL);
    private static final Pattern PET_SKIN_ID_PATTERN = Pattern.compile("id:\\s*\"(PET_SKIN_[^\"]+)\"");
    private static final Pattern PET_SKIN_NAME_PATTERN = Pattern.compile("\\bname:\\s*\"([^\"]+)\"");
    private static final Pattern PET_SKIN_ANIMATION_HEAD_PATTERN = Pattern.compile("\\banimation:\\s*\\{.*?\"(/head/[^\"]+)\"", Pattern.DOTALL);
    private static final Pattern PET_SKIN_DIRECT_HEAD_PATTERN = Pattern.compile("\\b(?:texture|head):\\s*\"((?:/head/)?[0-9a-fA-F]{32,})\"");
    private static final Pattern PET_SKIN_PAGE_IMAGE_PATTERN = Pattern.compile("property=\"og:image\" content=\"https?://mc-heads\\.net/head/([0-9a-fA-F]{32,})\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_SKIN_PAGE_NAME_PATTERN = Pattern.compile("\"name\":\"([^\"]+ Skin)\"");
    private static final Pattern PET_SKIN_PAGE_TITLE_PATTERN = Pattern.compile("<title>([^<]+?)\\s+price</title>", Pattern.CASE_INSENSITIVE);
    private static final Duration PET_ITEM_CACHE_TTL = Duration.ofHours(6);
    private static final Map<String, PetDefinition> PET_DEFINITION_OVERRIDES = Map.of(
        "ROSE_DRAGON", new PetDefinition(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI3YzNkZTA3NWEyYmIyMzhlZjUxNDMxMjA2YjEwZDU4NmNiMmE1YjFjYzQxZmU4NTFjYzVmMGIwMmQzNTdjNyJ9fX0=",
            200,
            100,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI0NTg5NmUwN2ZjZDUxMWI2MzJjNDc3YjFhYThhYzU5NDg4Y2M0NGNjMjIzNjI1NDBhNjVlOWUzMTJhNGFmOSJ9fX0="
        ),
        "CROW", new PetDefinition(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzMxZTdlMWM2MTQ2MzliYmM5NWEwZDgxZmY3ZTVmN2Y2MDZkOWIzMzExNWNmMmJmMmE0MGNkY2JiYjUzMTMyNCJ9fX0=",
            100,
            0,
            ""
        ),
        "HEDGEHOG", new PetDefinition(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWY1ZTgzNWMxMTZlOGUyMDBlMmUwNmFhNTkzY2FiOGYxYTlmOGM0MGU3ZjAwNWE5Yzc2ZjEyZTI0ZjRjNjM3MCJ9fX0=",
            100,
            0,
            ""
        ),
        "HERMIT_CRAB", new PetDefinition(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjY2MjlkZmEzZmRmZWYwNDA1NDAyNGUwMTU2ZDVlMTlkYTU0MDFiMTkxMWY1OWI0YmQzOTgyNjg1ZmU1NGMyYyJ9fX0=",
            100,
            0,
            ""
        )
    );
    private static final Map<String, String> PET_SKIN_TEXTURE_OVERRIDES = Map.ofEntries(
        Map.entry("PAUA_SHELL", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjFjMjBlYThlODkwY2I3NjUyYjI0ZGQwYmVlYTZkNThkYTFhMzg3OGI5YTM2ZDIwYjgyOTgxNDhkMjA5MGEyYyJ9fX0="),
        Map.entry("HERMIT_CRAB_PAUA_SHELL", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjFjMjBlYThlODkwY2I3NjUyYjI0ZGQwYmVlYTZkNThkYTFhMzg3OGI5YTM2ZDIwYjgyOTgxNDhkMjA5MGEyYyJ9fX0="),
        Map.entry("PINEAPPLE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Y1YmEzMzc4ZmMxZDIyZWYxMTVlNjBkMGM5MWJiOTFiOTNiYmFlYjMwOWEyZjZiMThmMzZmZGNhOGM1Y2JhNiJ9fX0="),
        Map.entry("HEDGEHOG_PINEAPPLE", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Y1YmEzMzc4ZmMxZDIyZWYxMTVlNjBkMGM5MWJiOTFiOTNiYmFlYjMwOWEyZjZiMThmMzZmZGNhOGM1Y2JhNiJ9fX0="),
        Map.entry("RAMBUTAN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDY0NDhhZTczYTRkYzk3ZmM0MjIzMjI0NzA2Zjk3NTNhOTEyYjc4NWU4MjBmNTFkMDlhY2UyZTY4NWFhZTBlIn19fQ=="),
        Map.entry("HEDGEHOG_RAMBUTAN", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDY0NDhhZTczYTRkYzk3ZmM0MjIzMjI0NzA2Zjk3NTNhOTEyYjc4NWU4MjBmNTFkMDlhY2UyZTY4NWFhZTBlIn19fQ==")
    );
    private static final int[] PET_LEVELS = {
        100, 110, 120, 130, 145, 160, 175, 190, 210, 230, 250, 275, 300, 330, 360, 400, 440, 490, 540, 600,
        660, 730, 800, 880, 960, 1050, 1150, 1260, 1380, 1510, 1650, 1800, 1960, 2130, 2310, 2500, 2700, 2920,
        3160, 3420, 3700, 4000, 4350, 4750, 5200, 5700, 6300, 7000, 7800, 8700, 9700, 10800, 12000, 13300, 14700,
        16200, 17800, 19500, 21300, 23200, 25200, 27400, 29800, 32400, 35200, 38200, 41400, 44800, 48400, 52200,
        56200, 60400, 64800, 69400, 74200, 79200, 84700, 90700, 97200, 104200, 111700, 119700, 128200, 137200,
        146700, 156700, 167700, 179700, 192700, 206700, 221700, 237700, 254700, 272700, 291700, 311700, 333700,
        357700, 383700, 411700, 441700, 476700, 516700, 561700, 611700, 666700, 726700, 791700, 861700, 936700,
        1016700, 1101700, 1191700, 1286700, 1386700, 1496700, 1616700, 1746700, 1886700, 0, 5555,
        1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700,
        1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700,
        1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700,
        1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700,
        1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700,
        1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700,
        1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700,
        1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700, 1886700
    };
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
    private Instant petItemCacheUpdatedAt = Instant.EPOCH;
    private Map<String, JsonObject> petItemCache = Map.of();
    private Instant petVisualCacheUpdatedAt = Instant.EPOCH;
    private Map<String, PetDefinition> petVisualCache = Map.of();
    private Instant petSkinCacheUpdatedAt = Instant.EPOCH;
    private Map<String, PetSkinDefinition> petSkinCache = Map.of();
    private Instant petSkinPageCacheUpdatedAt = Instant.EPOCH;
    private Map<String, PetSkinDefinition> petSkinPageCache = new HashMap<>();

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
        JsonObject mojangSkinProfile = requestOptionalJson("https://sessionserver.mojang.com/session/minecraft/profile/" + rawUuid + "?unsigned=false");
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
        profile.addProperty("playerSkinTexture", resolvePlayerSkinTexture(mojangSkinProfile));
        profile.addProperty("playerSkinTextureSignature", resolvePlayerSkinSignature(mojangSkinProfile));
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
        profile.add("accessoryStorage", parseAccessoryStorage(member));
        profile.add("pets", parsePets(member, storagePages));
        profile.add("dungeons", parseDungeons(member));
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
        JsonObject inventory = objectValue(member, "inventory");
        JsonObject bagContents = objectValue(inventory, "bag_contents");
        JsonObject sharedInventory = objectValue(member, "shared_inventory");
        addInventoryPage(storages, inventory, member, "inv_contents", "inventory", "Inventory", 9, 4);
        addChunkedInventoryPages(storages, inventory, member, "ender_chest_contents", "ender_chest", "Ender Chest", 9, 6);
        addInventoryPage(storages, inventory, member, "inv_armor", "armor", "Armor", 4, 1);
        addInventoryPage(storages, inventory, member, "equipment_contents", "equipment", "Equipment", 4, 1);
        addWardrobePages(storages, inventory, member, "wardrobe_contents");
        addInventoryPage(storages, inventory, member, "personal_vault_contents", "personal_vault", "Personal Vault", 9, 6);
        addInventoryPage(storages, inventory, member, "talisman_bag", "talisman_bag", "Accessory Bag", 9, 6);
        addInventoryPage(storages, inventory, member, "accessory_bag_storage", "accessory_storage", "Accessory Storage", 9, 6);
        addInventoryPage(storages, bagContents, inventory, "fishing_bag", "fishing_bag", "Fishing Bag", 9, 6);
        addInventoryPage(storages, bagContents, inventory, "quiver", "quiver", "Quiver", 9, 5);
        addInventoryPage(storages, bagContents, inventory, "potion_bag", "potion_bag", "Potion Bag", 9, 6);
        addInventoryPage(storages, bagContents, inventory, "sacks_bag", "sacks_bag", "Sack of Sacks", 9, 6);
        addInventoryPage(storages, inventory, sharedInventory, "candy_inventory_contents", "candy_inventory", "Candy Inventory", 9, 6);
        addInventoryPage(storages, sharedInventory, member, "carnival_mask_inventory_contents", "carnival_mask", "Carnival Mask Bag", 9, 6);
        Map<Integer, JsonObject> backpackButtons = parsePagedInventoryButtons(firstObjectValue(inventory, member, "backpack_icons"));
        addPagedInventories(storages, firstObjectValue(inventory, member, "backpack_contents"), "backpack", "Backpack", backpackButtons);
        addPagedInventories(storages, firstObjectValue(member, inventory, "storage_contents"), "storage", "Storage", Map.of());
        addPagedInventories(storages, firstObjectValue(sharedInventory, member, "storage_contents"), "storage", "Storage", Map.of());
        return storages;
    }

    private void addWardrobePages(JsonArray storages, JsonObject primary, JsonObject fallback, String sourceKey) {
        String encoded = encodedInventory(primary, fallback, sourceKey);
        JsonObject page = decodeInventoryPage("wardrobe", "Wardrobe", encoded, 9, 4);
        if (page == null) {
            return;
        }
        JsonArray slots = arrayValue(page, "slots");
        for (int pageIndex = 0; pageIndex < 3; pageIndex++) {
            int startIndex = pageIndex * 36;
            int endIndex = startIndex + 36;
            JsonArray chunk = new JsonArray();
            for (JsonElement element : slots) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject slot = element.getAsJsonObject();
                int rawIndex = intValue(slot, "index", -1);
                if (rawIndex < startIndex || rawIndex >= endIndex) {
                    continue;
                }
                JsonObject normalized = slot.deepCopy();
                normalized.addProperty("index", rawIndex - startIndex);
                normalized.addProperty("x", (rawIndex - startIndex) % 9);
                normalized.addProperty("y", (rawIndex - startIndex) / 9);
                chunk.add(normalized);
            }
            JsonObject wardrobePage = new JsonObject();
            wardrobePage.addProperty("id", "wardrobe_" + (pageIndex + 1));
            wardrobePage.addProperty("title", "Wardrobe " + (pageIndex + 1));
            wardrobePage.addProperty("columns", 9);
            wardrobePage.addProperty("rows", 4);
            wardrobePage.add("buttonItem", defaultButtonItem("wardrobe", "Wardrobe " + (pageIndex + 1)));
            wardrobePage.add("slots", chunk);
            storages.add(wardrobePage);
        }
    }

    private void addInventoryPage(JsonArray storages, JsonObject primary, JsonObject fallback, String sourceKey, String id, String title, int columns, int rows) {
        String encoded = encodedInventory(primary, fallback, sourceKey);
        JsonObject page = decodeInventoryPage(id, title, encoded, columns, rows);
        if (page != null) {
            page.add("buttonItem", defaultButtonItem(id, title));
            storages.add(page);
        }
    }

    private void addChunkedInventoryPages(JsonArray storages, JsonObject primary, JsonObject fallback, String sourceKey, String idPrefix, String titlePrefix, int columns, int rowsPerPage) {
        String encoded = encodedInventory(primary, fallback, sourceKey);
        JsonObject page = decodeInventoryPage(idPrefix, titlePrefix, encoded, columns, 0);
        if (page == null) {
            return;
        }
        JsonArray slots = arrayValue(page, "slots");
        int pageSize = Math.max(1, columns * rowsPerPage);
        int maxIndex = slots.asList().stream()
            .filter(JsonElement::isJsonObject)
            .map(JsonElement::getAsJsonObject)
            .mapToInt(slot -> intValue(slot, "index", -1))
            .max()
            .orElse(-1);
        int pageCount = Math.max(1, ((maxIndex + 1) + pageSize - 1) / pageSize);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int startIndex = pageIndex * pageSize;
            int endIndex = startIndex + pageSize;
            JsonArray chunk = new JsonArray();
            for (JsonElement element : slots) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject slot = element.getAsJsonObject();
                int rawIndex = intValue(slot, "index", -1);
                if (rawIndex < startIndex || rawIndex >= endIndex) {
                    continue;
                }
                JsonObject normalized = slot.deepCopy();
                normalized.addProperty("index", rawIndex - startIndex);
                normalized.addProperty("x", (rawIndex - startIndex) % columns);
                normalized.addProperty("y", (rawIndex - startIndex) / columns);
                chunk.add(normalized);
            }
            String pageTitle = pageCount == 1 ? titlePrefix : titlePrefix + " " + (pageIndex + 1);
            String pageId = pageCount == 1 ? idPrefix : idPrefix + "_" + (pageIndex + 1);
            JsonObject chunkedPage = new JsonObject();
            chunkedPage.addProperty("id", pageId);
            chunkedPage.addProperty("title", pageTitle);
            chunkedPage.addProperty("columns", columns);
            chunkedPage.addProperty("rows", rowsPerPage);
            chunkedPage.add("buttonItem", defaultButtonItem(idPrefix, pageTitle));
            chunkedPage.add("slots", chunk);
            storages.add(chunkedPage);
        }
    }

    private void addPagedInventories(JsonArray storages, JsonObject pages, String idPrefix, String titlePrefix, Map<Integer, JsonObject> buttonItems) {
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
                page.add("buttonItem", buttonItems.getOrDefault(pageIndex, defaultButtonItem(idPrefix, titlePrefix + " " + pageIndex)));
                storages.add(page);
            }
        }
    }

    private Map<Integer, JsonObject> parsePagedInventoryButtons(JsonObject pages) {
        Map<Integer, JsonObject> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : pages.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject pageObject = entry.getValue().getAsJsonObject();
            String encoded = stringValue(pageObject, "data");
            if (encoded.isBlank()) {
                continue;
            }
            JsonObject page = decodeInventoryPage("button_" + entry.getKey(), "Button", encoded, 9, 6);
            if (page == null) {
                continue;
            }
            JsonObject slot = firstObject(arrayValue(page, "slots"));
            JsonObject item = objectValue(slot, "item");
            if (!item.entrySet().isEmpty()) {
                values.put(parsePageIndex(entry.getKey()) + 1, item);
            }
        }
        return Map.copyOf(values);
    }

    private int parsePageIndex(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE / 2;
        }
    }

    private JsonObject firstObjectValue(JsonObject first, JsonObject second, String... keys) {
        for (String key : keys) {
            JsonObject primary = objectValue(first, key);
            if (!primary.entrySet().isEmpty()) {
                return primary;
            }
            JsonObject fallback = objectValue(second, key);
            if (!fallback.entrySet().isEmpty()) {
                return fallback;
            }
        }
        return new JsonObject();
    }

    private String encodedInventory(JsonObject primary, JsonObject fallback, String key) {
        String encoded = encodedInventory(primary, key);
        if (!encoded.isBlank()) {
            return encoded;
        }
        return encodedInventory(fallback, key);
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
            int sequentialSlot = 0;
            for (Tag<?> entry : items) {
                if (!(entry instanceof CompoundTag itemTag)) {
                    continue;
                }
                int slot = resolveSlotIndex(itemTag, sequentialSlot++);
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
        page.add("buttonItem", defaultButtonItem(id, title));
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

    private int resolveSlotIndex(CompoundTag itemTag, int fallback) {
        if (itemTag.containsKey("Slot")) {
            return Byte.toUnsignedInt(itemTag.getByte("Slot").orElse((byte) fallback));
        }
        if (itemTag.containsKey("slot")) {
            return Byte.toUnsignedInt(itemTag.getByte("slot").orElse((byte) fallback));
        }
        if (itemTag.containsKey("index")) {
            return itemTag.getInt("index").orElse(fallback);
        }
        return fallback;
    }

    private JsonObject parseSlot(CompoundTag itemTag, int slot, int columns) {
        if (itemTag == null || itemTag.entrySet().isEmpty()) {
            return null;
        }
        CompoundTag tag = itemTag.getCompoundTag("tag");
        CompoundTag display = tag == null ? null : tag.getCompoundTag("display");
        String displayName = display == null ? "" : display.getString("Name").orElse("");
        String lore = loreText(display);
        String itemId = resolveItemId(itemTag, tag);
        JsonObject resourceItem = resolveItemResource(itemId);
        String resourceLore = resourceLore(resourceItem);
        String minecraftItemId = resolveMinecraftItemId(itemTag, tag, itemId, resourceItem);
        if (lore.isBlank()) {
            lore = resourceLore;
        }
        int count = Byte.toUnsignedInt(itemTag.getByte("Count").orElse((byte) 0));
        boolean enchanted = tag != null && tag.containsKey("ench");
        String iconTexture = resolveHeadTexture(tag);
        if (iconTexture.isBlank()) {
            iconTexture = stringValue(resourceItem, "iconTexture");
        }
        String iconTextureSignature = resolveHeadTextureSignature(tag);
        if (iconTextureSignature.isBlank()) {
            iconTextureSignature = stringValue(resourceItem, "iconTextureSignature");
        }

        if (displayName.isBlank() && itemId.isBlank() && minecraftItemId.isBlank()) {
            return null;
        }

        JsonObject item = new JsonObject();
        item.addProperty("itemId", itemId);
        item.addProperty("minecraftItemId", minecraftItemId);
        item.addProperty("displayName", displayName.isBlank() ? fallbackItemDisplayName(itemId, minecraftItemId, resourceItem) : displayName);
        item.addProperty("rarity", resolveRarity(lore));
        item.addProperty("count", Math.max(1, count));
        item.addProperty("lore", lore);
        item.addProperty("iconTexture", iconTexture);
        item.addProperty("iconTextureSignature", iconTextureSignature);
        item.addProperty("leatherColor", resolveLeatherColor(display));
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
        return stringTagValue(itemTag, "id");
    }

    private String resolveMinecraftItemId(CompoundTag itemTag, CompoundTag tag, String itemId, JsonObject resourceItem) {
        String resourceMinecraftId = stringValue(resourceItem, "minecraftItemId");
        String namespacedId = stringTagValue(itemTag, "id");
        if (!namespacedId.isBlank()) {
            String normalized = normalizeMinecraftItemId(namespacedId, itemTag.getShort("Damage").orElse((short) 0));
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        short legacyId = numericTagValue(itemTag, "id", (short) 0);
        short damage = itemTag.getShort("Damage").orElse((short) 0);
        String mapped = legacyMinecraftId(legacyId, damage);
        if (!mapped.isBlank()) {
            return mapped;
        }
        if (!resourceMinecraftId.isBlank()) {
            return resourceMinecraftId;
        }
        String internalId = itemId == null || itemId.isBlank() ? resolveItemId(itemTag, tag) : itemId;
        String legacyInternalId = legacyStringMinecraftId(internalId, damage);
        if (!legacyInternalId.isBlank()) {
            return legacyInternalId;
        }
        return internalId.isBlank() ? "" : heuristicMinecraftId(internalId);
    }

    private JsonObject resolveItemResource(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return new JsonObject();
        }
        JsonObject resourceItem = petItemLookup().get(itemId);
        return resourceItem == null ? new JsonObject() : resourceItem;
    }

    private String fallbackItemDisplayName(String itemId, String minecraftItemId, JsonObject resourceItem) {
        String resourceName = stringValue(resourceItem, "name");
        if (!resourceName.isBlank()) {
            return resourceName;
        }
        if (itemId != null && !itemId.isBlank()) {
            return humanize(itemId);
        }
        if (minecraftItemId == null || minecraftItemId.isBlank()) {
            return "";
        }
        String normalized = minecraftItemId.startsWith("minecraft:") ? minecraftItemId.substring("minecraft:".length()) : minecraftItemId;
        return humanize(normalized);
    }

    private String resourceLore(JsonObject resourceItem) {
        String description = stringValue(resourceItem, "description");
        if (description == null || description.isBlank()) {
            return "";
        }
        return description
            .replace("%%gray%%", "§7")
            .replace("%%white%%", "§f")
            .replace("%%green%%", "§a")
            .replace("%%red%%", "§c")
            .replace("%%yellow%%", "§e")
            .replace("%%blue%%", "§9")
            .replace("%%gold%%", "§6")
            .replace("%%pink%%", "§d");
    }

    private String normalizeMinecraftItemId(String id, short damage) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String legacyNormalized = legacyStringMinecraftId(id, damage);
        if (!legacyNormalized.isBlank()) {
            return legacyNormalized;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "minecraft:skull", "minecraft:skull_item" ->
                damage == 1 ? "minecraft:wither_skeleton_skull" : damage == 2 ? "minecraft:zombie_head" : damage == 4 ? "minecraft:creeper_head" : "minecraft:player_head";
            case "minecraft:wool" -> switch (damage) {
                case 7 -> "minecraft:gray_wool";
                case 15 -> "minecraft:black_wool";
                default -> "minecraft:white_wool";
            };
            case "minecraft:stained_glass_pane" -> "minecraft:white_stained_glass_pane";
            case "minecraft:stained_glass" -> "minecraft:white_stained_glass";
            default -> lower;
        };
    }

    private String legacyStringMinecraftId(String id, short damage) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String trimmed = id.trim();
        if (trimmed.contains(":") && !trimmed.startsWith("minecraft:")) {
            String[] parts = trimmed.split(":", 2);
            short parsedDamage = damage;
            try {
                parsedDamage = Short.parseShort(parts[1]);
            } catch (NumberFormatException ignored) {
                parsedDamage = damage;
            }
            return switch (parts[0].toUpperCase(Locale.ROOT)) {
                case "MONSTER_EGG" -> legacySpawnEggMinecraftId(parsedDamage);
                case "SKULL_ITEM" -> normalizeMinecraftItemId("minecraft:skull_item", parsedDamage);
                case "BANNER" -> bannerMinecraftId(parsedDamage);
                default -> "";
            };
        }
        return "";
    }

    private String legacySpawnEggMinecraftId(short damage) {
        return switch (damage) {
            case 50 -> "minecraft:creeper_spawn_egg";
            case 51 -> "minecraft:skeleton_spawn_egg";
            case 52 -> "minecraft:spider_spawn_egg";
            case 54 -> "minecraft:zombie_spawn_egg";
            case 55 -> "minecraft:slime_spawn_egg";
            case 57 -> "minecraft:zombie_pigman_spawn_egg";
            case 58 -> "minecraft:enderman_spawn_egg";
            case 59 -> "minecraft:cave_spider_spawn_egg";
            case 60 -> "minecraft:silverfish_spawn_egg";
            case 61 -> "minecraft:blaze_spawn_egg";
            case 62 -> "minecraft:magma_cube_spawn_egg";
            case 65 -> "minecraft:bat_spawn_egg";
            case 66 -> "minecraft:witch_spawn_egg";
            case 90 -> "minecraft:pig_spawn_egg";
            case 91 -> "minecraft:sheep_spawn_egg";
            case 92 -> "minecraft:cow_spawn_egg";
            case 93 -> "minecraft:chicken_spawn_egg";
            case 94 -> "minecraft:squid_spawn_egg";
            case 95 -> "minecraft:wolf_spawn_egg";
            case 96 -> "minecraft:mooshroom_spawn_egg";
            case 98 -> "minecraft:ocelot_spawn_egg";
            case 100 -> "minecraft:horse_spawn_egg";
            case 101 -> "minecraft:rabbit_spawn_egg";
            case 120 -> "minecraft:villager_spawn_egg";
            default -> "minecraft:pig_spawn_egg";
        };
    }

    private String bannerMinecraftId(short damage) {
        return switch (damage) {
            case 15 -> "minecraft:black_banner";
            case 14 -> "minecraft:red_banner";
            case 11 -> "minecraft:blue_banner";
            case 10 -> "minecraft:purple_banner";
            case 5 -> "minecraft:lime_banner";
            default -> "minecraft:white_banner";
        };
    }

    private String legacyMinecraftId(short legacyId, short damage) {
        return switch (legacyId) {
            case 1 -> "minecraft:stone";
            case 2 -> "minecraft:grass_block";
            case 3 -> "minecraft:dirt";
            case 4 -> "minecraft:cobblestone";
            case 5 -> "minecraft:oak_planks";
            case 6 -> "minecraft:oak_sapling";
            case 12 -> "minecraft:sand";
            case 13 -> "minecraft:gravel";
            case 17 -> "minecraft:oak_log";
            case 20 -> "minecraft:glass";
            case 22 -> "minecraft:lapis_block";
            case 24 -> "minecraft:sandstone";
            case 30 -> "minecraft:cobweb";
            case 35 -> "minecraft:white_wool";
            case 41 -> "minecraft:gold_block";
            case 42 -> "minecraft:iron_block";
            case 45 -> "minecraft:bricks";
            case 46 -> "minecraft:tnt";
            case 48 -> "minecraft:mossy_cobblestone";
            case 49 -> "minecraft:obsidian";
            case 50 -> "minecraft:torch";
            case 54 -> "minecraft:chest";
            case 56 -> "minecraft:diamond_ore";
            case 57 -> "minecraft:diamond_block";
            case 58 -> "minecraft:crafting_table";
            case 79 -> "minecraft:ice";
            case 80 -> "minecraft:snow_block";
            case 82 -> "minecraft:clay";
            case 84 -> "minecraft:jukebox";
            case 89 -> "minecraft:glowstone";
            case 95 -> "minecraft:white_stained_glass";
            case 98 -> "minecraft:stone_bricks";
            case 101 -> "minecraft:iron_bars";
            case 103 -> "minecraft:melon";
            case 112 -> "minecraft:nether_bricks";
            case 121 -> "minecraft:end_stone";
            case 122 -> "minecraft:dragon_egg";
            case 123 -> "minecraft:redstone_lamp";
            case 129 -> "minecraft:emerald_ore";
            case 130 -> "minecraft:ender_chest";
            case 133 -> "minecraft:emerald_block";
            case 137 -> "minecraft:command_block";
            case 138 -> "minecraft:beacon";
            case 145 -> "minecraft:anvil";
            case 146 -> "minecraft:trapped_chest";
            case 152 -> "minecraft:redstone_block";
            case 154 -> "minecraft:hopper";
            case 160 -> "minecraft:white_stained_glass_pane";
            case 165 -> "minecraft:slime_block";
            case 166 -> "minecraft:barrier";
            case 170 -> "minecraft:hay_block";
            case 173 -> "minecraft:coal_block";
            case 256 -> "minecraft:iron_shovel";
            case 257 -> "minecraft:iron_pickaxe";
            case 258 -> "minecraft:iron_axe";
            case 261 -> "minecraft:bow";
            case 262 -> "minecraft:arrow";
            case 263 -> damage == 1 ? "minecraft:charcoal" : "minecraft:coal";
            case 264 -> "minecraft:diamond";
            case 265 -> "minecraft:iron_ingot";
            case 266 -> "minecraft:gold_ingot";
            case 267 -> "minecraft:iron_sword";
            case 268 -> "minecraft:wooden_sword";
            case 269 -> "minecraft:wooden_shovel";
            case 270 -> "minecraft:wooden_pickaxe";
            case 271 -> "minecraft:wooden_axe";
            case 272 -> "minecraft:stone_sword";
            case 273 -> "minecraft:stone_shovel";
            case 274 -> "minecraft:stone_pickaxe";
            case 275 -> "minecraft:stone_axe";
            case 276 -> "minecraft:diamond_sword";
            case 277 -> "minecraft:diamond_shovel";
            case 278 -> "minecraft:diamond_pickaxe";
            case 279 -> "minecraft:diamond_axe";
            case 280 -> "minecraft:stick";
            case 281 -> "minecraft:bowl";
            case 282 -> "minecraft:mushroom_stew";
            case 283 -> "minecraft:golden_sword";
            case 284 -> "minecraft:golden_shovel";
            case 285 -> "minecraft:golden_pickaxe";
            case 286 -> "minecraft:golden_axe";
            case 290 -> "minecraft:wooden_hoe";
            case 291 -> "minecraft:stone_hoe";
            case 292 -> "minecraft:iron_hoe";
            case 293 -> "minecraft:diamond_hoe";
            case 294 -> "minecraft:golden_hoe";
            case 297 -> "minecraft:bread";
            case 298 -> "minecraft:leather_helmet";
            case 299 -> "minecraft:leather_chestplate";
            case 300 -> "minecraft:leather_leggings";
            case 301 -> "minecraft:leather_boots";
            case 302 -> "minecraft:chainmail_helmet";
            case 303 -> "minecraft:chainmail_chestplate";
            case 304 -> "minecraft:chainmail_leggings";
            case 305 -> "minecraft:chainmail_boots";
            case 306 -> "minecraft:iron_helmet";
            case 307 -> "minecraft:iron_chestplate";
            case 308 -> "minecraft:iron_leggings";
            case 309 -> "minecraft:iron_boots";
            case 310 -> "minecraft:diamond_helmet";
            case 311 -> "minecraft:diamond_chestplate";
            case 312 -> "minecraft:diamond_leggings";
            case 313 -> "minecraft:diamond_boots";
            case 314 -> "minecraft:golden_helmet";
            case 315 -> "minecraft:golden_chestplate";
            case 316 -> "minecraft:golden_leggings";
            case 317 -> "minecraft:golden_boots";
            case 318 -> "minecraft:flint";
            case 319 -> "minecraft:porkchop";
            case 320 -> "minecraft:cooked_porkchop";
            case 322 -> "minecraft:golden_apple";
            case 325 -> "minecraft:bucket";
            case 331 -> "minecraft:redstone";
            case 332 -> "minecraft:snowball";
            case 334 -> "minecraft:leather";
            case 336 -> "minecraft:brick";
            case 337 -> "minecraft:clay_ball";
            case 338 -> "minecraft:sugar_cane";
            case 339 -> "minecraft:paper";
            case 340 -> "minecraft:book";
            case 341 -> "minecraft:slime_ball";
            case 345 -> "minecraft:compass";
            case 346 -> "minecraft:fishing_rod";
            case 347 -> "minecraft:clock";
            case 348 -> "minecraft:glowstone_dust";
            case 349 -> "minecraft:cod";
            case 351 -> switch (damage) {
                case 3 -> "minecraft:cocoa_beans";
                case 4 -> "minecraft:lapis_lazuli";
                case 15 -> "minecraft:bone_meal";
                default -> "minecraft:ink_sac";
            };
            case 352 -> "minecraft:bone";
            case 353 -> "minecraft:sugar";
            case 355 -> "minecraft:bed";
            case 357 -> "minecraft:cookie";
            case 360 -> "minecraft:melon_slice";
            case 361 -> "minecraft:pumpkin_seeds";
            case 362 -> "minecraft:melon_seeds";
            case 368 -> "minecraft:ender_pearl";
            case 369 -> "minecraft:blaze_rod";
            case 370 -> "minecraft:ghast_tear";
            case 371 -> "minecraft:gold_nugget";
            case 372 -> "minecraft:nether_wart";
            case 373 -> "minecraft:potion";
            case 378 -> "minecraft:magma_cream";
            case 379 -> "minecraft:brewing_stand";
            case 380 -> "minecraft:cauldron";
            case 381 -> "minecraft:ender_eye";
            case 384 -> "minecraft:experience_bottle";
            case 388 -> "minecraft:emerald";
            case 391 -> "minecraft:carrot";
            case 392 -> "minecraft:potato";
            case 397 -> damage == 1 ? "minecraft:wither_skeleton_skull" : damage == 2 ? "minecraft:zombie_head" : damage == 4 ? "minecraft:creeper_head" : "minecraft:player_head";
            case 399 -> "minecraft:nether_star";
            case 400 -> "minecraft:pumpkin_pie";
            case 403 -> "minecraft:enchanted_book";
            case 417 -> "minecraft:iron_horse_armor";
            default -> "";
        };
    }

    private String heuristicMinecraftId(String internalId) {
        if (internalId == null || internalId.isBlank()) {
            return "";
        }
        String upper = internalId.toUpperCase(Locale.ROOT);
        if (upper.contains("SWORD")) return "minecraft:diamond_sword";
        if (upper.contains("SHORTBOW") || upper.contains("BOW")) return "minecraft:bow";
        if (upper.contains("HELMET")) return "minecraft:diamond_helmet";
        if (upper.contains("CHESTPLATE")) return "minecraft:diamond_chestplate";
        if (upper.contains("LEGGINGS")) return "minecraft:diamond_leggings";
        if (upper.contains("BOOTS")) return "minecraft:diamond_boots";
        if (upper.contains("PICKAXE")) return "minecraft:diamond_pickaxe";
        if (upper.contains("AXE")) return "minecraft:diamond_axe";
        if (upper.contains("SHOVEL")) return "minecraft:diamond_shovel";
        if (upper.contains("HOE")) return "minecraft:diamond_hoe";
        if (upper.contains("DRILL")) return "minecraft:golden_pickaxe";
        if (upper.contains("ROD")) return "minecraft:fishing_rod";
        if (upper.contains("WAND")) return "minecraft:blaze_rod";
        if (upper.contains("GAUNTLET")) return "minecraft:golden_leggings";
        if (upper.contains("BELT")) return "minecraft:lead";
        if (upper.contains("NECKLACE")) return "minecraft:gold_nugget";
        if (upper.contains("CLOAK")) return "minecraft:elytra";
        if (upper.contains("GLOVES")) return "minecraft:leather";
        if (upper.contains("RING") || upper.contains("ARTIFACT") || upper.contains("RELIC") || upper.contains("TALISMAN")) return "minecraft:player_head";
        if (upper.contains("POTION")) return "minecraft:potion";
        if (upper.contains("BOOK")) return "minecraft:book";
        if (upper.contains("BACKPACK")) return "minecraft:player_head";
        if (upper.contains("PET_SKIN")) return "minecraft:player_head";
        if (upper.contains("DRAWING")) return "minecraft:map";
        return "minecraft:paper";
    }

    private String stringTagValue(CompoundTag tag, String key) {
        if (tag == null || key == null || !tag.containsKey(key)) {
            return "";
        }
        Tag<?> value = tag.get(key);
        return value instanceof StringTag stringTag ? stringTag.getValue() : "";
    }

    private short numericTagValue(CompoundTag tag, String key, short fallback) {
        if (tag == null || key == null || !tag.containsKey(key)) {
            return fallback;
        }
        Tag<?> value = tag.get(key);
        if (value instanceof ShortTag shortTag) {
            return shortTag.asShort();
        }
        if (value instanceof IntTag intTag) {
            return (short) intTag.asInt();
        }
        if (value instanceof ByteTag byteTag) {
            return (short) Byte.toUnsignedInt(byteTag.asByte());
        }
        if (value instanceof LongTag longTag) {
            return (short) longTag.asLong();
        }
        return fallback;
    }

    private String resolveHeadTexture(CompoundTag tag) {
        if (tag == null) {
            return "";
        }
        CompoundTag skullOwner = tag.getCompoundTag("SkullOwner");
        if (skullOwner == null) {
            return "";
        }
        CompoundTag properties = skullOwner.getCompoundTag("Properties");
        if (properties == null) {
            return "";
        }
        ListTag<?> textures = properties.getListTag("textures");
        if (textures == null || textures.size() == 0 || !(textures.get(0) instanceof CompoundTag textureTag)) {
            return "";
        }
        return textureTag.getString("Value").orElse("");
    }

    private String resolveHeadTextureSignature(CompoundTag tag) {
        if (tag == null) {
            return "";
        }
        CompoundTag skullOwner = tag.getCompoundTag("SkullOwner");
        if (skullOwner == null) {
            return "";
        }
        CompoundTag properties = skullOwner.getCompoundTag("Properties");
        if (properties == null) {
            return "";
        }
        ListTag<?> textures = properties.getListTag("textures");
        if (textures == null || textures.size() == 0 || !(textures.get(0) instanceof CompoundTag textureTag)) {
            return "";
        }
        return textureTag.getString("Signature").orElse("");
    }

    private JsonObject defaultButtonItem(String id, String title) {
        String lowerId = id == null ? "" : id.toLowerCase(Locale.ROOT);
        String minecraftItemId = "minecraft:chest";
        if (lowerId.startsWith("inventory")) {
            minecraftItemId = "minecraft:book";
        } else if (lowerId.startsWith("ender_chest")) {
            minecraftItemId = "minecraft:ender_chest";
        } else if (lowerId.startsWith("personal_vault")) {
            minecraftItemId = "minecraft:gold_block";
        } else if (lowerId.startsWith("armor")) {
            minecraftItemId = "minecraft:diamond_chestplate";
        } else if (lowerId.startsWith("equipment")) {
            minecraftItemId = "minecraft:anvil";
        } else if (lowerId.startsWith("wardrobe")) {
            minecraftItemId = "minecraft:leather_chestplate";
        } else if (lowerId.startsWith("quiver")) {
            minecraftItemId = "minecraft:arrow";
        } else if (lowerId.startsWith("potion")) {
            minecraftItemId = "minecraft:potion";
        } else if (lowerId.startsWith("fishing")) {
            minecraftItemId = "minecraft:fishing_rod";
        } else if (lowerId.startsWith("storage")) {
            minecraftItemId = "minecraft:chest";
        } else if (lowerId.startsWith("backpack")) {
            minecraftItemId = "minecraft:player_head";
        }
        JsonObject item = new JsonObject();
        item.addProperty("itemId", lowerId);
        item.addProperty("minecraftItemId", minecraftItemId);
        item.addProperty("displayName", title == null ? "" : title);
        item.addProperty("rarity", "");
        item.addProperty("count", 1);
        item.addProperty("lore", "");
        item.addProperty("iconTexture", "");
        item.addProperty("enchanted", false);
        return item;
    }

    private JsonObject firstObject(JsonArray values) {
        if (values == null) {
            return new JsonObject();
        }
        for (JsonElement value : values) {
            if (value.isJsonObject()) {
                return value.getAsJsonObject();
            }
        }
        return new JsonObject();
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
                builder.append(stringTag.getValue());
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
            String cleanLine = cleanText(line);
            if (cleanLine.toLowerCase(Locale.ROOT).contains("enrichment")) {
                return cleanLine.trim();
            }
        }
        return "";
    }

    private JsonArray parsePets(JsonObject member, JsonArray storagePages) {
        JsonArray values = new JsonArray();
        Map<String, JsonObject> petItems = petItemLookup();
        Map<String, PetDefinition> petDefinitions = petDefinitions();
        PetInventoryIndex petInventory = parsePetInventoryIndex(storagePages);
        JsonArray pets = arrayValue(objectValue(member, "pets_data"), "pets");
        if (pets.isEmpty()) {
            pets = arrayValue(member, "pets");
        }
        for (JsonElement element : pets) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject pet = element.getAsJsonObject();
            JsonObject parsed = new JsonObject();
            double experience = doubleValue(pet, "exp", 0.0D);
            String tier = stringValue(pet, "tier");
            String type = stringValue(pet, "type");
            String heldItem = stringValue(pet, "heldItem");
            String skin = stringValue(pet, "skin");
            PetInventoryVisual inventoryVisual = resolvePetInventoryVisual(
                petInventory,
                stringValue(pet, "uniqueId"),
                stringValue(pet, "uuid"),
                type,
                tier,
                experience,
                heldItem
            );
            if ((skin == null || skin.isBlank()) && inventoryVisual != null && inventoryVisual.skinId() != null && !inventoryVisual.skinId().isBlank()) {
                skin = inventoryVisual.skinId();
            }
            parsed.addProperty("type", type);
            int level = petLevel(experience, tier, type);
            parsed.addProperty("displayName", buildPetDisplayName(type, level, tier));
            parsed.addProperty("tier", tier);
            parsed.addProperty("level", level);
            parsed.addProperty("experience", experience);
            parsed.addProperty("active", booleanValue(pet, "active"));
            parsed.addProperty("heldItem", heldItem);
            parsed.addProperty("heldItemDisplayName", resolveHeldItemDisplayName(heldItem, petItems));
            parsed.addProperty("candyUsed", intValue(pet, "candyUsed", 0));
            parsed.addProperty("soulbound", booleanValue(pet, "petSoulbound"));
            parsed.addProperty("skin", skin);
            JsonObject visualPet = pet.deepCopy();
            visualPet.addProperty("skin", skin);
            JsonObject icon = resolvePetIcon(visualPet, petItems, petDefinitions, level);
            if (inventoryVisual != null && inventoryVisual.iconTexture() != null && !inventoryVisual.iconTexture().isBlank()) {
                icon = petIcon("minecraft:player_head", inventoryVisual.iconTexture(), inventoryVisual.iconTextureSignature(), inventoryVisual.skinDisplayName());
            }
            String skinDisplayName = inventoryVisual != null && inventoryVisual.skinDisplayName() != null && !inventoryVisual.skinDisplayName().isBlank()
                ? inventoryVisual.skinDisplayName()
                : resolvePetSkinDisplayName(visualPet, icon);
            parsed.addProperty("skinDisplayName", skinDisplayName);
            parsed.addProperty("minecraftItemId", stringValue(icon, "minecraftItemId"));
            parsed.addProperty("iconTexture", stringValue(icon, "iconTexture"));
            parsed.addProperty("iconTextureSignature", stringValue(icon, "iconTextureSignature"));
            values.add(parsed);
        }
        return values;
    }

    private PetInventoryIndex parsePetInventoryIndex(JsonArray storagePages) {
        Map<String, PetInventoryVisual> byUniqueId = new HashMap<>();
        Map<String, PetInventoryVisual> byUuid = new HashMap<>();
        List<PetInventoryVisual> all = new ArrayList<>();
        for (JsonElement pageElement : storagePages) {
            if (!pageElement.isJsonObject()) {
                continue;
            }
            for (JsonElement slotElement : arrayValue(pageElement.getAsJsonObject(), "slots")) {
                if (!slotElement.isJsonObject()) {
                    continue;
                }
                JsonObject item = objectValue(slotElement.getAsJsonObject(), "item");
                if (!"PET".equalsIgnoreCase(stringValue(item, "itemId"))) {
                    continue;
                }
                JsonObject petInfo = parsePetInfoFromLore(stringValue(item, "lore"));
                if (petInfo.entrySet().isEmpty()) {
                    continue;
                }
                PetInventoryVisual visual = new PetInventoryVisual(
                    stringValue(petInfo, "uniqueId"),
                    stringValue(petInfo, "uuid"),
                    stringValue(petInfo, "type"),
                    stringValue(petInfo, "tier"),
                    doubleValue(petInfo, "exp", 0.0D),
                    stringValue(petInfo, "heldItem"),
                    stringValue(petInfo, "skin"),
                    resolvePetSkinDisplayNameFromTooltip(stringValue(item, "lore")),
                    stringValue(item, "iconTexture"),
                    stringValue(item, "iconTextureSignature")
                );
                all.add(visual);
                if (!visual.uniqueId().isBlank()) {
                    byUniqueId.putIfAbsent(visual.uniqueId(), visual);
                }
                if (!visual.uuid().isBlank()) {
                    byUuid.putIfAbsent(visual.uuid(), visual);
                }
            }
        }
        return new PetInventoryIndex(Map.copyOf(byUniqueId), Map.copyOf(byUuid), List.copyOf(all));
    }

    private JsonObject parsePetInfoFromLore(String lore) {
        if (lore == null || lore.isBlank()) {
            return new JsonObject();
        }
        for (String line : lore.split("\\R")) {
            String clean = cleanText(line);
            if (!clean.startsWith("PET INFO:")) {
                continue;
            }
            String json = clean.substring("PET INFO:".length()).trim();
            try {
                JsonElement parsed = JsonParser.parseString(json);
                if (parsed.isJsonObject()) {
                    return parsed.getAsJsonObject();
                }
            } catch (Exception ignored) {
                return new JsonObject();
            }
        }
        return new JsonObject();
    }

    private PetInventoryVisual resolvePetInventoryVisual(PetInventoryIndex index, String uniqueId, String uuid, String type, String tier, double experience, String heldItem) {
        if (index == null) {
            return null;
        }
        if (uniqueId != null && !uniqueId.isBlank()) {
            PetInventoryVisual visual = index.byUniqueId().get(uniqueId);
            if (visual != null) {
                return visual;
            }
        }
        if (uuid != null && !uuid.isBlank()) {
            PetInventoryVisual visual = index.byUuid().get(uuid);
            if (visual != null) {
                return visual;
            }
        }
        for (PetInventoryVisual visual : index.all()) {
            if (!equalsIgnoreCase(visual.type(), type) || !equalsIgnoreCase(visual.tier(), tier)) {
                continue;
            }
            if (!equalsIgnoreCase(visual.heldItem(), heldItem)) {
                continue;
            }
            if (Math.abs(visual.experience() - experience) <= 1.0D) {
                return visual;
            }
        }
        return null;
    }

    private String resolvePetSkinDisplayNameFromTooltip(String lore) {
        if (lore == null || lore.isBlank()) {
            return "";
        }
        for (String line : lore.split("\\R")) {
            String clean = cleanText(line);
            if (!clean.endsWith(" Skin")) {
                continue;
            }
            int separator = clean.lastIndexOf(", ");
            if (separator >= 0) {
                return clean.substring(separator + 2, clean.length() - " Skin".length()).trim();
            }
            return clean.substring(0, clean.length() - " Skin".length()).trim();
        }
        return "";
    }

    private boolean equalsIgnoreCase(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
    }

    private JsonObject parseAccessoryStorage(JsonObject member) {
        JsonObject storage = objectValue(member, "accessory_bag_storage");
        JsonObject response = new JsonObject();
        response.addProperty("selectedPower", stringValue(storage, "selected_power"));
        response.addProperty("highestMagicalPower", intValue(storage, "highest_magical_power", 0));
        response.addProperty("bagUpgradesPurchased", intValue(storage, "bag_upgrades_purchased", 0));
        JsonArray unlockedPowers = new JsonArray();
        for (JsonElement element : arrayValue(storage, "unlocked_powers")) {
            if (element.isJsonPrimitive()) {
                unlockedPowers.add(element.getAsString());
            }
        }
        response.add("unlockedPowers", unlockedPowers);
        JsonObject tuning = new JsonObject();
        JsonObject rawTuning = objectValue(storage, "tuning");
        for (Map.Entry<String, JsonElement> entry : rawTuning.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                tuning.addProperty(entry.getKey(), entry.getValue().getAsString());
            }
        }
        response.add("tuning", tuning);
        return response;
    }

    private JsonObject parseDungeons(JsonObject member) {
        JsonObject response = new JsonObject();
        JsonObject dungeons = objectValue(member, "dungeons");
        response.addProperty("selectedClass", stringValue(dungeons, "selected_dungeon_class"));
        response.addProperty("secrets", intValue(dungeons, "secrets", 0));

        JsonArray classes = new JsonArray();
        JsonObject playerClasses = objectValue(dungeons, "player_classes");
        for (Map.Entry<String, JsonElement> entry : playerClasses.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject dungeonClass = entry.getValue().getAsJsonObject();
            double experience = doubleValue(dungeonClass, "experience", 0.0D);
            JsonObject parsed = new JsonObject();
            parsed.addProperty("id", entry.getKey());
            parsed.addProperty("displayName", humanize(entry.getKey()));
            parsed.addProperty("level", levelFromExperience(CATACOMBS_XP, experience));
            parsed.addProperty("experience", experience);
            parsed.addProperty("selected", entry.getKey().equalsIgnoreCase(stringValue(dungeons, "selected_dungeon_class")));
            classes.add(parsed);
        }
        response.add("classes", classes);

        JsonArray floors = new JsonArray();
        JsonObject dungeonTypes = objectValue(dungeons, "dungeon_types");
        addDungeonFloors(floors, objectValue(dungeonTypes, "catacombs"), false);
        addDungeonFloors(floors, objectValue(dungeonTypes, "master_catacombs"), true);
        response.add("floors", floors);
        return response;
    }

    private void addDungeonFloors(JsonArray floors, JsonObject data, boolean masterMode) {
        JsonObject completions = objectValue(data, "tier_completions");
        JsonObject fastestTimes = objectValue(data, masterMode ? "fastest_time_s" : "fastest_time");
        JsonObject fastestSPlusTimes = objectValue(data, "fastest_time_s_plus");
        JsonObject bestScore = objectValue(data, "best_score");
        for (Map.Entry<String, JsonElement> entry : completions.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                continue;
            }
            int floor = parseInt(entry.getKey(), -1);
            if (floor < 0) {
                continue;
            }
            JsonObject parsed = new JsonObject();
            parsed.addProperty("id", (masterMode ? "m" : "f") + floor);
            parsed.addProperty("displayName", masterMode ? "M" + floor : floor == 0 ? "Entrance" : "F" + floor);
            parsed.addProperty("completions", entry.getValue().getAsInt());
            parsed.addProperty("fastestTimeMs", intValue(fastestTimes, entry.getKey(), 0));
            parsed.addProperty("fastestSPlusTimeMs", intValue(fastestSPlusTimes, entry.getKey(), 0));
            parsed.addProperty("bestScore", intValue(bestScore, entry.getKey(), 0));
            floors.add(parsed);
        }
    }

    private Map<String, JsonObject> petItemLookup() {
        if (!petItemCache.isEmpty() && petItemCacheUpdatedAt.plus(PET_ITEM_CACHE_TTL).isAfter(Instant.now())) {
            return petItemCache;
        }
        Map<String, JsonObject> lookup = new HashMap<>();
        try {
            JsonArray items = arrayValue(getJson("https://api.hypixel.net/v2/resources/skyblock/items", true), "items");
            for (JsonElement element : items) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String id = stringValue(item, "id");
                String material = stringValue(item, "material");
                JsonObject skin = objectValue(item, "skin");
                if (id.isBlank()) {
                    continue;
                }
                JsonObject cached = item.deepCopy();
                if (!skin.entrySet().isEmpty()) {
                    cached.addProperty("minecraftItemId", "minecraft:player_head");
                    cached.addProperty("iconTexture", normalizeTextureValue(stringValue(skin, "value")));
                    cached.addProperty("iconTextureSignature", stringValue(skin, "signature"));
                } else {
                    cached.addProperty("minecraftItemId", materialToMinecraftItemId(item));
                    cached.addProperty("iconTexture", "");
                    cached.addProperty("iconTextureSignature", "");
                }
                lookup.put(id, cached);
            }
            petItemCache = Map.copyOf(lookup);
            petItemCacheUpdatedAt = Instant.now();
        } catch (Exception ignored) {
            if (!petItemCache.isEmpty()) {
                return petItemCache;
            }
        }
        return petItemCache;
    }

    private JsonObject resolvePetIcon(JsonObject pet, Map<String, JsonObject> petItems, Map<String, PetDefinition> petDefinitions, int level) {
        String skin = stringValue(pet, "skin");
        if (!skin.isBlank()) {
            for (String candidateId : petSkinCandidates(skin, stringValue(pet, "type"))) {
                JsonObject exactSkin = petItems.get(candidateId);
                if (exactSkin != null && isPetSkinItem(candidateId, exactSkin, skin, stringValue(pet, "type"))) {
                    return exactSkin;
                }
                PetSkinDefinition itemPageSkin = petSkinFromItemPage(candidateId);
                if (itemPageSkin != null && !itemPageSkin.texture().isBlank()) {
                    return petIcon("minecraft:player_head", itemPageSkin.texture(), "", itemPageSkin.name());
                }
                PetSkinDefinition skyCryptSkin = petSkins().get(candidateId);
                if (skyCryptSkin != null && !skyCryptSkin.texture().isBlank()) {
                    return petIcon("minecraft:player_head", skyCryptSkin.texture(), "", skyCryptSkin.name());
                }
            }
            JsonObject overrideSkin = overridePetSkinIcon(skin, stringValue(pet, "type"));
            if (!overrideSkin.entrySet().isEmpty()) {
                return overrideSkin;
            }
        }

        String type = stringValue(pet, "type");
        PetDefinition definition = petDefinitions.get(type);
        if (definition != null) {
            if (definition.hatchingLevel() > 0 && level < definition.hatchingLevel() && !definition.hatchingHeadTexture().isBlank()) {
                return petIcon("minecraft:player_head", normalizeTextureValue(definition.hatchingHeadTexture()), "");
            }
            if (!definition.headTexture().isBlank()) {
                return petIcon("minecraft:player_head", normalizeTextureValue(definition.headTexture()), "");
            }
        }
        return petIcon(fallbackPetMinecraftItemId(type), "", "");
    }

    private List<String> petSkinCandidates(String skin, String type) {
        List<String> candidates = new ArrayList<>();
        if (skin == null || skin.isBlank()) {
            return candidates;
        }
        String normalizedSkin = skin.toUpperCase(Locale.ROOT);
        candidates.add(normalizedSkin);
        String strippedSkin = normalizedSkin.startsWith("PET_SKIN_") ? normalizedSkin.substring("PET_SKIN_".length()) : normalizedSkin;
        candidates.add(strippedSkin);
        if (!normalizedSkin.startsWith("PET_SKIN_")) {
            candidates.add("PET_SKIN_" + strippedSkin);
        }
        for (String alias : petTypeAliases(type)) {
            if (strippedSkin.startsWith(alias + "_")) {
                candidates.add(strippedSkin.substring(alias.length() + 1));
            }
            candidates.add(alias + "_" + strippedSkin);
            candidates.add(strippedSkin + "_" + alias);
            candidates.add("PET_SKIN_" + alias + "_" + strippedSkin);
            candidates.add("PET_SKIN_" + strippedSkin + "_" + alias);
        }
        int separator = strippedSkin.lastIndexOf('_');
        if (separator > 0 && separator + 1 < strippedSkin.length()) {
            candidates.add(strippedSkin.substring(separator + 1));
        }
        return candidates.stream().filter(candidate -> candidate != null && !candidate.isBlank()).distinct().toList();
    }

    private JsonObject overridePetSkinIcon(String skin, String type) {
        for (String candidate : petSkinCandidates(skin, type)) {
            String texture = PET_SKIN_TEXTURE_OVERRIDES.get(candidate);
            if (texture != null && !texture.isBlank()) {
                JsonObject icon = petIcon("minecraft:player_head", normalizeTextureValue(texture), "");
                icon.addProperty("name", humanize(candidate));
                return icon;
            }
        }
        return new JsonObject();
    }

    private boolean isPetSkinItem(String id, JsonObject item, String skin, String type) {
        if (item == null || stringValue(item, "iconTexture").isBlank() || id == null || id.isBlank()) {
            return false;
        }
        String normalizedSkin = normalizePetSkinId(skin);
        String normalizedId = id.toUpperCase(Locale.ROOT);
        boolean directSkinItem = normalizedId.equals(normalizedSkin) || normalizedId.equals("PET_SKIN_" + normalizedSkin);
        if (!directSkinItem) {
            return false;
        }
        for (String alias : petTypeAliases(type)) {
            if (alias == null || alias.isBlank()) {
                continue;
            }
            String normalizedAlias = alias.toUpperCase(Locale.ROOT);
            if (normalizedId.contains(normalizedAlias)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePetSkinId(String skin) {
        if (skin == null || skin.isBlank()) {
            return "";
        }
        String normalized = skin.toUpperCase(Locale.ROOT);
        return normalized.startsWith("PET_SKIN_") ? normalized.substring("PET_SKIN_".length()) : normalized;
    }

    private String resolvePetSkinDisplayName(JsonObject pet, JsonObject icon) {
        String skin = stringValue(pet, "skin");
        if (skin.isBlank()) {
            return "";
        }
        String iconName = stringValue(icon, "name");
        return iconName.isBlank() ? humanize(normalizePetSkinId(skin)) : iconName;
    }

    private Map<String, PetSkinDefinition> petSkins() {
        if (!petSkinCache.isEmpty() && petSkinCacheUpdatedAt.plus(PET_ITEM_CACHE_TTL).isAfter(Instant.now())) {
            return petSkinCache;
        }
        try {
            String source = getText("https://raw.githubusercontent.com/SkyCryptWebsite/SkyCrypt/master/src/constants/skins-animations.js", false);
            Map<String, PetSkinDefinition> parsed = parsePetSkins(source);
            if (!parsed.isEmpty()) {
                petSkinCache = Map.copyOf(parsed);
                petSkinCacheUpdatedAt = Instant.now();
            }
        } catch (Exception ignored) {
            if (!petSkinCache.isEmpty()) {
                return petSkinCache;
            }
        }
        return petSkinCache;
    }

    private PetSkinDefinition petSkinFromItemPage(String candidateId) {
        if (candidateId == null || candidateId.isBlank() || !candidateId.startsWith("PET_SKIN_")) {
            return null;
        }
        if (petSkinPageCacheUpdatedAt.plus(PET_ITEM_CACHE_TTL).isBefore(Instant.now())) {
            petSkinPageCache = new HashMap<>();
            petSkinPageCacheUpdatedAt = Instant.now();
        }
        if (petSkinPageCache.containsKey(candidateId)) {
            return petSkinPageCache.get(candidateId);
        }
        PetSkinDefinition resolved = new PetSkinDefinition("", "");
        try {
            String source = getText("https://sky.coflnet.com/item/" + encode(candidateId), false);
            String textureHash = match(source, PET_SKIN_PAGE_IMAGE_PATTERN);
            if (!textureHash.isBlank()) {
                String name = match(source, PET_SKIN_PAGE_NAME_PATTERN);
                if (name.isBlank()) {
                    name = match(source, PET_SKIN_PAGE_TITLE_PATTERN).replace(" price", "").trim();
                }
                resolved = new PetSkinDefinition(name, encodeHeadTexture(textureHash));
            }
        } catch (Exception ignored) {
            resolved = new PetSkinDefinition("", "");
        }
        petSkinPageCache.put(candidateId, resolved);
        return resolved.texture().isBlank() ? null : resolved;
    }

    private Map<String, PetSkinDefinition> parsePetSkins(String source) {
        if (source == null || source.isBlank()) {
            return Map.of();
        }
        Map<String, PetSkinDefinition> parsed = new LinkedHashMap<>();
        Matcher matcher = PET_SKIN_ID_PATTERN.matcher(source);
        while (matcher.find()) {
            String id = matcher.group(1);
            int entryStart = source.lastIndexOf('{', matcher.start());
            int entryEnd = matchingBrace(source, entryStart);
            if (entryStart < 0 || entryEnd <= entryStart) {
                continue;
            }
            String block = source.substring(entryStart, entryEnd + 1);
            String name = match(block, PET_SKIN_NAME_PATTERN);
            String animatedHead = match(block, PET_SKIN_ANIMATION_HEAD_PATTERN);
            String directHead = match(block, PET_SKIN_DIRECT_HEAD_PATTERN);
            String texture = normalizeTextureValue(!animatedHead.isBlank() ? animatedHead : directHead);
            if (id == null || id.isBlank() || texture.isBlank()) {
                continue;
            }
            parsed.putIfAbsent(id, new PetSkinDefinition(name, texture));
        }
        return parsed.isEmpty() ? Map.of() : Map.copyOf(parsed);
    }

    private List<String> petTypeAliases(String type) {
        List<String> aliases = new ArrayList<>();
        if (type == null || type.isBlank()) {
            return aliases;
        }
        String normalized = type.toUpperCase(Locale.ROOT);
        aliases.add(normalized);
        if (normalized.contains("_DRAGON")) {
            aliases.add("DRAGON");
        }
        int separator = normalized.lastIndexOf('_');
        if (separator > 0 && separator + 1 < normalized.length()) {
            aliases.add(normalized.substring(separator + 1));
        }
        if (normalized.startsWith("ENDER_")) {
            aliases.add(normalized.substring("ENDER_".length()));
        }
        aliases.add(normalized.replace("WITHER_SKELETON", "WITHER"));
        aliases.add(normalized.replace("GRANDMA_WOLF", "WOLF"));
        aliases.add(normalized.replace("ENDER_DRAGON", "DRAGON"));
        aliases.add(normalized.replace("GOLDEN_DRAGON", "DRAGON"));
        return aliases.stream().filter(alias -> alias != null && !alias.isBlank()).distinct().toList();
    }

    private List<String> skinTokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String normalized = value.toUpperCase(Locale.ROOT).replace("PET_SKIN_", "");
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split("[^A-Z0-9]+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String buildPetDisplayName(String type, int level, String tier) {
        return "§7[Lvl " + Math.max(1, level) + "] " + rarityColorCode(tier) + humanize(type);
    }

    private String rarityColorCode(String rarity) {
        return switch (rarity == null ? "" : rarity.toUpperCase(Locale.ROOT)) {
            case "COMMON" -> "§f";
            case "UNCOMMON" -> "§a";
            case "RARE" -> "§9";
            case "EPIC" -> "§5";
            case "LEGENDARY" -> "§6";
            case "MYTHIC" -> "§d";
            case "DIVINE" -> "§b";
            case "SPECIAL", "VERY SPECIAL" -> "§c";
            default -> "§f";
        };
    }

    private JsonObject petIcon(String minecraftItemId, String iconTexture, String iconTextureSignature) {
        return petIcon(minecraftItemId, iconTexture, iconTextureSignature, "");
    }

    private JsonObject petIcon(String minecraftItemId, String iconTexture, String iconTextureSignature, String name) {
        JsonObject icon = new JsonObject();
        icon.addProperty("minecraftItemId", minecraftItemId);
        icon.addProperty("iconTexture", iconTexture == null ? "" : iconTexture);
        icon.addProperty("iconTextureSignature", iconTextureSignature == null ? "" : iconTextureSignature);
        icon.addProperty("name", name == null ? "" : name);
        return icon;
    }

    private String normalizeTextureValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("/head/")) {
            return encodeHeadTexture(trimmed.substring("/head/".length()));
        }
        if (trimmed.matches("^[0-9a-fA-F]{32,}$")) {
            return encodeHeadTexture(trimmed);
        }
        return trimmed;
    }

    private String resolvePlayerSkinTexture(JsonObject skinProfile) {
        JsonObject property = firstProperty(skinProfile, "textures");
        return stringValue(property, "value");
    }

    private String resolvePlayerSkinSignature(JsonObject skinProfile) {
        JsonObject property = firstProperty(skinProfile, "textures");
        return stringValue(property, "signature");
    }

    private JsonObject firstProperty(JsonObject object, String name) {
        for (JsonElement element : arrayValue(object, "properties")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject property = element.getAsJsonObject();
            if (name.equals(stringValue(property, "name"))) {
                return property;
            }
        }
        return new JsonObject();
    }

    private String resolveHeldItemDisplayName(String heldItem, Map<String, JsonObject> petItems) {
        if (heldItem == null || heldItem.isBlank()) {
            return "";
        }
        JsonObject item = petItems.get(heldItem);
        if (item != null) {
            String name = stringValue(item, "name");
            if (!name.isBlank()) {
                return cleanText(name);
            }
        }
        return humanize(heldItem);
    }

    private int petLevel(double experience, String tier, String type) {
        if (experience <= 0.0D) {
            return 1;
        }
        int offset = switch (tier == null ? "" : tier.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> 6;
            case "RARE" -> 11;
            case "EPIC" -> 16;
            case "LEGENDARY", "MYTHIC" -> 20;
            default -> 0;
        };
        int start = Math.min(offset, PET_LEVELS.length - 1);
        int maxLevel = petDefinitions().getOrDefault(type, PetDefinition.DEFAULT).maxLevel();
        int end = Math.min(PET_LEVELS.length, start + Math.max(1, maxLevel));
        double remaining = experience;
        int level = 1;
        for (int index = start; index < end; index++) {
            if (remaining < PET_LEVELS[index]) {
                break;
            }
            remaining -= PET_LEVELS[index];
            level++;
        }
        return Math.min(maxLevel, level);
    }

    private Map<String, PetDefinition> petDefinitions() {
        if (!petVisualCache.isEmpty() && petVisualCacheUpdatedAt.plus(PET_ITEM_CACHE_TTL).isAfter(Instant.now())) {
            return petVisualCache;
        }
        try {
            String source = getText("https://raw.githubusercontent.com/SkyCryptWebsite/SkyCrypt/master/src/constants/pets.js", false);
            Map<String, PetDefinition> parsed = parsePetDefinitions(source);
            Map<String, PetDefinition> merged = new HashMap<>(parsed);
            merged.putAll(PET_DEFINITION_OVERRIDES);
            if (!merged.isEmpty()) {
                petVisualCache = Map.copyOf(merged);
                petVisualCacheUpdatedAt = Instant.now();
            }
        } catch (Exception ignored) {
            // keep stale cache if available
        }
        if (petVisualCache.isEmpty()) {
            petVisualCache = PET_DEFINITION_OVERRIDES;
        }
        return petVisualCache;
    }

    private Map<String, PetDefinition> parsePetDefinitions(String source) {
        if (source == null || source.isBlank()) {
            return Map.of();
        }
        int dataStart = source.indexOf("export const PET_DATA = {");
        if (dataStart < 0) {
            return Map.of();
        }
        int blockStart = source.indexOf('{', dataStart);
        int blockEnd = matchingBrace(source, blockStart);
        if (blockStart < 0 || blockEnd <= blockStart) {
            return Map.of();
        }
        String block = source.substring(blockStart + 1, blockEnd);
        Map<String, PetDefinition> definitions = new HashMap<>();
        Matcher matcher = PET_TYPE_PATTERN.matcher(block);
        while (matcher.find()) {
            String type = matcher.group(1);
            int entryStart = matcher.end() - 1;
            int entryEnd = matchingBrace(block, entryStart);
            if (entryEnd <= entryStart) {
                continue;
            }
            String entry = block.substring(entryStart + 1, entryEnd);
            String head = match(entry, PET_HEAD_PATTERN);
            int maxLevel = parseInt(match(entry, PET_MAX_LEVEL_PATTERN), 100);
            int hatchingLevel = parseInt(match(entry, PET_HATCHING_LEVEL_PATTERN), 0);
            String hatchingHead = match(entry, PET_HATCHING_HEAD_PATTERN);
            definitions.put(type, new PetDefinition(
                encodeHeadTexture(head),
                Math.max(1, maxLevel),
                hatchingLevel,
                encodeHeadTexture(hatchingHead)
            ));
        }
        return definitions;
    }

    private int matchingBrace(String source, int openingBraceIndex) {
        if (source == null || openingBraceIndex < 0 || openingBraceIndex >= source.length() || source.charAt(openingBraceIndex) != '{') {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        for (int index = openingBraceIndex; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '"' && (index == 0 || source.charAt(index - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private String match(String source, Pattern pattern) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String encodeHeadTexture(String headPath) {
        if (headPath == null || headPath.isBlank()) {
            return "";
        }
        String normalized = headPath.startsWith("/head/") ? headPath.substring("/head/".length()) : headPath;
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + normalized + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private record PetDefinition(String headTexture, int maxLevel, int hatchingLevel, String hatchingHeadTexture) {
        private static final PetDefinition DEFAULT = new PetDefinition("", 100, 0, "");
    }

    private record PetSkinDefinition(String name, String texture) {
    }

    private record PetInventoryVisual(
        String uniqueId,
        String uuid,
        String type,
        String tier,
        double experience,
        String heldItem,
        String skinId,
        String skinDisplayName,
        String iconTexture,
        String iconTextureSignature
    ) {
    }

    private record PetInventoryIndex(
        Map<String, PetInventoryVisual> byUniqueId,
        Map<String, PetInventoryVisual> byUuid,
        List<PetInventoryVisual> all
    ) {
    }


    private String materialToMinecraftItemId(JsonObject item) {
        String material = stringValue(item, "material");
        int durability = intValue(item, "durability", 0);
        if (material == null || material.isBlank()) {
            return "minecraft:paper";
        }
        return switch (material.toUpperCase(Locale.ROOT)) {
            case "SKULL_ITEM" -> "minecraft:player_head";
            case "RAW_FISH" -> "minecraft:cod";
            case "COOKED_FISH" -> "minecraft:cooked_cod";
            case "PRISMARINE_CRYSTALS" -> "minecraft:prismarine_crystals";
            case "FIREWORK" -> "minecraft:firework_rocket";
            case "FIREWORK_CHARGE" -> "minecraft:firework_star";
            case "FLINT_AND_STEEL" -> "minecraft:flint_and_steel";
            case "BLAZE_POWDER" -> "minecraft:blaze_powder";
            case "NETHER_BRICK_ITEM", "NETHER_BRICK" -> "minecraft:nether_brick";
            case "WOOD_DOOR" -> "minecraft:oak_door";
            case "IRON_DOOR" -> "minecraft:iron_door";
            case "CAULDRON_ITEM" -> "minecraft:cauldron";
            case "BREWING_STAND_ITEM" -> "minecraft:brewing_stand";
            case "DIODE" -> "minecraft:repeater";
            case "REDSTONE_COMPARATOR" -> "minecraft:comparator";
            case "SULPHUR" -> "minecraft:gunpowder";
            case "GOLD_BARDING" -> "minecraft:golden_horse_armor";
            case "IRON_BARDING" -> "minecraft:iron_horse_armor";
            case "DIAMOND_BARDING" -> "minecraft:diamond_horse_armor";
            case "MINECART" -> "minecraft:minecart";
            case "STORAGE_MINECART" -> "minecraft:chest_minecart";
            case "HOPPER_MINECART" -> "minecraft:hopper_minecart";
            case "POWERED_MINECART" -> "minecraft:furnace_minecart";
            case "EXPLOSIVE_MINECART" -> "minecraft:tnt_minecart";
            case "MELON_BLOCK" -> "minecraft:melon";
            case "WORKBENCH" -> "minecraft:crafting_table";
            case "EMPTY_MAP" -> "minecraft:map";
            case "BED" -> "minecraft:red_bed";
            case "WEB" -> "minecraft:cobweb";
            case "WATER_LILY" -> "minecraft:lily_pad";
            case "NETHER_STALK" -> "minecraft:nether_wart";
            case "SPECKLED_MELON" -> "minecraft:glistering_melon_slice";
            case "EXP_BOTTLE" -> "minecraft:experience_bottle";
            case "WATCH" -> "minecraft:clock";
            case "YELLOW_FLOWER" -> "minecraft:dandelion";
            case "RED_ROSE" -> switch (durability) {
                case 1 -> "minecraft:blue_orchid";
                case 2 -> "minecraft:allium";
                case 3 -> "minecraft:azure_bluet";
                case 4 -> "minecraft:red_tulip";
                case 5 -> "minecraft:orange_tulip";
                case 6 -> "minecraft:white_tulip";
                case 7 -> "minecraft:pink_tulip";
                case 8 -> "minecraft:oxeye_daisy";
                default -> "minecraft:poppy";
            };
            case "DOUBLE_PLANT" -> switch (durability) {
                case 1 -> "minecraft:lilac";
                case 4 -> "minecraft:rose_bush";
                case 5 -> "minecraft:peony";
                default -> "minecraft:sunflower";
            };
            case "INK_SACK" -> switch (durability) {
                case 3 -> "minecraft:cocoa_beans";
                case 4 -> "minecraft:lapis_lazuli";
                case 15 -> "minecraft:bone_meal";
                default -> "minecraft:ink_sac";
            };
            case "BANNER" -> bannerMinecraftId((short) durability);
            case "MONSTER_EGG" -> "minecraft:pig_spawn_egg";
            default -> "minecraft:" + material.toLowerCase(Locale.ROOT);
        };
    }

    private String fallbackPetMinecraftItemId(String type) {
        return "minecraft:player_head";
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
        JsonObject slayers = objectValue(objectValue(member, "slayer"), "slayer_bosses");
        if (slayers.entrySet().isEmpty()) {
            slayers = objectValue(member, "slayer_bosses");
        }
        for (Map.Entry<String, JsonElement> entry : slayers.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject boss = entry.getValue().getAsJsonObject();
            JsonObject parsed = new JsonObject();
            parsed.addProperty("id", entry.getKey());
            parsed.addProperty("displayName", humanize(entry.getKey()));
            parsed.addProperty("level", slayerLevel(entry.getKey(), boss));
            parsed.addProperty("experience", longValue(boss, "xp", 0L));
            parsed.addProperty("kills", slayerKills(boss));
            values.add(parsed);
        }
        return values;
    }

    private int slayerLevel(String slayerId, JsonObject boss) {
        JsonObject claimedLevels = objectValue(boss, "claimed_levels");
        int highest = 0;
        for (String key : claimedLevels.keySet()) {
            Matcher matcher = Pattern.compile("(\\d+)$").matcher(key);
            if (matcher.find()) {
                highest = Math.max(highest, Integer.parseInt(matcher.group(1)));
            }
        }
        return Math.max(highest, slayerLevelFromXp(slayerId, longValue(boss, "xp", 0L)));
    }

    private int slayerLevelFromXp(String slayerId, long experience) {
        if (experience <= 0L) {
            return 0;
        }
        long[] thresholds = switch ((slayerId == null ? "" : slayerId).toLowerCase(Locale.ROOT)) {
            case "zombie", "spider", "wolf" -> new long[]{5L, 15L, 200L, 1_000L, 5_000L, 20_000L, 100_000L, 400_000L, 1_000_000L};
            case "enderman" -> new long[]{10L, 30L, 250L, 1_500L, 5_000L, 20_000L, 100_000L, 400_000L, 1_000_000L};
            case "blaze" -> new long[]{10L, 30L, 250L, 1_500L, 5_000L, 20_000L, 100_000L, 400_000L, 1_000_000L};
            case "vampire" -> new long[]{20L, 75L, 240L, 840L, 2_400L};
            default -> new long[]{5L, 15L, 200L, 1_000L, 5_000L, 20_000L, 100_000L, 400_000L, 1_000_000L};
        };
        int level = 0;
        for (int index = 0; index < thresholds.length; index++) {
            if (experience >= thresholds[index]) {
                level = index + 1;
            }
        }
        return level;
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
        if (level >= requirements.length - 1 && remaining > 0.0D) {
            level += (int) Math.floor(remaining / 200_000_000.0D);
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

    private String getText(String url, boolean withApiKey) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("User-Agent", USER_AGENT)
            .GET();
        if (withApiKey) {
            builder.header("API-Key", config.hypixelApiKey());
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().isBlank()) {
            throw new IOException("HTTP " + response.statusCode() + " fuer " + url);
        }
        return response.body();
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

    private int resolveLeatherColor(CompoundTag display) {
        if (display == null || !display.containsKey("color")) {
            return -1;
        }
        return display.getInt("color").orElse(-1);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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
            Matcher matcher = RARITY_PATTERN.matcher(cleanText(lines[index]).toUpperCase(Locale.ROOT));
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
