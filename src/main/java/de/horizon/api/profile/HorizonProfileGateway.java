package de.horizon.api.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.horizon.api.HorizonApiClient;
import de.horizon.api.HorizonApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HorizonProfileGateway {
    private final HorizonApiClient apiClient;

    public HorizonProfileGateway(HorizonApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public HorizonProfileData loadProfile(String playerName) throws IOException, InterruptedException {
        JsonObject root = apiClient.getJson("/v1/skyblock/profile", Map.of("player", playerName == null ? "" : playerName.trim()));
        JsonObject profile = objectValue(root, "profile");
        if (profile.entrySet().isEmpty()) {
            throw new HorizonApiException("Horizon-Backend lieferte kein Profil.");
        }

        return new HorizonProfileData(
            stringValue(profile, "playerName"),
            stringValue(profile, "playerUuid"),
            stringValue(profile, "profileId"),
            stringValue(profile, "profileName"),
            intValue(profile, "skyblockLevel"),
            intValue(profile, "catacombsLevel"),
            doubleValue(profile, "purse"),
            doubleValue(profile, "bank"),
            doubleValue(profile, "networth"),
            parseStorages(arrayValue(profile, "storages")),
            parseAccessories(arrayValue(profile, "accessories")),
            parsePets(arrayValue(profile, "pets")),
            parseSkills(arrayValue(profile, "skills")),
            parseSlayers(arrayValue(profile, "slayers")),
            parseMetadata(objectValue(profile, "metadata"))
        );
    }

    private List<HorizonStoragePage> parseStorages(JsonArray storages) {
        List<HorizonStoragePage> pages = new ArrayList<>();
        for (JsonElement element : storages) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject page = element.getAsJsonObject();
            List<HorizonInventorySlot> slots = new ArrayList<>();
            for (JsonElement slotElement : arrayValue(page, "slots")) {
                if (!slotElement.isJsonObject()) {
                    continue;
                }
                JsonObject slot = slotElement.getAsJsonObject();
                slots.add(new HorizonInventorySlot(
                    intValue(slot, "index"),
                    intValue(slot, "x"),
                    intValue(slot, "y"),
                    parseItem(objectValue(slot, "item"))
                ));
            }
            pages.add(new HorizonStoragePage(
                stringValue(page, "id"),
                stringValue(page, "title"),
                intValue(page, "columns"),
                intValue(page, "rows"),
                List.copyOf(slots)
            ));
        }
        return List.copyOf(pages);
    }

    private HorizonInventoryItem parseItem(JsonObject item) {
        if (item.entrySet().isEmpty()) {
            return HorizonInventoryItem.empty();
        }
        return new HorizonInventoryItem(
            stringValue(item, "itemId"),
            stringValue(item, "displayName"),
            stringValue(item, "rarity"),
            intValue(item, "count"),
            stringValue(item, "lore"),
            stringValue(item, "iconTexture"),
            booleanValue(item, "enchanted")
        );
    }

    private List<HorizonAccessory> parseAccessories(JsonArray accessories) {
        List<HorizonAccessory> values = new ArrayList<>();
        for (JsonElement element : accessories) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject accessory = element.getAsJsonObject();
            values.add(new HorizonAccessory(
                stringValue(accessory, "id"),
                stringValue(accessory, "displayName"),
                stringValue(accessory, "rarity"),
                stringValue(accessory, "enrichment"),
                booleanValue(accessory, "active")
            ));
        }
        return List.copyOf(values);
    }

    private List<HorizonPet> parsePets(JsonArray pets) {
        List<HorizonPet> values = new ArrayList<>();
        for (JsonElement element : pets) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject pet = element.getAsJsonObject();
            values.add(new HorizonPet(
                stringValue(pet, "type"),
                stringValue(pet, "displayName"),
                stringValue(pet, "tier"),
                intValue(pet, "level"),
                booleanValue(pet, "active"),
                stringValue(pet, "heldItem")
            ));
        }
        return List.copyOf(values);
    }

    private List<HorizonSkill> parseSkills(JsonArray skills) {
        List<HorizonSkill> values = new ArrayList<>();
        for (JsonElement element : skills) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject skill = element.getAsJsonObject();
            values.add(new HorizonSkill(
                stringValue(skill, "id"),
                stringValue(skill, "displayName"),
                intValue(skill, "level"),
                doubleValue(skill, "progress"),
                doubleValue(skill, "experience")
            ));
        }
        return List.copyOf(values);
    }

    private List<HorizonSlayerBoss> parseSlayers(JsonArray slayers) {
        List<HorizonSlayerBoss> values = new ArrayList<>();
        for (JsonElement element : slayers) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject slayer = element.getAsJsonObject();
            values.add(new HorizonSlayerBoss(
                stringValue(slayer, "id"),
                stringValue(slayer, "displayName"),
                intValue(slayer, "level"),
                longValue(slayer, "experience"),
                intValue(slayer, "kills")
            ));
        }
        return List.copyOf(values);
    }

    private Map<String, String> parseMetadata(JsonObject object) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonNull()) {
                continue;
            }
            values.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Map.copyOf(values);
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

    private String stringValue(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private int intValue(JsonObject object, String key) {
        try {
            return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private long longValue(JsonObject object, String key) {
        try {
            return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsLong() : 0L;
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private double doubleValue(JsonObject object, String key) {
        try {
            return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsDouble() : 0.0D;
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    private boolean booleanValue(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsBoolean();
    }
}
