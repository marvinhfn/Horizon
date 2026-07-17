package de.horizon.feature.dungeon.puzzle;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates all dungeon puzzle solvers.
 * Hooked into: tick, chat messages, block changes, and world rendering.
 */
public final class PuzzleSolverService {
    private final BlazeSolver        blaze       = new BlazeSolver();
    private final BoulderSolver      boulder     = new BoulderSolver();
    private final CreeperBeamsSolver beams       = new CreeperBeamsSolver();
    private final IceFillSolver      iceFill     = new IceFillSolver();
    private final IcePathSolver      icePath     = new IcePathSolver();
    private final QuizSolver         quiz        = new QuizSolver();
    private final ThreeWeidosSolver  weirdos     = new ThreeWeidosSolver();
    private final TicTacToeSolver    ttt         = new TicTacToeSolver();
    private final WaterSolver        water       = new WaterSolver();
    private final TeleportMazeSolver maze        = new TeleportMazeSolver();

    private String lastRoomName = null;
    // Debounce: don't reset solvers just because the room scanner temporarily loses the room
    private int roomNullTicks = 0;
    private static final int ROOM_NULL_THRESHOLD = 60; // 3 seconds

    public void tick(Minecraft mc, DungeonStateService state, DungeonRoomDetector roomDetector, HorizonConfig config) {
        if (!config.isPuzzleSolverEnabled()) return;
        if (!state.isInDungeon() || state.isInBoss()) {
            if (lastRoomName != null) resetAll();
            blaze.reset();
            return;
        }

        // Entity-based solvers tick every frame regardless of room detection
        blaze.tick(mc);
        icePath.tick(mc);
        String lowerRoom = lastRoomName != null ? lastRoomName.toLowerCase(Locale.ROOT) : null;
        if ("tic tac toe".equals(lowerRoom)) ttt.tick(mc);
        if ("water board".equals(lowerRoom)) water.tick(mc);
        if ("ice fill".equals(lowerRoom)) iceFill.tick(mc);
        // Water board auto-detection: tick even when room name doesn't match
        if (water.hasSolution() && !"water board".equals(lowerRoom)) water.tick(mc);

        Optional<DetectedDungeonRoom> roomOpt = roomDetector.currentRoom();
        String roomName = roomOpt.map(r -> r.name()).orElse(null);

        if (roomName != null) {
            roomNullTicks = 0;
            if (!Objects.equals(roomName, lastRoomName)) {
                boulder.reset(); beams.reset(); iceFill.reset(); icePath.reset(); quiz.reset();
                weirdos.reset(); ttt.reset(); maze.reset();
                // Only reset water if entering a genuinely different room (not a scanner glitch)
                if (!water.hasSolution()) water.reset();
                lastRoomName = roomName;
                onRoomEnter(roomOpt.get(), roomDetector, mc, config);
            }
            // Fallback: try auto-detecting water board for any detected room
            // (in case room hash doesn't match "Water Board" in rooms.json)
            if (!"water board".equals(lowerRoom) && !water.hasSolution()) {
                water.tryAutoDetect(roomOpt.get(), roomDetector, mc);
            }
        } else {
            roomNullTicks++;
            if (roomNullTicks >= ROOM_NULL_THRESHOLD) {
                if (lastRoomName != null) resetAllExceptWater();
            }
        }
    }

    private void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector, Minecraft mc, HorizonConfig config) {
        String name = room.name().toLowerCase(Locale.ROOT);
        switch (name) {
            case "higher blaze", "lower blaze", "blaze" -> {
                blaze.reset();
                blaze.detectPlatform(mc, room.origin());
            }
            case "boulder"         -> boulder.onRoomEnter(room, detector, mc);
            case "creeper beams"   -> beams.onRoomEnter(room, detector, mc);
            case "ice fill"        -> iceFill.onRoomEnter(room, detector, mc);
            case "ice path"        -> icePath.onRoomEnter(room, detector);
            case "quiz"            -> quiz.onRoomEnter(room, detector);
            case "three weirdos"   -> weirdos.onRoomEnter();
            case "tic tac toe"     -> ttt.onRoomEnter(room, detector);
            case "water board"     -> water.onRoomEnter(room, detector, mc);
            case "teleport maze"   -> maze.onRoomEnter(room, detector);
        }
    }

    public void handleChatMessage(String raw, Minecraft mc) {
        quiz.onChatMessage(raw, mc);
        weirdos.onChatMessage(raw, mc);
    }

    /** Returns a colored Component if this is a quiz answer option, null otherwise. */
    public Component colorQuizOption(String raw) {
        return quiz.colorQuizOption(raw);
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config, Minecraft mc) {
        if (!config.isPuzzleSolverEnabled()) return;
        int style = config.getPuzzleSolverStyle();
        // Entity-based solvers always render
        blaze.renderWorld(ctx, style, mc);
        icePath.renderWorld(ctx, style, mc);
        // Water board always renders if it has a solution (handles auto-detection fallback)
        if (water.hasSolution()) water.renderWorld(ctx, style);
        if (lastRoomName == null) return;
        switch (lastRoomName.toLowerCase(Locale.ROOT)) {
            case "boulder"        -> boulder.renderWorld(ctx, style);
            case "creeper beams"  -> beams.renderWorld(ctx, style);
            case "ice fill"       -> iceFill.renderWorld(ctx, style);
            case "quiz"           -> quiz.renderWorld(ctx, style);
            case "three weirdos"  -> weirdos.renderWorld(ctx, style);
            case "tic tac toe"    -> ttt.renderWorld(ctx, style);
            case "teleport maze"  -> maze.renderWorld(ctx, style);
        }
    }

    /** Called when player clicks a block — used by Water Board lever tracking. */
    public void onBlockInteract(BlockPos pos) {
        if (water.hasSolution()) {
            water.onLeverClick(pos, pos.getY());
        }
    }

    /** Called on block state change — used by Creeper Beams and Boulder. */
    public void onBlockChange(BlockPos world, Minecraft mc) {
        beams.onBlockChange(world, mc);
        if ("boulder".equals(lastRoomName != null ? lastRoomName.toLowerCase(Locale.ROOT) : null)) {
            boulder.onBlockChange(world);
        }
    }

    /** Called when player is teleported (for Teleport Maze). */
    public void onTeleport(double newX, double newZ, double oldX, double oldZ, float yaw) {
        maze.onTeleport(newX, newZ, oldX, oldZ, yaw);
    }

    public TeleportMazeSolver getTeleportMazeSolver() {
        return maze;
    }

    private void resetAll() {
        boulder.reset(); beams.reset(); iceFill.reset(); icePath.reset(); quiz.reset();
        weirdos.reset(); ttt.reset(); water.reset(); maze.reset();
        lastRoomName = null;
        roomNullTicks = 0;
    }

    private void resetAllExceptWater() {
        boulder.reset(); beams.reset(); iceFill.reset(); icePath.reset(); quiz.reset();
        weirdos.reset(); ttt.reset(); maze.reset();
        lastRoomName = null;
        roomNullTicks = 0;
    }
}
