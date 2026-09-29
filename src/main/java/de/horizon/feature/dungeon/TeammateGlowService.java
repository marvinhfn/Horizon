package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks dungeon party members and their classes from the tablist.
 * Provides class-based glow colors for teammate ESP.
 */
public final class TeammateGlowService {
    // Dungeon party tab lines end with the member's class + level in parentheses, e.g.
    //   "[184] [MVP++] PlayerName (Archer 50)"  or  "[184] PlayerName (Healer XI)"  or  "... (DEAD)".
    // The old full-line regex broke on rank tags containing '+' (\w doesn't match '+') and on the
    // varied level formats, so it matched nothing → no glow. We now only extract the trailing class
    // group and rely on the PlayerInfo's own profile name (never the parsed tab text) to find self.
    private static final Pattern CLASS_SUFFIX = Pattern.compile(
        "\\(([A-Za-z]+)(?:\\s+[^)]*)?\\)\\s*$"
    );

    public enum DungeonClass {
        ARCHER(0xFFAA0000),   // Dark Red
        BERSERK(0xFFFFAA00),  // Gold
        HEALER(0xFFAA00AA),   // Dark Purple
        MAGE(0xFF00AAAA),     // Dark Aqua
        TANK(0xFF00AA00);     // Dark Green

        private final int defaultColor;

        DungeonClass(int defaultColor) {
            this.defaultColor = defaultColor;
        }

        public int defaultColor() { return defaultColor; }

        public static DungeonClass fromName(String name) {
            for (DungeonClass dc : values()) {
                if (dc.name().equalsIgnoreCase(name)) return dc;
            }
            return null;
        }
    }

    public record Teammate(String name, UUID uuid, DungeonClass dungeonClass, boolean dead) {}

    // LinkedHashMap keeps tab-list insertion order — the map's "icon-N" decoration key indexes into
    // the living teammates in this same order, so player heads land on the right party member.
    private final Map<UUID, Teammate> teammates = new java.util.LinkedHashMap<>();
    private DungeonClass selfClass;
    private long lastScanTick = -1;

    public void tick(Minecraft mc, boolean inDungeon) {
        if (!inDungeon || mc == null || mc.level == null || mc.player == null || mc.getConnection() == null) {
            if (!teammates.isEmpty()) teammates.clear();
            return;
        }

        long currentTick = mc.level.getGameTime();
        if (currentTick == lastScanTick) return;
        lastScanTick = currentTick;

        // Only rescan every 20 ticks (1 second) for performance
        if (currentTick % 20 != 0 && !teammates.isEmpty()) return;

        teammates.clear();
        selfClass = null;
        String selfName = mc.player.getName().getString();
        Collection<PlayerInfo> playerInfos = mc.getConnection().getListedOnlinePlayers();

        for (PlayerInfo info : playerInfos) {
            if (info.getTabListDisplayName() == null) continue;
            String tabLine = ChatFormatting.stripFormatting(info.getTabListDisplayName().getString());
            if (tabLine == null) continue;

            // A dungeon-party line has a trailing "(Class Level)" / "(DEAD)" group.
            Matcher m = CLASS_SUFFIX.matcher(tabLine.trim());
            if (!m.find()) continue;

            String classStr = m.group(1);
            boolean dead = "DEAD".equalsIgnoreCase(classStr);
            DungeonClass dc = dead ? null : DungeonClass.fromName(classStr);
            // Not a real party class ("EMPTY" slot etc.) and not dead → skip: not a teammate row.
            if (!dead && dc == null) continue;

            // Use the profile's real name, not the parsed tab text (which carries ranks/emblems).
            String name = info.getProfile().name();
            if (name == null || name.isEmpty()) name = tabLine;

            // Remember the local player's own class separately (kept out of the glow map).
            if (name.equalsIgnoreCase(selfName)) {
                selfClass = dc;
                continue;
            }

            teammates.put(info.getProfile().id(), new Teammate(name, info.getProfile().id(), dc, dead));
        }
    }

    public void onWorldChange() {
        teammates.clear();
        selfClass = null;
        lastScanTick = -1;
    }

    /** The local player's own dungeon class, or null when unknown. */
    public DungeonClass getSelfClass() { return selfClass; }

    /**
     * Returns the glow color for a teammate entity, or -1 if not a teammate.
     */
    public int getTeammateGlowColor(Entity entity, HorizonConfig config) {
        if (!(entity instanceof AbstractClientPlayer)) return -1;
        if (entity.getUUID().version() != 4) return -1;

        Teammate teammate = teammates.get(entity.getUUID());
        if (teammate == null || teammate.dead) return -1;
        if (teammate.dungeonClass == null) return 0xFFFFFFFF; // unknown class = white

        return config.getClassColor(teammate.dungeonClass);
    }

    /**
     * Returns the teammate info for an entity, or null.
     */
    public Teammate getTeammate(Entity entity) {
        if (!(entity instanceof AbstractClientPlayer)) return null;
        return teammates.get(entity.getUUID());
    }

    public Collection<Teammate> getTeammates() {
        return teammates.values();
    }

    /** Living (non-dead) teammates in tab-list order — index matches the map's "icon-N" decoration key. */
    public java.util.List<Teammate> getLivingTeammatesOrdered() {
        java.util.List<Teammate> out = new java.util.ArrayList<>();
        for (Teammate t : teammates.values()) if (!t.dead()) out.add(t);
        return out;
    }
}
