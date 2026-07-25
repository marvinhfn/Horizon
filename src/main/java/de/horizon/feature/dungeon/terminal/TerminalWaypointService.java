package de.horizon.feature.dungeon.terminal;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Terminal hitboxes &amp; titles (F7 P3). Records the world position of each terminal the
 * first time its GUI is opened, draws a highlighted box + label there for the rest of the
 * run, and shows the terminal type as an on-screen title on open and on approach.
 */
public final class TerminalWaypointService {

    private static final double PROXIMITY = 6.0;      // blocks: show approach title within this range
    private static final long TITLE_COOLDOWN_MS = 4000; // ms between repeated approach titles per terminal

    // Discovered terminals for the current run: position -> type.
    private final Map<BlockPos, TerminalSolverService.TerminalType> terminals = new LinkedHashMap<>();
    // Terminals already completed this run — no longer highlighted.
    private final java.util.Set<BlockPos> done = new java.util.HashSet<>();

    private BlockPos lastTitledPos = null;
    private long lastTitleTime = 0L;

    /**
     * Called when a terminal screen opens. Records the terminal's world position (the block
     * the player was looking at) and shows its type as a title.
     */
    public void onTerminalOpen(TerminalSolverService.TerminalType type, HorizonConfig config) {
        if (type == TerminalSolverService.TerminalType.NONE) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        if (config.isTerminalWaypointsEnabled() && mc.hitResult instanceof BlockHitResult bhr
                && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            terminals.put(bhr.getBlockPos().immutable(), type);
        }

        if (config.isTerminalTitleEnabled()) {
            showTitle(mc, type);
            lastTitledPos = null; // avoid an immediate duplicate approach-title
            lastTitleTime = System.currentTimeMillis();
        }
    }

    /** Approach detection: shows a title when the player nears a known terminal. */
    public void tick(Minecraft mc, HorizonConfig config, boolean inBoss) {
        if (!config.isTerminalTitleEnabled() || !inBoss || mc == null || mc.player == null) return;
        if (terminals.isEmpty()) return;

        Vec3 eye = mc.player.position();
        BlockPos nearest = null;
        double nearestSq = PROXIMITY * PROXIMITY;
        TerminalSolverService.TerminalType nearestType = TerminalSolverService.TerminalType.NONE;

        for (Map.Entry<BlockPos, TerminalSolverService.TerminalType> e : terminals.entrySet()) {
            BlockPos p = e.getKey();
            double dsq = eye.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            if (dsq < nearestSq) {
                nearestSq = dsq;
                nearest = p;
                nearestType = e.getValue();
            }
        }

        long now = System.currentTimeMillis();
        if (nearest == null) {
            lastTitledPos = null;
            return;
        }
        if (nearest.equals(lastTitledPos) && now - lastTitleTime < TITLE_COOLDOWN_MS) return;

        showTitle(mc, nearestType);
        lastTitledPos = nearest;
        lastTitleTime = now;
    }

    /**
     * Marks a terminal complete when Hypixel announces the local player activated a terminal — you
     * are standing on it at that moment, so the nearest recorded terminal is the one just finished.
     */
    public void handleChatMessage(String raw, Minecraft mc) {
        if (mc == null || mc.player == null || terminals.isEmpty()) return;
        String lower = raw.toLowerCase();
        if (!lower.contains("activated a terminal")) return;
        String self = mc.player.getName().getString().toLowerCase();
        if (!lower.contains(self)) return; // only MY completions — teammates finish their own

        Vec3 pos = mc.player.position();
        BlockPos nearest = null;
        double nearestSq = 64.0; // within 8 blocks
        for (BlockPos p : terminals.keySet()) {
            if (done.contains(p)) continue;
            double dsq = pos.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            if (dsq < nearestSq) { nearestSq = dsq; nearest = p; }
        }
        if (nearest != null) done.add(nearest);
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config, boolean inBoss) {
        if (!config.isTerminalWaypointsEnabled() || !inBoss || terminals.isEmpty()) return;

        List<DungeonRenderUtil.BoxSpec> boxes = new ArrayList<>();
        List<DungeonRenderUtil.StringSpec> labels = new ArrayList<>();

        for (Map.Entry<BlockPos, TerminalSolverService.TerminalType> e : terminals.entrySet()) {
            BlockPos p = e.getKey();
            if (done.contains(p)) continue; // only terminals not yet completed
            int rgb = colorFor(e.getValue());
            AABB box = new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1);
            boxes.add(new DungeonRenderUtil.BoxSpec(box, 0x40000000 | rgb, 0xFF000000 | rgb));
            labels.add(new DungeonRenderUtil.StringSpec(
                e.getValue().label(), p.getX() + 0.5, p.getY() + 1.4, p.getZ() + 0.5));
        }

        DungeonRenderUtil.drawBoxesBatched(ctx, boxes, true, DungeonRenderUtil.DEFAULT_LINE_WIDTH);
        DungeonRenderUtil.drawStringsBatched(ctx, labels);
    }

    private static void showTitle(Minecraft mc, TerminalSolverService.TerminalType type) {
        if (mc.gui == null) return;
        mc.gui.setTitle(Component.literal(type.label()).withStyle(ChatFormatting.AQUA));
        mc.gui.setSubtitle(Component.literal("Terminal").withStyle(ChatFormatting.GRAY));
        mc.gui.setTimes(2, 30, 8);
    }

    private static int colorFor(TerminalSolverService.TerminalType type) {
        return switch (type) {
            case PANES -> 0x55FF55;
            case ORDER -> 0x55FFFF;
            case SAME_COLOR -> 0xFF55FF;
            case ITEM_NAME -> 0xFFAA00;
            case COLOURED_ITEMS -> 0xFFFF55;
            case MELODY -> 0xAA00FF;
            default -> 0xFFFFFF;
        };
    }

    public void reset() {
        terminals.clear();
        done.clear();
        lastTitledPos = null;
        lastTitleTime = 0L;
    }
}
