package de.horizon.feature.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import de.horizon.hypixel.SkyBlockIsland;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User waypoints, stored per SkyBlock island and persisted to disk. Renders boxes/beacons/labels in
 * the world; when an island's list is "sorted" it draws a route tracer that advances as you reach
 * each waypoint. Edit mode lets you create waypoints by right-clicking blocks and open a waypoint's
 * config by left-clicking it.
 */
public final class WaypointService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One island's waypoints + the set of groups that are "sorted" (ordered route). */
    public static final class IslandWaypoints {
        public List<Waypoint> waypoints = new ArrayList<>();
        public java.util.Set<String> sortedGroups = new java.util.HashSet<>();
        public transient Map<String, Integer> routeIndex = new java.util.HashMap<>();
    }

    private static final class Data {
        Map<String, IslandWaypoints> islands = new LinkedHashMap<>();
    }

    private final Data data = new Data();
    private boolean loaded = false;
    private boolean editMode = false;

    // Detection references (set by HorizonClient) so dungeon waypoints save per-room.
    private de.horizon.feature.dungeon.DungeonStateService state;
    private de.horizon.feature.dungeon.room.DungeonRoomDetector roomDetector;

    public void wire(de.horizon.feature.dungeon.DungeonStateService state,
                     de.horizon.feature.dungeon.room.DungeonRoomDetector roomDetector) {
        this.state = state;
        this.roomDetector = roomDetector;
    }

    /**
     * All selectable islands for the dropdown: {id, label}. The ids match {@link SkyBlockIsland#id()}
     * (+ "dungeons" from the dungeon state) so a waypoint created on an island is filterable there.
     */
    public static final String[][] ISLANDS = {
        {"private_island", "Private Island"}, {"garden", "The Garden"}, {"hub", "Hub"},
        {"farming_islands", "The Farming Islands"}, {"spiders_den", "Spider's Den"},
        {"end", "The End"}, {"crimson_isle", "Crimson Isle"}, {"kuudra", "Kuudra"},
        {"dwarven_mines", "Dwarven Mines"}, {"crystal_hollows", "Crystal Hollows"},
        {"jerry", "Jerry's Workshop"}, {"dungeon_hub", "Dungeon Hub"},
        {"dungeons", "The Catacombs (Dungeons)"}, {"rift", "The Rift"}, {"unknown", "Other / Unknown"},
    };

    private static Path file() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
            .resolve("horizon").resolve("waypoints.json");
    }

    private void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            try (Reader r = Files.newBufferedReader(f)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d != null && d.islands != null) data.islands.putAll(d.islands);
            }
        } catch (Exception ignored) { }
    }

    public void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            try (Writer w = Files.newBufferedWriter(f)) {
                GSON.toJson(data, w);
            }
        } catch (Exception ignored) { }
    }

    // ── Island / list access ─────────────────────────────────────────────────────

    public String currentIslandId() {
        // Dungeons are detected reliably by the dungeon state (fromTabList often returns UNKNOWN there).
        if (state != null && state.isInDungeon()) return "dungeons";
        SkyBlockIsland island = SkyBlockIsland.fromTabList(Minecraft.getInstance());
        String id = island == null ? "unknown" : island.id();
        for (String[] i : ISLANDS) if (i[0].equals(id)) return id;
        return "unknown";
    }

    public List<String> knownIslandIds() {
        List<String> ids = new ArrayList<>();
        for (String[] i : ISLANDS) ids.add(i[0]);
        return ids;
    }

    public static String islandLabel(String id) {
        for (String[] i : ISLANDS) if (i[0].equals(id)) return i[1];
        return id;
    }

    public IslandWaypoints island(String id) {
        loadIfNeeded();
        return data.islands.computeIfAbsent(id, k -> new IslandWaypoints());
    }

    public List<Waypoint> waypoints(String islandId) {
        return island(islandId).waypoints;
    }

    public java.util.List<String> groups(String islandId) {
        java.util.LinkedHashSet<String> g = new java.util.LinkedHashSet<>();
        g.add("Default");
        for (Waypoint w : waypoints(islandId)) g.add(w.group == null || w.group.isBlank() ? "Default" : w.group);
        return new ArrayList<>(g);
    }

    public boolean isGroupSorted(String islandId, String group) {
        return island(islandId).sortedGroups.contains(group);
    }

    public void setGroupSorted(String islandId, String group, boolean sorted) {
        if (sorted) island(islandId).sortedGroups.add(group);
        else island(islandId).sortedGroups.remove(group);
        save();
    }

    public boolean isEditMode() { return editMode; }
    public void setEditMode(boolean v) { editMode = v; }

    // ── Edit interactions ─────────────────────────────────────────────────────────

    /** Right-click a block in edit mode → create a default waypoint there. */
    public boolean onBlockInteract(BlockPos pos) {
        if (!editMode || pos == null) return false;
        IslandWaypoints iw = island(currentIslandId());
        int n = iw.waypoints.size() + 1;
        Waypoint w = new Waypoint(pos.getX(), pos.getY(), pos.getZ(), "Waypoint #" + n);
        // In a dungeon CLEAR room (not the boss), store RELATIVE to the room so it works across runs
        // where the room is placed differently. In the boss section (or elsewhere) keep fixed coords —
        // the boss area is always at the same coordinates.
        if (state != null && roomDetector != null && state.isInDungeon() && !state.isInBoss()) {
            roomDetector.currentRoom().ifPresent(room -> {
                BlockPos rel = roomDetector.worldToRelative(room, pos);
                w.room = room.name();
                w.x = rel.getX(); w.y = rel.getY(); w.z = rel.getZ();
            });
        }
        iw.waypoints.add(w);
        save();
        return true;
    }

    /** The waypoint whose block a click/crosshair is on, or null (handles room-relative waypoints). */
    public Waypoint pick(BlockPos pos) {
        if (pos == null) return null;
        for (Waypoint w : waypoints(currentIslandId())) {
            BlockPos wp = worldPos(w);
            if (wp != null && wp.getX() == pos.getX() && wp.getY() == pos.getY() && wp.getZ() == pos.getZ()) return w;
        }
        return null;
    }

    /** World position of a waypoint (converts room-relative ones via the current room), or null if a
     * room-relative waypoint's room isn't the current room. */
    private BlockPos worldPos(Waypoint w) {
        if (w.room == null) return new BlockPos(w.x, w.y, w.z);
        if (roomDetector == null) return null;
        return roomDetector.currentRoom()
            .filter(r -> r.name().equalsIgnoreCase(w.room))
            .map(r -> roomDetector.relativeToWorld(r, new BlockPos(w.x, w.y, w.z)))
            .orElse(null);
    }

    public void remove(String islandId, Waypoint w) {
        island(islandId).waypoints.remove(w);
        save();
    }

    // ── Import / export (JSON via clipboard) ─────────────────────────────────────

    public String exportIsland(String islandId) {
        return GSON.toJson(island(islandId).waypoints);
    }

    public boolean importIsland(String islandId, String json) {
        try {
            Waypoint[] arr = GSON.fromJson(json, Waypoint[].class);
            if (arr == null) return false;
            IslandWaypoints iw = island(islandId);
            for (Waypoint w : arr) if (w != null) iw.waypoints.add(w);
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────────

    /**
     * True if a DIFFERENT block sits between the camera and the waypoint block (so it should be
     * hidden). The waypoint usually sits on a solid block, so a naive raycast to its centre always
     * hits that block — we must ignore the waypoint's own block and any block adjacent to it.
     */
    private static boolean isOccluded(Minecraft mc, Vec3 cam, BlockPos wp) {
        if (mc.level == null) return false;
        Vec3 target = new Vec3(wp.getX() + 0.5, wp.getY() + 0.5, wp.getZ() + 0.5);
        net.minecraft.world.phys.BlockHitResult hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
            cam, target, net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return false;
        BlockPos hp = hit.getBlockPos();
        // Not occluded if the ray reached the waypoint's own block (or a face directly touching it).
        int md = Math.abs(hp.getX() - wp.getX()) + Math.abs(hp.getY() - wp.getY()) + Math.abs(hp.getZ() - wp.getZ());
        if (md <= 1) return false;
        return hit.getLocation().distanceTo(cam) < target.distanceTo(cam) - 1.0;
    }

    public void renderWorld(LevelRenderContext ctx) {
        loadIfNeeded();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        IslandWaypoints iw = island(currentIslandId());
        if (iw.waypoints.isEmpty()) return;

        Vec3 camPos = ctx.levelState().cameraRenderState.pos;
        for (Waypoint w : iw.waypoints) {
            BlockPos wp = worldPos(w); // null = a room-relative waypoint whose room isn't the current one
            if (wp == null) continue;
            if (!w.throughWalls && isOccluded(mc, camPos, wp)) continue;
            AABB box = new AABB(wp.getX(), wp.getY(), wp.getZ(), wp.getX() + 1, wp.getY() + 1, wp.getZ() + 1);
            int style = w.type == 0 ? 1 : (w.type == 1 ? 0 : 2); // Outlined=1, Box=0(filled), Both=2
            DungeonRenderUtil.drawBox(ctx, box, w.color, style, true, 3.0f);
            if (w.beacon) {
                AABB beam = new AABB(wp.getX() + 0.3, wp.getY(), wp.getZ() + 0.3, wp.getX() + 0.7, wp.getY() + 300, wp.getZ() + 0.7);
                DungeonRenderUtil.drawBox(ctx, beam, (w.color & 0x00FFFFFF) | 0x40000000, 0, true, 1f);
            }
            DungeonRenderUtil.drawString(ctx, w.name, wp.getX() + 0.5, wp.getY() + 1.4, wp.getZ() + 0.5, w.color, 1.0f, null);
        }

        // Sorted groups: an ordered route. Advance the current index when the player reaches the NEXT
        // waypoint (within 3 blocks), then draw a tracer from the current waypoint to the next one.
        Vec3 pp = mc.player.position();
        for (String group : iw.sortedGroups) {
            List<Vec3> route = new ArrayList<>();
            int color = 0xFF55FF55;
            for (Waypoint w : iw.waypoints) {
                if (!group.equals(w.group)) continue;
                BlockPos wp = worldPos(w);
                if (wp != null) { route.add(new Vec3(wp.getX() + 0.5, wp.getY() + 0.5, wp.getZ() + 0.5)); color = w.color; }
            }
            if (route.size() < 2) continue;
            int cur = iw.routeIndex.getOrDefault(group, 0);
            if (cur >= route.size()) cur = 0;
            int next = (cur + 1) % route.size();
            if (pp.distanceTo(route.get(next)) < 3.0) { cur = next; next = (cur + 1) % route.size(); }
            iw.routeIndex.put(group, cur);
            DungeonRenderUtil.drawLine(ctx, List.of(route.get(cur), route.get(next)), color, true, 3.0f);
        }
    }
}
