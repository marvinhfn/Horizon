package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.concurrent.CopyOnWriteArrayList;

import static de.horizon.feature.dungeon.puzzle.DungeonRenderUtil.drawBox;

/**
 * Simon Says solver for F7 Phase 3 (Goldor).
 * Uses block update events instead of polling.
 * Sea lanterns appear at x=111 (grid), buttons to click at x=110 (one west).
 * Start button at (110, 121, 91).
 */
public final class SimonSaysService {
    private static final int COLOR_FIRST  = 0x8800FF44;
    private static final int COLOR_SECOND = 0x88FFAA00;
    private static final int COLOR_OTHER  = 0x88FF4444;

    private final CopyOnWriteArrayList<BlockPos> solution = new CopyOnWriteArrayList<>();
    private boolean wasStartButtonLast = false;
    private boolean hasButtons = false;
    private int solutionTotal = 0;
    private int startClicks = 0;

    private static boolean isValidButtonLocation(BlockPos pos) {
        return pos.getY() >= 120 && pos.getY() <= 123 && pos.getZ() >= 92 && pos.getZ() <= 95;
    }

    /**
     * Called from block update events (single block or section update).
     * Detects sea lanterns appearing on the grid at x=111 → adds button pos (x=110) to solution.
     * Detects grid reset: air blocks at x=110 in valid positions → clears solution.
     */
    public void onBlockUpdate(BlockPos pos, BlockState state) {
        // Sea lantern at x=111 → new solution entry
        if (state.is(Blocks.SEA_LANTERN) && pos.getX() == 111 && isValidButtonLocation(pos)) {
            BlockPos button = pos.west(); // x=110
            if (solution.isEmpty() || !solution.get(solution.size() - 1).equals(button)) {
                solution.add(button);
            }
        }
    }

    /**
     * Called from section block update to detect grid reset.
     * If 16 air blocks appear at x=110 in valid locations, the grid was cleared.
     */
    public void onSectionUpdate(BlockPos pos, BlockState state) {
        // Track individual updates via onBlockUpdate
        onBlockUpdate(pos, state);
    }

    /**
     * Call when a full section update arrives with bulk changes.
     * Pass air block count at x=110 in valid positions.
     */
    public void onSectionReset(int airCountAtX110) {
        if (airCountAtX110 >= 16) {
            solution.clear();
            startClicks = 0;
        }
    }

    public void tick(Minecraft mc, DungeonStateService state, HorizonConfig config) {
        if (!config.isSimonSaysEnabled()) return;
        if (!state.isInDungeon() || !state.isInBoss() || mc.level == null) return;

        // Check if buttons are present (stone button at grid start)
        BlockPos checkPos = new BlockPos(110, 120, 92);
        if (mc.level.getChunkSource().hasChunk(checkPos.getX() >> 4, checkPos.getZ() >> 4)) {
            BlockState bs = mc.level.getBlockState(checkPos);
            if (bs.is(Blocks.STONE_BUTTON)) {
                if (wasStartButtonLast) {
                    wasStartButtonLast = false;
                    // After start button press, keep only some solution entries based on total
                    int kept = switch (solution.size()) {
                        case 0 -> 0;
                        case 1 -> 1;
                        default -> {
                            if (solution.size() <= 3) yield 2;
                            else if (solution.size() <= 6) yield 3;
                            else if (solution.size() <= 9) yield 4;
                            else yield 5;
                        }
                    };
                    while (solution.size() > kept) {
                        solution.remove(0);
                    }
                }
                if (!hasButtons) solutionTotal = solution.size();
                hasButtons = true;
            } else {
                hasButtons = false;
            }
        }
    }

    public void handleChatMessage(String raw) {
        String lower = raw.toLowerCase(java.util.Locale.ROOT)
            .replaceAll("(?i)\u00a7[0-9a-fk-or]", "");
        if (lower.contains("goldor: who dares trespass")) {
            reset();
        }
    }

    /**
     * Called when player clicks a block. Returns true if the click should be blocked.
     */
    public boolean onBlockInteract(BlockPos pos, HorizonConfig config) {
        if (!config.isSimonSaysEnabled()) return false;
        if (pos.getX() != 110) {
            startClicks = 0;
            return false;
        }

        // Start button
        wasStartButtonLast = pos.getY() == 121 && pos.getZ() == 91;
        if (wasStartButtonLast) {
            startClicks++;
        } else {
            startClicks = 0;
        }

        if (solution.isEmpty()) return false;
        if (!isValidButtonLocation(pos)) return false;

        // Correct button: remove from solution
        if (!solution.isEmpty() && solution.get(0).equals(pos)) {
            solution.remove(0);
            return false;
        }

        // Wrong button: block if configured
        if (config.isSimonSaysBlockWrongClicks()) {
            return true;
        }

        // Not blocking: skip ahead in solution
        while (!solution.isEmpty()) {
            BlockPos next = solution.get(0);
            if (next.equals(pos)) break;
            solution.remove(0);
        }
        if (!solution.isEmpty()) solution.remove(0);
        return false;
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isSimonSaysEnabled()) return;
        if (solution.isEmpty()) return;

        for (int i = 0; i < solution.size(); i++) {
            BlockPos pos = solution.get(i);
            int color = i == 0 ? COLOR_FIRST : i == 1 ? COLOR_SECOND : COLOR_OTHER;
            // Button shape: slightly inset from full block
            AABB box = new AABB(
                pos.getX() + 0.875, pos.getY() + 0.375, pos.getZ() + 0.3125,
                pos.getX() + 1.0,   pos.getY() + 0.625, pos.getZ() + 0.6875
            );
            drawBox(ctx, box, color, 2, false);
        }
    }

    public void reset() {
        solution.clear();
        wasStartButtonLast = false;
        hasButtons = false;
        solutionTotal = 0;
        startClicks = 0;
    }
}
