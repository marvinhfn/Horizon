package de.horizon.feature.skyblock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Computes an item's <b>craft value</b> = the base item plus every NBT attribute that traces back to a
 * tradeable item (enchants, master stars, recombobulator, hot-potato/fuming books, Art of War/Peace,
 * gemstones, runes, skins, reforge stones). Each component is priced through
 * {@link SkyblockPriceService#componentPrice} — Instabuy by default, Buy Order when {@code buyOrder}.
 *
 * <p>Best-effort by design: essence stars (1–5) and unmapped reforges are not priced; gems and runes
 * are resolved heuristically. Anything that does not resolve simply contributes 0.
 */
public final class ItemCraftValueService {
    private final SkyblockPriceService prices;

    public ItemCraftValueService(SkyblockPriceService prices) {
        this.prices = prices;
    }

    // Stacking / self-levelling enchants (sold by Elizabeth, applied at level 1, level up via XP).
    // Only ONE level-1 book is ever needed regardless of current level.
    private static final java.util.Set<String> STACKING_ENCHANTS = java.util.Set.of(
        "champion", "compact", "cultivating", "expertise", "hecatomb", "toxophilite");

    // Star level (6..10) -> master star item id.
    private static final String[] MASTER_STARS = {
        "FIRST_MASTER_STAR", "SECOND_MASTER_STAR", "THIRD_MASTER_STAR", "FOURTH_MASTER_STAR", "FIFTH_MASTER_STAR"
    };

    // Common tradeable reforge stones (modifier name -> reforge-stone item id). Unmapped reforges skipped.
    private static final Map<String, String> REFORGE_STONES = Map.ofEntries(
        Map.entry("withered", "SADAN_BROOCH"),
        Map.entry("fabled", "DRAGON_CLAW"),
        Map.entry("gilded", "MIDAS_JEWEL"),
        Map.entry("suspicious", "SPOOKY_SHARD"),
        Map.entry("warped", "AOTE_STONE"),
        Map.entry("bulky", "BULKY_STONE"),
        Map.entry("heated", "HOT_STUFF"),
        Map.entry("ambered", "AMBER_MATERIAL"),
        Map.entry("blessed", "BLESSED_FRUIT"),
        Map.entry("fruitful", "ONYX"),
        Map.entry("magnetic", "LAPIS_CRYSTAL"),
        Map.entry("fleet", "DIAMONITE"),
        Map.entry("auspicious", "ROCK_GEMSTONE"),
        Map.entry("mithraic", "PURE_MITHRIL"),
        Map.entry("refined", "REFINED_DIAMOND"),
        Map.entry("stellar", "PETRIFIED_STARFALL"),
        Map.entry("fortified", "METEOR_SHARD"),
        Map.entry("waxed", "BLAZE_WAX"),
        Map.entry("submerged", "DEEP_SEA_ORB"),
        Map.entry("perfect", "DIAMOND_ATOM"),
        Map.entry("necrotic", "NECROMANCER_BROOCH"),
        Map.entry("spiritual", "SPIRIT_STONE"),
        Map.entry("loving", "RED_SCARF"),
        Map.entry("jaded", "JADERALD"),
        Map.entry("giant", "GIANT_TOOTH"),
        Map.entry("bustling", "SKYMART_BROCHURE"));

    /** One line of a craft-value breakdown: what it is and how many coins it contributes. */
    public record CostEntry(String label, long amount) {}

    /** Total craft value in coins; 0 for a non-SkyBlock item or when nothing resolves. */
    public long craftValue(ItemStack stack, boolean buyOrder) {
        long total = 0L;
        for (CostEntry e : breakdown(stack, buyOrder)) total += e.amount();
        return total;
    }

    /** The itemised components that make up {@link #craftValue} (only entries that resolve to &gt; 0). */
    public List<CostEntry> breakdown(ItemStack stack, boolean buyOrder) {
        EnchantData.load();
        List<CostEntry> out = new ArrayList<>();
        if (stack == null || stack.isEmpty()) return out;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return out;
        CompoundTag root = cd.copyTag();
        // MC 26.1.2 Hypixel flattens the old ExtraAttributes fields (enchantments, modifier,
        // upgrade_level, ability_scroll, ...) directly into the custom_data root. Fall back to the
        // legacy nested wrapper only when it's actually present.
        CompoundTag ea = root.getCompoundOrEmpty("ExtraAttributes");
        if (ea.isEmpty()) ea = root;
        String id = root.getStringOr("id", "");
        if (id.isEmpty()) id = ea.getStringOr("id", "");
        if (id.isEmpty()) return out;
        id = id.replaceFirst("^STARRED_", "");

        add(out, "Base: " + id, prices.componentPrice(id, buyOrder));

        // Enchantments -> ENCHANTMENT_[ULTIMATE_]<NAME>_<lvl>
        CompoundTag ench = ea.getCompoundOrEmpty("enchantments");
        for (String key : ench.keySet()) {
            int lvl = ench.getIntOr(key, 0);
            if (lvl <= 0) continue;
            boolean ult = key.startsWith("ultimate_");
            boolean stacking = STACKING_ENCHANTS.contains(key);
            String name = key.toUpperCase(Locale.ROOT);
            int l = Math.max(1, Math.min(lvl, 15));
            long l1 = prices.componentPrice("ENCHANTMENT_" + name + "_1", buyOrder);
            long value;
            String note;
            if (stacking) {
                // Self-levelling (Elizabeth) enchant: only one level-1 book is ever applied.
                value = l1;
                note = "1x L1 (stacking)";
            } else if (!ult && prices.componentPrice("ENCHANTMENT_" + name + "_" + l, buyOrder) > 0) {
                value = prices.componentPrice("ENCHANTMENT_" + name + "_" + l, buyOrder);
                note = "book L" + l;
            } else {
                int count = 1 << (l - 1);
                value = (long) count * l1;
                note = count + "x L1";
            }
            String disp = EnchantData.displayName(key);
            add(out, (disp != null ? disp : key) + " " + EnchantData.toRoman(lvl) + " §8(" + note + ")", value);
        }

        // Master stars (levels 6..10 from upgrade_level / dungeon_item_level)
        int stars = Math.max(ea.getIntOr("upgrade_level", 0), ea.getIntOr("dungeon_item_level", 0));
        for (int lvl = 6; lvl <= stars && lvl <= 10; lvl++) {
            add(out, "Master Star " + (lvl - 5), prices.componentPrice(MASTER_STARS[lvl - 6], buyOrder));
        }

        // Recombobulator
        if (ea.getIntOr("rarity_upgrades", 0) > 0) {
            add(out, "Recombobulator", prices.componentPrice("RECOMBOBULATOR_3000", buyOrder));
        }

        // Hot Potato / Fuming books (first 10 hot potato, remainder fuming)
        int potato = ea.getIntOr("hot_potato_count", 0);
        if (potato > 0) {
            int hot = Math.min(potato, 10);
            int fuming = Math.max(0, potato - 10);
            if (hot > 0) add(out, hot + "x Hot Potato Book", hot * prices.componentPrice("HOT_POTATO_BOOK", buyOrder));
            if (fuming > 0) add(out, fuming + "x Fuming Potato Book", fuming * prices.componentPrice("FUMING_POTATO_BOOK", buyOrder));
        }

        // Art of War / Art of Peace
        int aow = ea.getIntOr("art_of_war_count", 0);
        if (aow > 0) add(out, aow + "x The Art of War", aow * prices.componentPrice("THE_ART_OF_WAR", buyOrder));
        if (ea.getIntOr("artOfPeaceApplied", 0) > 0 || ea.getBooleanOr("artOfPeaceApplied", false)) {
            add(out, "The Art of Peace", prices.componentPrice("THE_ART_OF_PEACE", buyOrder));
        }

        // Ability scrolls (e.g. Hyperion: Implosion / Shadow Warp / Wither Shield)
        net.minecraft.nbt.ListTag scrolls = ea.getListOrEmpty("ability_scroll");
        for (int i = 0; i < scrolls.size(); i++) {
            String s = scrolls.getStringOr(i, "");
            if (!s.isEmpty()) add(out, "Scroll: " + s, prices.componentPrice(s.toUpperCase(Locale.ROOT), buyOrder));
        }

        // Etherwarp conduit, mana disintegrators, wood singularity, transmission tuners
        if (ea.getIntOr("ethermerge", 0) > 0) add(out, "Etherwarp Conduit", prices.componentPrice("ETHERWARP_CONDUIT", buyOrder));
        int manaDis = ea.getIntOr("mana_disintegrator_count", 0);
        if (manaDis > 0) add(out, manaDis + "x Mana Disintegrator", manaDis * prices.componentPrice("MANA_DISINTEGRATOR", buyOrder));
        int woodSing = ea.getIntOr("wood_singularity_count", 0);
        if (woodSing > 0) add(out, woodSing + "x Wood Singularity", woodSing * prices.componentPrice("WOOD_SINGULARITY", buyOrder));
        int tuners = ea.getIntOr("tuned_transmission", 0);
        if (tuners > 0) add(out, tuners + "x Transmission Tuner", tuners * prices.componentPrice("TRANSMISSION_TUNER", buyOrder));

        // Power scroll, talisman enrichment, drill parts (best-effort, ids map straight through)
        String powerScroll = ea.getStringOr("power_ability_scroll", "");
        if (!powerScroll.isEmpty()) add(out, "Power Scroll: " + powerScroll, prices.componentPrice(powerScroll.toUpperCase(Locale.ROOT), buyOrder));
        String enrichment = ea.getStringOr("talisman_enrichment", "");
        if (!enrichment.isEmpty()) add(out, "Enrichment: " + enrichment, prices.componentPrice("TALISMAN_ENRICHMENT_" + enrichment.toUpperCase(Locale.ROOT), buyOrder));
        for (String part : new String[]{"drill_part_upgrade_module", "drill_part_fuel_tank", "drill_part_engine"}) {
            String p = ea.getStringOr(part, "");
            if (!p.isEmpty()) add(out, "Drill: " + p, prices.componentPrice(p.toUpperCase(Locale.ROOT), buyOrder));
        }

        // Skin
        String skin = ea.getStringOr("skin", "");
        if (!skin.isEmpty()) add(out, "Skin: " + skin, prices.componentPrice(skin.toUpperCase(Locale.ROOT), buyOrder));

        // Dye (applied) — dye_item already holds the full item id, e.g. DYE_NECRON
        String dye = ea.getStringOr("dye_item", "");
        if (!dye.isEmpty()) add(out, "Dye: " + dye, prices.componentPrice(dye.toUpperCase(Locale.ROOT), buyOrder));

        // Reforge stone (best-effort, only tradeable stones)
        String modifier = ea.getStringOr("modifier", "");
        String stone = REFORGE_STONES.get(modifier.toLowerCase(Locale.ROOT));
        if (stone != null) add(out, "Reforge: " + modifier + " (" + stone + ")", prices.componentPrice(stone, buyOrder));

        // Gemstones (best-effort): <QUALITY>_<TYPE>_GEM
        gemsBreakdown(ea.getCompoundOrEmpty("gems"), buyOrder, out);

        // Runes — lowest-BIN id is RUNE-<NAME>-<level> (dashes), NBT is { <NAME>: level }.
        CompoundTag runes = ea.getCompoundOrEmpty("runes");
        for (String key : runes.keySet()) {
            int rlvl = Math.max(1, runes.getIntOr(key, 1));
            add(out, "Rune: " + key + " " + rlvl,
                prices.componentPrice("RUNE-" + key.toUpperCase(Locale.ROOT) + "-" + rlvl, buyOrder));
        }

        return out;
    }

    private static void add(List<CostEntry> out, String label, long amount) {
        if (amount > 0) out.add(new CostEntry(label, amount));
    }

    private void gemsBreakdown(CompoundTag gems, boolean buyOrder, List<CostEntry> out) {
        for (String key : gems.keySet()) {
            if (key.equals("unlocked_slots") || key.endsWith("_gem")) continue;
            // key is like "JADE_0" / "COMBAT_0"; type is the part before the trailing "_<index>".
            int us = key.lastIndexOf('_');
            String type = us > 0 ? key.substring(0, us) : key;
            // Universal/combat slots carry the actual gem type in a sibling "<key>_gem" entry.
            String siblingType = gems.getStringOr(key + "_gem", "");
            if (!siblingType.isEmpty()) type = siblingType;
            // Value may be a plain quality string or a { quality: "FINE", uuid: ... } compound.
            String quality = gems.getCompoundOrEmpty(key).getStringOr("quality", "");
            if (quality.isEmpty()) quality = gems.getStringOr(key, "");
            if (quality.isEmpty() || type.isEmpty()) continue;
            String gemId = quality.toUpperCase(Locale.ROOT) + "_" + type.toUpperCase(Locale.ROOT) + "_GEM";
            add(out, "Gem: " + quality + " " + type, prices.componentPrice(gemId, buyOrder));
        }
    }
}
