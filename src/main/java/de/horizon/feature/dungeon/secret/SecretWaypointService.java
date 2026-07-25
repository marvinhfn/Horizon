package de.horizon.feature.dungeon.secret;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Renders secret waypoints for the room the player is currently in. Positions
 * come from {@link SecretDatabase} in room-local coordinates and are placed in
 * the world via the detector's rotation-aware {@code relativeToWorld}.
 *
 * <p>A waypoint disappears only once its secret is <em>actually</em> done and
 * stays hidden for the rest of the run (state persists across re-entering the
 * room). Detection is state/entity based so it can never be cleared merely by
 * clicking near it, from too far away, or through a wall:
 * <ul>
 *   <li>lever — the lever block's powered state actually flips;</li>
 *   <li>chest — a chest block at the waypoint is actually opened (a locked chest
 *       re-shows when "That chest is locked!" appears);</li>
 *   <li>wither essence — the head/skull block at the waypoint is actually gone;</li>
 *   <li>item / bat — the entity we saw at the waypoint is actually gone;</li>
 *   <li>redstone key — chest/lever interaction, or the chat pickup line.</li>
 * </ul>
 */
public final class SecretWaypointService {

    private static final double LABEL_RANGE_SQR = 24 * 24;
    private static final double ENTITY_PROBE = 2.0;   // radius to detect the item/bat entity
    private static final double NEAR_PLAYER_SQR = 20 * 20; // only evaluate when we're in the room

    // Persist for the whole dungeon run (world coords are unique per room), reset on world change.
    private final Set<BlockPos> done = new HashSet<>();
    private final Set<BlockPos> interacted = new HashSet<>();
    private final Set<BlockPos> armed = new HashSet<>();          // item/bat waypoints where we saw the entity
    private final Set<BlockPos> essenceSeen = new HashSet<>();    // essence waypoints where we saw the skull block
    private final Map<BlockPos, Boolean> leverBaseline = new HashMap<>(); // lever waypoint -> initial powered state
    private BlockPos lastInteractPos = null;

