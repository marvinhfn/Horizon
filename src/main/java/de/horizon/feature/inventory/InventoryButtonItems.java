package de.horizon.feature.inventory;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.horizon.HorizonMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Resolves button item-ID strings to {@link ItemStack} instances.
 *
 * Supported formats:
 *   - "minecraft:diamond"          → vanilla item
 *   - "HEAD:CONDENSED_FERMENTO"    → Hypixel SkyBlock skull (fetched from public API)
 */
public final class InventoryButtonItems {

    private InventoryButtonItems() {}

    private static final ItemStack FALLBACK = new ItemStack(Items.BARRIER);

    /** Returns a ready-to-render ItemStack for the given item-ID string. */
    public static ItemStack resolve(String itemId) {
        if (itemId == null || itemId.isBlank()) return FALLBACK.copy();

        String trimmed = itemId.trim();

        if (trimmed.toUpperCase().startsWith("HEAD:")) {
            String skinId = trimmed.substring(5).trim();
            return resolveHead(skinId);
        }

        // Plain Minecraft item
        try {
            Identifier id = Identifier.tryParse(trimmed);
            if (id == null) return FALLBACK.copy();
            Item item = BuiltInRegistries.ITEM.get(id)
                    .map(ref -> ref.value()).orElse(Items.AIR);
            if (item == Items.AIR) return FALLBACK.copy();
            return new ItemStack(item);
        } catch (Exception e) {
            return FALLBACK.copy();
        }
    }

    private static ItemStack resolveHead(String skinId) {
        // Trigger the async load on first use.
        SkyBlockHeadCache.ensureLoaded();

        String texture = SkyBlockHeadCache.getTexture(skinId);
        if (texture == null) {
            // Cache not ready yet or item not found – return a plain skull as placeholder.
            return new ItemStack(Items.PLAYER_HEAD);
        }

        return createSkullFromTexture(texture);
    }

    /**
     * Creates a {@link Items#PLAYER_HEAD} ItemStack with the given base64 texture value
     * set via the Minecraft data-component profile system.
     */
    public static ItemStack createSkullFromTexture(String base64TextureValue) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.properties().put("textures",
                    new Property("textures", base64TextureValue, ""));
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
            skull.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            return skull;
        } catch (Exception e) {
            HorizonMod.LOGGER.warn("InventoryButtonItems: skull creation failed: {}", e.getMessage());
            return new ItemStack(Items.PLAYER_HEAD);
        }
    }
}
