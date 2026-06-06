package de.horizon.hypixel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public enum SkyBlockIsland {
    HUB("Hub", "hub"),
    DUNGEONS("Dungeons", "dungeons"),
    GARDEN("Garden", "garden"),
    DWARVEN_MINES("Dwarven Mines", "dwarven_mines"),
    CRYSTAL_HOLLOWS("Crystal Hollows", "crystal_hollows"),
    CRIMSON_ISLE("Crimson Isle", "crimson_isle"),
    FARMING_ISLANDS("Farming Islands", "farming_islands"),
    THE_RIFT("The Rift", "rift"),
    SPIDERS_DEN("Spider's Den", "spiders_den"),
    THE_END("The End", "end"),
    DUNGEON_HUB("Dungeon Hub", "dungeon_hub"),
    PRIVATE_ISLAND("Private Island", "private_island"),
    JERRY_ISLAND("Jerry's Workshop", "jerry"),
    KUUDRA("Kuudra", "kuudra"),
    UNKNOWN("Unknown", "unknown");

    private final String label;
    private final String id;

    SkyBlockIsland(String label, String id) {
        this.label = label;
        this.id = id;
    }

    public String label() {
        return label;
    }

    public String id() {
        return id;
    }

    public static SkyBlockIsland[] knownIslands() {
        SkyBlockIsland[] all = values();
        SkyBlockIsland[] known = new SkyBlockIsland[all.length - 1];
        int i = 0;
        for (SkyBlockIsland island : all) {
            if (island != UNKNOWN) {
                known[i++] = island;
            }
        }
        return known;
    }

    /**
     * Detects the current island from scoreboard title and lines.
     * Used by the custom scoreboard overlay for display purposes.
     */
    public static SkyBlockIsland detect(String title, List<String> lines) {
        // Dungeons: high-confidence content signals
        String normTitle = norm(title);
        if (normTitle.contains("catacombs")) return DUNGEONS;
        for (String line : lines) {
            String n = norm(line);
            if (n.contains("the catacombs") || n.contains("master catacombs")
                || n.contains("crypts:") || n.contains("secrets found")) {
                return DUNGEONS;
            }
        }

        // Secondary content signals (no ⏣ needed)
        boolean hasPowder = false;
        for (String line : lines) {
            String n = norm(line);
            if (n.contains("powder:") || n.contains("mithril powder") || n.contains("gemstone powder")) {
                hasPowder = true;
            }
            // "garden" can appear anywhere in a line (location, title, sub-area)
            if (n.contains("garden")) return GARDEN;
            // "Plot - N" lines only appear in the Garden
            if (n.contains("plot -") || n.contains("plot \u2013")) return GARDEN;
            // Dojo is exclusive to the Crimson Isle
            if (n.contains("dojo")) return CRIMSON_ISLE;
            // Rift time countdown is exclusive to The Rift
            if (n.contains("rift time") || n.contains("lifetime:")) return THE_RIFT;
        }

        // ⏣ line: check sub-location names comprehensively
        for (String line : lines) {
            if (!line.contains("⏣")) continue;
            String n = norm(line);

            // Garden
            if (n.contains("garden") || n.contains("plot")) return GARDEN;

            // Glacite Tunnels is part of Dwarven Mines
            if (n.contains("glacite") || n.contains("fossil research")) return DWARVEN_MINES;

            // Crystal Hollows sub-locations
            if (n.contains("crystal hollow") || n.contains("crystal nucleus")
                || n.contains("mithril deposit") || n.contains("goblin holdout")
                || n.contains("jungle") && hasPowder
                || n.contains("precursor") || n.contains("magma fields")) return CRYSTAL_HOLLOWS;

            // Dwarven Mines sub-locations
            if (n.contains("dwarven") || n.contains("royal mines") || n.contains("upper mines")
                || n.contains("far reserve") || n.contains("mithril quarry")
                || n.contains("goblin burrow") || n.contains("royal palace")
                || n.contains("the lift") || n.contains("forge") || n.contains("deep forges")
                || n.contains("throne room") || n.contains("rampart") || n.contains("cliffside")
                || n.contains("puzzle room")) return DWARVEN_MINES;

            // Kuudra is part of Crimson Isle
            if (n.contains("kuudra") || n.contains("molten ford") || n.contains("nether fortress")) return CRIMSON_ISLE;

            // Crimson Isle sub-locations
            if (n.contains("crimson isle") || n.contains("dragontail") || n.contains("stronghold")
                || n.contains("town of salt") || n.contains("mage outpost")
                || n.contains("barbarian outpost") || n.contains("lava springs")
                || n.contains("ruins of the nether") || n.contains("bastion")
                || n.contains("burning desert") || n.contains("scarecrow")
                || n.contains("dojo")) return CRIMSON_ISLE;

            // Farming Islands
            if (n.contains("farming island") || n.contains("mushroom desert")
                || n.contains("barn") || n.contains("shepherd")) return FARMING_ISLANDS;

            // The Rift
            if (n.contains("the rift") || n.contains("still gorge") || n.contains("wyld woods")
                || n.contains("west village") || n.contains("barter bank")
                || n.contains("mirrorverse") || n.contains("lagoon")
                || n.contains("dreadfarm") || n.contains("rift")) return THE_RIFT;

            // Spider's Den
            if (n.contains("spider") || n.contains("arachneum")) return SPIDERS_DEN;

            // The End sub-locations
            if (n.contains("the end") || n.contains("dragon") || n.contains("void slate")) return THE_END;

            // Any other ⏣ line on SkyBlock → Hub (Hub has many sub-locations)
            return HUB;
        }

        // Fallback via powder if no ⏣ matched
        if (hasPowder) return DWARVEN_MINES;

        return UNKNOWN;
    }

    /**
     * Reads the current island from the tab list "Area: <name>" entry.
     * Used for feature gating (island filters, dungeon-only features, etc.).
     */
    public static SkyBlockIsland fromTabList(MinecraftClient mc) {
        if (mc == null) return UNKNOWN;
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return UNKNOWN;
        Collection<PlayerListEntry> entries = handler.getPlayerList();
        for (PlayerListEntry entry : entries) {
            Text display = entry.getDisplayName();
            if (display == null) continue;
            String text = display.getString().toLowerCase(Locale.ROOT).trim();
            if (text.contains("area:")) {
                String area = text.substring(text.indexOf("area:") + 5).trim();
                return fromAreaName(area);
            }
        }
        return UNKNOWN;
    }

    /** Maps an area name (from tab list) to an island enum value. */
    private static SkyBlockIsland fromAreaName(String area) {
        if (area.isEmpty()) return UNKNOWN;

        if (area.contains("garden")) return GARDEN;
        if (area.contains("dungeon hub")) return DUNGEON_HUB;
        if (area.contains("catacombs") || area.contains("dungeon")) return DUNGEONS;
        if (area.contains("hub")) return HUB;
        if (area.contains("dwarven") || area.contains("glacite") || area.contains("forge")) return DWARVEN_MINES;
        if (area.contains("crystal hollow")) return CRYSTAL_HOLLOWS;
        if (area.contains("crimson")) return CRIMSON_ISLE;
        if (area.contains("kuudra")) return KUUDRA;
        if (area.contains("farming island") || area.contains("barn") || area.contains("mushroom desert")) return FARMING_ISLANDS;
        if (area.contains("rift")) return THE_RIFT;
        if (area.contains("spider")) return SPIDERS_DEN;
        if (area.contains("the end") || area.contains("dragon")) return THE_END;
        if (area.contains("jerry")) return JERRY_ISLAND;
        if (area.contains("private island") || area.contains("your island")) return PRIVATE_ISLAND;

        return UNKNOWN;
    }

    private static String norm(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