    /** World-space render pass; call from the level render hook. */
    public void renderWorld(LevelRenderContext ctx, HorizonConfig config, DungeonRoomDetector detector,
                            boolean inDungeon, boolean inBoss) {
        if (config == null || !config.isSecretWaypointsEnabled()) return;
        if (!inDungeon || inBoss || detector == null) return;

        Optional<DetectedDungeonRoom> current = detector.currentRoom();
        if (current.isEmpty()) return;
        DetectedDungeonRoom room = current.get();

        Map<SecretType, List<BlockPos>> secrets = SecretDatabase.forRoom(room.name());
        if (secrets.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Vec3 eye = mc.player != null ? mc.player.position() : Vec3.ZERO;
        boolean noDepth = config.isSecretWaypointsThroughWalls();

        List<DungeonRenderUtil.BoxSpec> boxes = new ArrayList<>();
        List<DungeonRenderUtil.StringSpec> labels = new ArrayList<>();
        for (Map.Entry<SecretType, List<BlockPos>> entry : secrets.entrySet()) {
            SecretType type = entry.getKey();
            if (!config.isSecretCategoryEnabled(type)) continue;
            int rgb = config.getSecretCategoryColor(type) & 0xFFFFFF;
            for (BlockPos rel : entry.getValue()) {
                BlockPos w = detector.relativeToWorld(room, rel);
                if (done.contains(w)) continue;
                if (isCompleted(type, w, eye, level)) {
                    done.add(w);
                    continue;
                }
                AABB box = new AABB(w.getX(), w.getY(), w.getZ(), w.getX() + 1, w.getY() + 1, w.getZ() + 1);
                boxes.add(new DungeonRenderUtil.BoxSpec(box, 0x40000000 | rgb, 0xFF000000 | rgb));
                if (eye.distanceToSqr(w.getX() + 0.5, w.getY() + 0.5, w.getZ() + 0.5) <= LABEL_RANGE_SQR) {
                    labels.add(new DungeonRenderUtil.StringSpec(type.label(), w.getX() + 0.5, w.getY() + 1.3, w.getZ() + 0.5));
                }
            }
        }

        DungeonRenderUtil.drawBoxesBatched(ctx, boxes, noDepth, DungeonRenderUtil.DEFAULT_LINE_WIDTH);
        DungeonRenderUtil.drawStringsBatched(ctx, labels);
    }

    /** Whether the secret at world pos {@code w} of {@code type} is now genuinely done. */
    private boolean isCompleted(SecretType type, BlockPos w, Vec3 eye, Level level) {
        return switch (type) {
            case CHEST -> chestOpened(w, level);
            case LEVER -> leverToggled(w, eye, level);
            case ESSENCE -> essenceCollected(w, eye, level);
            case REDSTONE -> redstoneUsed(w, level);
            case ITEM -> entityPickedUp(w, eye, level, ItemEntity.class);
            case BAT -> entityPickedUp(w, eye, level, Bat.class);
        };
    }

    private boolean tooFar(BlockPos w, Vec3 eye) {
        return eye.distanceToSqr(w.getX() + 0.5, w.getY() + 0.5, w.getZ() + 0.5) > NEAR_PLAYER_SQR;
    }

    /** True once a chest block at the waypoint has actually been right-clicked (opened). */
    private boolean chestOpened(BlockPos w, Level level) {
        if (level == null) return false;
        for (BlockPos p : interacted) {
            if (near(p, w) && level.getBlockState(p).getBlock() instanceof ChestBlock) return true;
        }
        return false;
    }

    /** Redstone-key secrets: a chest or lever was interacted (the chat line also clears these). */
    private boolean redstoneUsed(BlockPos w, Level level) {
        if (level == null) return false;
        for (BlockPos p : interacted) {
            if (!near(p, w)) continue;
            var block = level.getBlockState(p).getBlock();
            if (block instanceof ChestBlock || block instanceof LeverBlock) return true;
        }
        return false;
    }

    /** True once the lever block at (or ±1 of) the waypoint has flipped from its initial state. */
    private boolean leverToggled(BlockPos w, Vec3 eye, Level level) {
        if (level == null || tooFar(w, eye)) return false;
        BlockPos leverPos = findBlock(level, w, s -> s.getBlock() instanceof LeverBlock);
        if (leverPos == null) return false;
        boolean powered = level.getBlockState(leverPos).getValue(LeverBlock.POWERED);
        Boolean baseline = leverBaseline.get(w);
        if (baseline == null) {
            leverBaseline.put(w, powered);
            return false;
        }
        return powered != baseline;
    }

    /** True once a head/skull block we saw at the waypoint has been removed (collected). */
    private boolean essenceCollected(BlockPos w, Vec3 eye, Level level) {
        if (level == null || tooFar(w, eye)) return false;
        BlockPos skullPos = findBlock(level, w, s -> s.getBlock() instanceof AbstractSkullBlock);
        if (skullPos != null) {
            essenceSeen.add(w);
            return false;
        }
        // No skull nearby now — done only if we had actually seen it here.
        return essenceSeen.contains(w);
    }

    /** True after an item/bat entity that was present at the waypoint has disappeared. */
    private boolean entityPickedUp(BlockPos w, Vec3 eye, Level level, Class<? extends net.minecraft.world.entity.Entity> type) {
        if (level == null || tooFar(w, eye)) return false;
        AABB probe = new AABB(w).inflate(ENTITY_PROBE);
        boolean present = !level.getEntitiesOfClass(type, probe).isEmpty();
        if (present) {
            armed.add(w);
            return false;
        }
        // Gone now — done only if we had actually seen it here (avoids clearing
        // items that were never rendered as an entity, e.g. picked up by a mate).
        return armed.contains(w);
    }

    /** Finds a block matching {@code pred} within ±1 of {@code center}, or null. */
    private static BlockPos findBlock(Level level, BlockPos center, java.util.function.Predicate<BlockState> pred) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (pred.test(level.getBlockState(p))) return p.immutable();
                }
            }
        }
        return null;
    }

    private static boolean near(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= 1
            && Math.abs(a.getY() - b.getY()) <= 1
            && Math.abs(a.getZ() - b.getZ()) <= 1;
    }

    /** Called when the player right-clicks a block (chest/lever/essence/redstone). */
    public void onBlockInteract(BlockPos pos) {
        if (pos != null) {
            lastInteractPos = pos.immutable();
            interacted.add(lastInteractPos);
        }
    }

    /**
     * Redstone keys announce themselves in chat; a locked chest reappears. Both are
     * handled from the client chat hook.
     */
    public void handleChatMessage(String rawMessage, DungeonRoomDetector detector) {
        if (rawMessage == null) return;
        String lower = rawMessage.toLowerCase(Locale.ROOT);

        if (lower.contains("that chest is locked")) {
            // The chest we just clicked is locked, not opened — un-mark it so it shows again.
            if (lastInteractPos != null) {
                done.remove(lastInteractPos);
                interacted.remove(lastInteractPos);
            }
            return;
        }

        // Shift-clicking a lever activates the mechanism without flipping its POWERED
        // state, so the block-state watcher never fires. This chat line is the signal
        // that a lever secret opened something — clear the lever we just clicked.
        if (lower.contains("you hear the sound of something opening") && detector != null) {
            clearNearestLever(detector);
            return;
        }

        if (lower.contains("found a secret redstone key") && detector != null) {
            detector.currentRoom().ifPresent(room -> {
                Map<SecretType, List<BlockPos>> secrets = SecretDatabase.forRoom(room.name());
                List<BlockPos> redstone = secrets.get(SecretType.REDSTONE);
                if (redstone != null) {
                    for (BlockPos rel : redstone) done.add(detector.relativeToWorld(room, rel));
                }
            });
        }
    }

    /** Marks the lever waypoint nearest to the last interacted block (or player) as done. */
    private void clearNearestLever(DungeonRoomDetector detector) {
        detector.currentRoom().ifPresent(room -> {
            List<BlockPos> levers = SecretDatabase.forRoom(room.name()).get(SecretType.LEVER);
            if (levers == null || levers.isEmpty()) return;
            Minecraft mc = Minecraft.getInstance();
            BlockPos ref = lastInteractPos;
            if (ref == null && mc.player != null) ref = mc.player.blockPosition();
            if (ref == null) return;

            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos rel : levers) {
                BlockPos w = detector.relativeToWorld(room, rel);
                double dist = w.distSqr(ref);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = w;
                }
            }
            if (best != null) done.add(best);
        });
    }

    public void reset() {
        done.clear();
        interacted.clear();
        armed.clear();
        essenceSeen.clear();
        leverBaseline.clear();
        lastInteractPos = null;
    }
}
