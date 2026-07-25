package de.horizon.feature.dungeon.terminal;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Terminal / device / lever waypoints for F7 P3, at fixed positions per section (S1–S4). Only the
 * section the player is currently in is shown. Individual waypoints disappear when their hologram is
 * no longer "inactive" (terminals: "Inactive Terminal", levers: "Not Activated", devices: "Inactive
 * Device"); a whole section is also cleared when its chat counter reaches its max (e.g. 7/7, S2 8/8).
 */
public final class TerminalWaypointService {

    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern COUNT = Pattern.compile("\\((\\d+)/(\\d+)\\)");
    private static final double STAGE_RANGE_SQ = 60 * 60;  // show a section only within this range
    private static final double MATCH_SQ = 12.25;          // hologram↔position match radius (3.5)
    private static final double CONFIRM_SQ = 30 * 30;      // trust "hologram gone" only within this
    private static final int SCAN_INTERVAL = 5;

    private enum Kind {
        TERMINAL(0x55FFFF, "Terminal"), DEVICE(0xFFAA00, "Device"), LEVER(0x55FF55, "Lever");
        final int color;
        final String label;
        Kind(int color, String label) { this.color = color; this.label = label; }

        boolean isInactiveText(String name) {
            return switch (this) {
                case LEVER -> name.contains("not activated");
                case TERMINAL -> name.contains("inactive") && name.contains("terminal");
                case DEVICE -> name.contains("inactive") && name.contains("device");
            };
        }
    }

    private record Wp(Kind kind, BlockPos pos) {}

    private static Wp t(int x, int y, int z) { return new Wp(Kind.TERMINAL, new BlockPos(x, y, z)); }
    private static Wp d(int x, int y, int z) { return new Wp(Kind.DEVICE, new BlockPos(x, y, z)); }
    private static Wp l(int x, int y, int z) { return new Wp(Kind.LEVER, new BlockPos(x, y, z)); }

    // Fixed positions per section (from the block the player looked at). Section total = size().
    private static final List<List<Wp>> STAGES = List.of(
        List.of( // S1 (7)
            t(111, 113, 73), t(111, 119, 79), t(89, 112, 92), t(89, 122, 101),
            d(110, 121, 91), l(94, 124, 113), l(106, 124, 113)),
        List.of( // S2 (8)
            t(68, 109, 121), t(59, 120, 122), t(47, 109, 121), t(39, 108, 143), t(40, 124, 122),
            d(60, 131, 142), l(23, 132, 138), l(27, 124, 127)),
        List.of( // S3 (7)
            t(-3, 109, 112), t(-3, 119, 93), t(19, 123, 93), t(-3, 109, 77),
            d(-2, 119, 74), l(2, 122, 55), l(14, 122, 55)),
        List.of( // S4 (7)
            t(41, 109, 29), t(44, 121, 29), t(67, 109, 29), t(72, 115, 48),
            d(63, 127, 35), l(86, 128, 46), l(84, 121, 34))
    );

    private final Set<BlockPos> done = new HashSet<>();
    private final Set<BlockPos> everSeenInactive = new HashSet<>();
    private final Map<BlockPos, Integer> missed = new HashMap<>();
    private int activeStage = -1;
    private int scanCooldown = 0;

    /** Kept for the type title on open (positions are fixed now, no recording). */
    public void onTerminalOpen(TerminalSolverService.TerminalType type, HorizonConfig config) {
        if (type == TerminalSolverService.TerminalType.NONE || !config.isTerminalTitleEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null) {
            mc.gui.setTitle(Component.literal(type.label()).withStyle(ChatFormatting.AQUA));
            mc.gui.setSubtitle(Component.literal("Terminal").withStyle(ChatFormatting.GRAY));
            mc.gui.setTimes(2, 30, 8);
        }
    }

    public void tick(Minecraft mc, HorizonConfig config, boolean inDungeon) {
        if (mc == null || mc.player == null || mc.level == null
                || !config.isTerminalWaypointsEnabled() || !inDungeon) {
            activeStage = -1;
            return;
        }

        Vec3 p = mc.player.position();
        int best = -1;
        double bestSq = STAGE_RANGE_SQ;
        for (int s = 0; s < STAGES.size(); s++) {
            double dsq = stageMinDistSq(p, STAGES.get(s));
            if (dsq < bestSq) { bestSq = dsq; best = s; }
        }
        activeStage = best;
        if (best < 0) return;

        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            scanHolograms(mc, p);
        }
    }

    /**
     * Match each still-inactive hologram to its fixed position; once a position that was seen
     * inactive no longer has one (while the player is near enough for it to be loaded), it's done.
     */
    private void scanHolograms(Minecraft mc, Vec3 p) {
        Set<BlockPos> inactiveNow = new HashSet<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!e.hasCustomName()) continue;
            String name = FORMATTING.matcher(e.getCustomName().getString()).replaceAll("").toLowerCase();
            for (List<Wp> stage : STAGES) {
                for (Wp w : stage) {
                    if (!w.kind().isInactiveText(name)) continue;
                    double dx = e.getX() - (w.pos().getX() + 0.5);
                    double dy = e.getY() - (w.pos().getY() + 0.5);
                    double dz = e.getZ() - (w.pos().getZ() + 0.5);
                    if (dx * dx + dy * dy + dz * dz <= MATCH_SQ) {
                        inactiveNow.add(w.pos());
                        everSeenInactive.add(w.pos());
                    }
                }
            }
        }
        for (List<Wp> stage : STAGES) {
            for (Wp w : stage) {
                BlockPos pos = w.pos();
                if (done.contains(pos)) continue;
                if (inactiveNow.contains(pos)) { missed.put(pos, 0); continue; }
                if (!everSeenInactive.contains(pos)) continue;
                if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > CONFIRM_SQ) continue;
                if (missed.merge(pos, 1, Integer::sum) >= 2) done.add(pos);
            }
        }
    }

    /** A section is fully cleared once its shared counter reaches its max (7/7, S2 8/8). */
    public void handleChatMessage(String raw, Minecraft mc) {
        if (activeStage < 0) return;
        String low = FORMATTING.matcher(raw).replaceAll("").toLowerCase();
        if (!low.contains("activated a terminal") && !low.contains("activated a lever")
                && !low.contains("completed a device")) return;

        // Each section has exactly ONE device, so "completed a device!" unambiguously clears it —
        // its hologram sits farther from the block than the terminal/lever ones, so the distance
        // match can miss it.
        if (low.contains("completed a device")) {
            for (Wp w : STAGES.get(activeStage)) if (w.kind() == Kind.DEVICE) done.add(w.pos());
        }

        Matcher m = COUNT.matcher(low);
        if (!m.find()) return;
        try {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            if (x >= y) for (Wp w : STAGES.get(activeStage)) done.add(w.pos());
        } catch (NumberFormatException ignored) {}
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isTerminalWaypointsEnabled() || activeStage < 0) return;

        List<DungeonRenderUtil.BoxSpec> boxes = new ArrayList<>();
        List<DungeonRenderUtil.StringSpec> labels = new ArrayList<>();
        for (Wp w : STAGES.get(activeStage)) {
            if (done.contains(w.pos())) continue;
            BlockPos pos = w.pos();
            int rgb = w.kind().color;
            AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            boxes.add(new DungeonRenderUtil.BoxSpec(box, 0x40000000 | rgb, 0xFF000000 | rgb));
            labels.add(new DungeonRenderUtil.StringSpec(w.kind().label, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5));
        }
        DungeonRenderUtil.drawBoxesBatched(ctx, boxes, true, DungeonRenderUtil.DEFAULT_LINE_WIDTH);
        DungeonRenderUtil.drawStringsBatched(ctx, labels);
    }

    private static double stageMinDistSq(Vec3 p, List<Wp> stage) {
        double best = Double.MAX_VALUE;
        for (Wp w : stage) {
            double dsq = p.distanceToSqr(w.pos().getX() + 0.5, w.pos().getY() + 0.5, w.pos().getZ() + 0.5);
            if (dsq < best) best = dsq;
        }
        return best;
    }

    public void reset() {
        done.clear();
        everSeenInactive.clear();
        missed.clear();
        activeStage = -1;
        scanCooldown = 0;
    }
}
