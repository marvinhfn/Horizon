package de.horizon.feature.dungeon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.horizon.config.HorizonConfig;
import de.horizon.render.PillarboxState;
import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import de.horizon.hud.HudStyle;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DungeonSolverOverlay {
    private static final Pattern STARTS_WITH_PATTERN = Pattern.compile("starts with ['\"]?([a-z0-9])", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLAZE_HEALTH_PATTERN = Pattern.compile("Blaze\\s+[\\d,]+/([\\d,]+)", Pattern.CASE_INSENSITIVE);
    private static final int PANEL = 0xD8090E14;
    private static final int CURRENT = 0xAA9C4DFF;
    private static final int NEXT = 0x88FF66D9;
    private static final int VISITED = 0x88FF4D4D;
    private static final int GOOD = CURRENT;
    private static final int WARN = NEXT;
    private static final int BAD = VISITED;
    private static final List<RelativePair> CREEPER_BEAM_PAIRS = loadCreeperBeamPairs();
    private static final Map<String, List<int[]>> BOULDER_SOLUTIONS = loadBoulderSolutions();
    private static final WaterBoardData WATER_BOARD_DATA = loadWaterBoardData();
    private static final IceFillData ICE_FILL_DATA = loadIceFillData();
    private static final List<BlockPos> TELEPORT_MAZE_PADS = List.of(
        new BlockPos(4, 69, 12), new BlockPos(4, 69, 6), new BlockPos(10, 69, 12), new BlockPos(10, 69, 6),
        new BlockPos(4, 69, 20), new BlockPos(4, 69, 14), new BlockPos(10, 69, 20), new BlockPos(10, 69, 14),
        new BlockPos(4, 69, 28), new BlockPos(4, 69, 22), new BlockPos(10, 69, 28), new BlockPos(10, 69, 22),
        new BlockPos(12, 69, 28), new BlockPos(12, 69, 22), new BlockPos(18, 69, 28), new BlockPos(18, 69, 22),
        new BlockPos(20, 69, 28), new BlockPos(20, 69, 22), new BlockPos(26, 69, 28), new BlockPos(26, 69, 22),
        new BlockPos(26, 69, 20), new BlockPos(26, 69, 14), new BlockPos(20, 69, 20), new BlockPos(20, 69, 14),
        new BlockPos(26, 69, 12), new BlockPos(26, 69, 6), new BlockPos(20, 69, 12), new BlockPos(20, 69, 6),
        new BlockPos(15, 69, 14), new BlockPos(15, 69, 12)
    );
    private final Map<String, List<String>> quizAnswers = new LinkedHashMap<>();
    private final Set<Integer> glowingEntities = new HashSet<>();
    private final List<String> solverLines = new ArrayList<>();
    private final List<WorldBox> worldBoxes = new ArrayList<>();
    private RenderType cachedFilledRenderType;
    private boolean filledRenderUnavailable;
    private RenderType cachedLineRenderType;
    private boolean lineRenderUnavailable;
    private Field screenXField;
    private Field screenYField;
    private boolean screenPositionReflectionFailed;
    private BlazeTarget currentBlazeTarget;
    private List<BlazeTarget> cachedBlazeTargets = List.of();
    private long cachedBlazeTargetsTick;
    private AABB cachedBlazeBox;
    private long cachedBlazeBoxTick;
    private String activeBoulderLayout = "";
    private BlockPos activeBoulderOrigin = BlockPos.ZERO;
    private List<int[]> activeBoulderSolution = List.of();
    private int activeBoulderStep;
    private BlockPos activeTeleportOrigin = BlockPos.ZERO;
    private final Set<BlockPos> visitedTeleportPads = new HashSet<>();
    private String lastQuizAnswer = "";
    private List<String> lastQuizAnswers = List.of();
    private String lastQuizOption = "";
    private String lastWeirdosHint = "";
    private String correctWeirdoNpc = "";
    private String wrongWeirdoNpc = "";
    private int chatHintTicks;
    private int solverTick;

    public DungeonSolverOverlay() {
        addQuizAnswer("how many total fairy souls", "267 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in the hub", "80 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in hub", "80 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in dungeon hub", "7 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in spider", "19 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in the end", "12 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in deep caverns", "21 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in gold mine", "12 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in the park", "12 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in crimson isle", "29 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in the farming islands", "20 Fairy Souls");
        addQuizAnswer("how many fairy souls are there in jerry", "5 Fairy Souls");
        addQuizAnswer("what is the status of scarf", "Apprentice Necromancer");
        addQuizAnswer("what is the status of bonzo", "New Necromancer");
        addQuizAnswer("what is the status of thorn", "Shaman Necromancer");
        addQuizAnswer("what is the status of the watcher", "Stalker");
        addQuizAnswer("what is the status of sadan", "Necromancer Lord");
        addQuizAnswer("what is the status of livid", "Master Necromancer");
        addQuizAnswer("what is the status of the professor", "Professor");
        addQuizAnswer("what is the status of maxor", "The Wither Lords");
        addQuizAnswer("what is the name of the lady of the nether", "Elle");
        addQuizAnswer("which villager in the village gives you a rogue sword", "Jamie");
        addQuizAnswer("what is the name of rick's brother", "Pat");
        addQuizAnswer("which brother is on the spider", "Rick");
        addQuizAnswer("what is the name of the person that upgrades pets", "Kat");
        addQuizAnswer("what is the name of the vendor in the hub who sells stained", "Wool Weaver");
        addQuizAnswer("which of these monsters only spawns at night", "Zombie Villager", "Ghast");
        addQuizAnswer("which of these enemies does not spawn in the spider", "Zombie Spider", "Wither Skeleton", "Dashing Spooder", "Broodfather", "Night Spider");
        addQuizAnswer("which of these is not a dragon in the end", "Zoomer Dragon", "Weak Dragon", "Stonk Dragon", "Boomer Dragon", "Booger Dragon", "Older Dragon", "Elder Dragon", "Stable Dragon", "Professor Dragon");
    }

    public void handleChatMessage(String rawMessage) {
        String cleaned = clean(rawMessage);
        Matcher npcMatcher = Pattern.compile("^\\[NPC] ([^:]+): (.+)$").matcher(cleaned);
        if (npcMatcher.find()) {
            handleNpcPuzzleLine(npcMatcher.group(1), npcMatcher.group(2));
        }

        handleQuizOption(cleaned);
        String message = cleaned.toLowerCase(Locale.ROOT);
        if (message.equals("what skyblock year is it?") || message.contains("what skyblock year is it")) {
            int year = (int) (((System.currentTimeMillis() / 1000L) - 1560276000L) / 446400L) + 1;
            lastQuizAnswer = "Year " + year;
            lastQuizAnswers = List.of(lastQuizAnswer);
            lastQuizOption = "";
            chatHintTicks = 20 * 30;
            return;
        }
        for (Map.Entry<String, List<String>> entry : quizAnswers.entrySet()) {
            if (message.contains(entry.getKey())) {
                lastQuizAnswers = entry.getValue();
                lastQuizAnswer = entry.getValue().get(0);
                lastQuizOption = "";
                chatHintTicks = 20 * 30;
                return;
            }
        }

        if (message.contains("the reward is in my chest") || message.contains("at least one of them is lying") || message.contains("both of them are telling the truth")) {
            lastWeirdosHint = "Three Weirdos: Waehle die Chest des wahrheitsgemaessen NPCs.";
            chatHintTicks = 20 * 20;
        }
        if (message.contains("i always tell the truth")) {
            lastWeirdosHint = "Three Weirdos: Dieser NPC ist oft der sichere Kandidat, pruefe Widerspruch.";
            chatHintTicks = 20 * 20;
        }
    }

    public void tick(Minecraft client, HorizonConfig config, DungeonStateService dungeonState, DungeonRoomDetector roomDetector) {
        if (chatHintTicks > 0) {
            chatHintTicks--;
        }
        if (client == null || client.level == null || client.player == null || config == null) {
            clearGlowing(client);
            return;
        }
        if (dungeonState == null || !dungeonState.isInDungeon()) {
            clearGlowing(client);
            solverLines.clear();
            worldBoxes.clear();
            currentBlazeTarget = null;
            cachedBlazeBox = null;
            activeBoulderLayout = "";
            activeBoulderSolution = List.of();
            activeBoulderStep = 0;
            activeBoulderOrigin = BlockPos.ZERO;
            activeTeleportOrigin = BlockPos.ZERO;
            visitedTeleportPads.clear();
            return;
        }

        solverTick++;
        if (solverTick % 3 != 0) {
            return;
        }

        clearGlowing(client);
        solverLines.clear();
        worldBoxes.clear();
        currentBlazeTarget = null;
        DetectedDungeonRoom currentRoom = roomDetector == null ? null : roomDetector.currentRoom().orElse(null);
        if (currentRoom != null) {
            solverLines.add("Room: " + currentRoom.name() + " (" + currentRoom.confidence() + "%)");
        }
        if (config.isPuzzleBlazeEnabled() && (shouldRunRoomSolver(currentRoom, "Blaze") || cachedBlazeBox != null && solverTick - cachedBlazeBoxTick <= 40)) {
            renderBlazeWorldSolver(client, currentRoom);
        }
        if (config.isPuzzleQuizEnabled() && shouldRunRoomSolver(currentRoom, "Quiz")) {
            renderQuizWorldSolver(client);
        }
        if (config.isPuzzleThreeWeirdosEnabled() && shouldRunRoomSolver(currentRoom, "Three Weirdos")) {
            renderWeirdosWorldSolver(client);
        }
        if (config.isPuzzleCreeperBeamsEnabled() && shouldRunRoomSolver(currentRoom, "Creeper Beams")) {
            renderCreeperBeamsWorldSolver(client, currentRoom, roomDetector);
        }
        if (config.isPuzzleBoulderEnabled() && shouldRunRoomSolver(currentRoom, "Boulder")) {
            renderBoulderWorldSolver(client, currentRoom, roomDetector);
        }
        if (config.isPuzzleIcePathEnabled() && shouldRunRoomSolver(currentRoom, "Ice Path")) {
            renderIcePathWorldSolver(client);
        }
        if (config.isPuzzleIceFillEnabled() && shouldRunRoomSolver(currentRoom, "Ice Fill")) {
            renderIceFillWorldSolver(client, currentRoom, roomDetector);
        }
        if (config.isPuzzleTeleportMazeEnabled() && shouldRunRoomSolver(currentRoom, "Teleport Maze")) {
            renderTeleportMazeWorldSolver(client, currentRoom, roomDetector);
        }
        if (config.isPuzzleWaterBoardEnabled() && shouldRunRoomSolver(currentRoom, "Water Board")) {
            renderWaterBoardWorldSolver(client, currentRoom, roomDetector);
        }
    }

    public void handleBlockInteract(BlockPos pos, DungeonStateService dungeonState, DungeonRoomDetector roomDetector) {
        if (pos == null || dungeonState == null || !dungeonState.isInDungeon() || roomDetector == null) {
            return;
        }
        DetectedDungeonRoom currentRoom = roomDetector.currentRoom().orElse(null);
        if (currentRoom == null) {
            return;
        }
        if (currentRoom.isPuzzle("Boulder")) {
            advanceBoulderStep(pos, currentRoom, roomDetector);
        }
        if (currentRoom.isPuzzle("Teleport Maze")) {
            visitedTeleportPads.add(pos.immutable());
        }
    }

    public void renderWorldHud(GuiGraphicsExtractor context, Minecraft client, HorizonConfig config) {
        // World puzzle solvers intentionally render with particles/glow in tick(), not as a generic HUD.
    }

    public void renderWorld(LevelRenderContext context) {
        if (worldBoxes.isEmpty() || context == null || context.levelState() == null || context.levelState().cameraRenderState == null || context.bufferSource() == null) {
            return;
        }
        RenderType fillLayer = filledRenderType();
        if (fillLayer != null) {
            VertexConsumer consumer = context.bufferSource().getBuffer(fillLayer);
            if (consumer != null) {
                for (WorldBox box : worldBoxes) {
                    drawFilledBoxCompat(context, consumer, box.box(), box.color());
                }
                return;
            }
        }

        RenderType lineLayer = lineRenderType();
        if (lineLayer == null) {
            return;
        }
        VertexConsumer consumer = context.bufferSource().getBuffer(lineLayer);
        if (consumer == null) {
            return;
        }
        for (WorldBox box : worldBoxes) {
            drawOutlineCompat(context, consumer, box.box(), box.color());
        }
    }

    private RenderType filledRenderType() {
        if (cachedFilledRenderType != null) {
            return cachedFilledRenderType;
        }
        if (filledRenderUnavailable) {
            return null;
        }
        try {
            cachedFilledRenderType = (RenderType) Class.forName("net.minecraft.client.render.RenderTypes").getMethod("debugFilledBox").invoke(null);
            return cachedFilledRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            cachedFilledRenderType = (RenderType) Class.forName("net.minecraft.class_12249").getMethod("method_76019").invoke(null);
            return cachedFilledRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            cachedFilledRenderType = (RenderType) RenderType.class.getMethod("getDebugFilledBox").invoke(null);
            return cachedFilledRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            cachedFilledRenderType = (RenderType) RenderType.class.getMethod("method_49047").invoke(null);
            return cachedFilledRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            java.lang.reflect.Field field = Class.forName("net.minecraft.client.render.RenderTypes").getDeclaredField("DEBUG_FILLED_BOX");
            field.setAccessible(true);
            cachedFilledRenderType = (RenderType) field.get(null);
            return cachedFilledRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            java.lang.reflect.Field field = RenderType.class.getDeclaredField("DEBUG_FILLED_BOX");
            field.setAccessible(true);
            cachedFilledRenderType = (RenderType) field.get(null);
            return cachedFilledRenderType;
        } catch (ReflectiveOperationException ignored) {
            filledRenderUnavailable = true;
            return null;
        }
    }

    private RenderType lineRenderType() {
        if (cachedLineRenderType != null) {
            return cachedLineRenderType;
        }
        if (lineRenderUnavailable) {
            return null;
        }
        try {
            cachedLineRenderType = (RenderType) Class.forName("net.minecraft.client.render.RenderTypes").getMethod("lines").invoke(null);
            return cachedLineRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            cachedLineRenderType = (RenderType) Class.forName("net.minecraft.class_12249").getMethod("method_76015").invoke(null);
            return cachedLineRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            cachedLineRenderType = (RenderType) RenderType.class.getMethod("getLines").invoke(null);
            return cachedLineRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            cachedLineRenderType = (RenderType) RenderType.class.getMethod("method_23594").invoke(null);
            return cachedLineRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            java.lang.reflect.Field field = Class.forName("net.minecraft.client.render.RenderTypes").getDeclaredField("LINES");
            field.setAccessible(true);
            cachedLineRenderType = (RenderType) field.get(null);
            return cachedLineRenderType;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            java.lang.reflect.Field field = RenderType.class.getDeclaredField("LINES");
            field.setAccessible(true);
            cachedLineRenderType = (RenderType) field.get(null);
            return cachedLineRenderType;
        } catch (ReflectiveOperationException ignored) {
            lineRenderUnavailable = true;
            return null;
        }
    }

    private void drawFilledBoxCompat(LevelRenderContext context, VertexConsumer consumer, AABB worldBox, int color) {
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        double minX = worldBox.minX - context.levelState().cameraRenderState.pos.x;
        double minY = worldBox.minY - context.levelState().cameraRenderState.pos.y;
        double minZ = worldBox.minZ - context.levelState().cameraRenderState.pos.z;
        double maxX = worldBox.maxX - context.levelState().cameraRenderState.pos.x;
        double maxY = worldBox.maxY - context.levelState().cameraRenderState.pos.y;
        double maxZ = worldBox.maxZ - context.levelState().cameraRenderState.pos.z;
        try {
            ShapeRenderer.class
                .getMethod("drawFilledBox", context.poseStack().getClass(), VertexConsumer.class, double.class, double.class, double.class, double.class, double.class, double.class, float.class, float.class, float.class, float.class)
                .invoke(null, context.poseStack(), consumer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Class.forName("net.minecraft.class_9974")
                .getMethod("method_62300", context.poseStack().getClass(), VertexConsumer.class, double.class, double.class, double.class, double.class, double.class, double.class, float.class, float.class, float.class, float.class)
                .invoke(null, context.poseStack(), consumer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void drawOutlineCompat(LevelRenderContext context, VertexConsumer consumer, AABB worldBox, int color) {
        double minX = worldBox.minX - context.levelState().cameraRenderState.pos.x;
        double minY = worldBox.minY - context.levelState().cameraRenderState.pos.y;
        double minZ = worldBox.minZ - context.levelState().cameraRenderState.pos.z;
        double maxX = worldBox.maxX - context.levelState().cameraRenderState.pos.x;
        double maxY = worldBox.maxY - context.levelState().cameraRenderState.pos.y;
        double maxZ = worldBox.maxZ - context.levelState().cameraRenderState.pos.z;
        try {
            ShapeRenderer.class
                .getMethod("drawBox", context.poseStack().getClass(), VertexConsumer.class, double.class, double.class, double.class, double.class, double.class, double.class, float.class, float.class, float.class, float.class)
                .invoke(null, context.poseStack(), consumer, minX, minY, minZ, maxX, maxY, maxZ,
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F,
                    ((color >> 24) & 0xFF) / 255.0F);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Class.forName("net.minecraft.class_9974")
                .getMethod("method_62297", context.poseStack().getClass(), VertexConsumer.class, double.class, double.class, double.class, double.class, double.class, double.class, float.class, float.class, float.class, float.class)
                .invoke(null, context.poseStack(), consumer, minX, minY, minZ, maxX, maxY, maxZ,
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F,
                    ((color >> 24) & 0xFF) / 255.0F);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public void renderHudOverlay(GuiGraphicsExtractor context, Minecraft client, HorizonConfig config) {
        if (client == null || config == null || client.options.hideGui || !config.isSolverDebugHudEnabled() || solverLines.isEmpty()) {
            return;
        }

        int width = 230;
        int height = 24 + solverLines.size() * 13;
        int x = client.getWindow().getGuiScaledWidth() - 2 * PillarboxState.scaledBarWidth() - width - 14;
        int y = 14;
        context.fill(x, y, x + width, y + height, HudStyle.panel());
        context.outline(x, y, width, height, HudStyle.border());
        context.text(client.font, Component.literal("Horizon Solver"), x + 10, y + 8, HudStyle.accent());
        for (int index = 0; index < solverLines.size(); index++) {
            context.text(client.font, Component.literal(solverLines.get(index)), x + 10, y + 22 + index * 13, HudStyle.muted());
        }
    }

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor context, HorizonConfig config, DungeonStateService dungeonState, DungeonRoomDetector roomDetector) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || dungeonState == null || !dungeonState.isInDungeon()) {
            return;
        }

        String title = screen.getTitle().getString();
        String normalized = title.toLowerCase(Locale.ROOT);
        SolveResult result = solve(screen, normalized, config, roomDetector == null ? null : roomDetector.currentRoom().orElse(null));
        if (result == null) {
            return;
        }

        drawHighlights(screen, context, result);
        drawPanel(screen, context, client, result);
    }

    private SolveResult solve(AbstractContainerScreen<?> screen, String title, HorizonConfig config, DetectedDungeonRoom currentRoom) {
        List<Slot> containerSlots = containerSlots(screen);
        if (title.contains("correct all") && config.isTerminalCorrectAllEnabled()) {
            return correctAll(containerSlots);
        }
        if (title.contains("click in order") && config.isTerminalClickInOrderEnabled()) {
            return clickInOrder(containerSlots);
        }
        if (title.contains("starts with") && config.isTerminalStartsWithEnabled()) {
            return startsWith(containerSlots, title);
        }
        if (title.contains("select all") && config.isTerminalSelectAllColorEnabled()) {
            return selectAllColor(containerSlots, title);
        }
        if (title.contains("same color") && config.isTerminalSameColorEnabled()) {
            return sameColor(containerSlots);
        }
        if (title.contains("tic tac toe") && config.isPuzzleTicTacToeEnabled()) {
            return ticTacToe(containerSlots);
        }
        if ((title.contains("maze") || title.contains("navigate")) && config.isTerminalNavigateMazeEnabled()) {
            return navigateMaze(containerSlots);
        }
        if (title.contains("ice fill") && config.isPuzzleIceFillEnabled() && currentRoom != null && currentRoom.isPuzzle("Ice Fill")) {
            return iceFill(containerSlots);
        }

        String puzzleHint = puzzleHint(title, config, currentRoom);
        if (puzzleHint != null) {
            return new SolveResult("Puzzle", puzzleHint, List.of());
        }
        return null;
    }

    private SolveResult correctAll(List<Slot> slots) {
        List<Highlight> highlights = new ArrayList<>();
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (stack.is(Items.GREEN_STAINED_GLASS_PANE) || stack.is(Items.LIME_STAINED_GLASS_PANE)) {
                highlights.add(new Highlight(slot, GOOD, "Click"));
            }
        }
        return new SolveResult("Correct All", highlights.isEmpty() ? "Keine gruenen Panes gefunden." : "Klicke alle gruen markierten Panes.", highlights);
    }

    private SolveResult clickInOrder(List<Slot> slots) {
        List<NumberedSlot> numbered = new ArrayList<>();
        for (Slot slot : slots) {
            String text = stackText(slot.getItem());
            Integer number = firstInteger(text);
            if (number != null) {
                numbered.add(new NumberedSlot(slot, number));
            }
        }
        numbered.sort(Comparator.comparingInt(NumberedSlot::number));
        List<Highlight> highlights = new ArrayList<>();
        int[] previewColors = {CURRENT, NEXT, 0x66FF66D9, 0x44FF66D9};
        for (int index = 0; index < Math.min(4, numbered.size()); index++) {
            highlights.add(new Highlight(numbered.get(index).slot(), previewColors[index], String.valueOf(index + 1)));
        }
        return new SolveResult("Click In Order", highlights.isEmpty() ? "Keine Zahlen erkannt." : "Jetzt hell markieren, die naechsten drei schwach als Vorschau.", highlights);
    }

    private SolveResult startsWith(List<Slot> slots, String title) {
        Matcher matcher = STARTS_WITH_PATTERN.matcher(title);
        if (!matcher.find()) {
            return new SolveResult("Starts With", "Startbuchstabe nicht erkannt.", List.of());
        }
        String prefix = matcher.group(1).toLowerCase(Locale.ROOT);
        List<Highlight> highlights = new ArrayList<>();
        for (Slot slot : slots) {
            String name = clean(slot.getItem().getHoverName().getString()).toLowerCase(Locale.ROOT);
            if (name.startsWith(prefix)) {
                highlights.add(new Highlight(slot, GOOD, prefix.toUpperCase(Locale.ROOT)));
            }
        }
        return new SolveResult("Starts With", highlights.isEmpty() ? "Keine passenden Items fuer " + prefix + "." : "Klicke Items mit " + prefix.toUpperCase(Locale.ROOT) + ".", highlights);
    }

    private SolveResult selectAllColor(List<Slot> slots, String title) {
        String targetColor = targetColor(title);
        List<Highlight> highlights = new ArrayList<>();
        for (Slot slot : slots) {
            String itemId = itemId(slot.getItem());
            if (targetColor != null && itemId.contains(targetColor)) {
                highlights.add(new Highlight(slot, GOOD, targetColor));
            }
        }
        return new SolveResult("Select All Color", highlights.isEmpty() ? "Zielfarbe nicht erkannt oder keine Treffer." : "Klicke alle " + targetColor + " Items.", highlights);
    }

    private SolveResult sameColor(List<Slot> slots) {
        String mostCommon = null;
        int bestCount = 0;
        for (Slot slot : slots) {
            String color = colorName(slot.getItem());
            if (color == null) {
                continue;
            }
            int count = 0;
            for (Slot other : slots) {
                if (color.equals(colorName(other.getItem()))) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                mostCommon = color;
            }
        }
        if (mostCommon == null) {
            return new SolveResult("Same Color", "Keine Farbe erkannt.", List.of());
        }
        List<Highlight> highlights = new ArrayList<>();
        for (Slot slot : slots) {
            if (!mostCommon.equals(colorName(slot.getItem())) && colorName(slot.getItem()) != null) {
                highlights.add(new Highlight(slot, CURRENT, "Change"));
            }
        }
        return new SolveResult("Same Color", "Ziel: " + mostCommon + ". Markierte Slots umstellen.", highlights);
    }

    private SolveResult ticTacToe(List<Slot> slots) {
        if (slots.size() < 9) {
            return new SolveResult("Tic Tac Toe", "Board nicht erkannt.", List.of());
        }
        List<Slot> boardSlots = slots.subList(0, Math.min(9, slots.size()));
        char[] board = new char[9];
        for (int index = 0; index < boardSlots.size(); index++) {
            String text = stackText(boardSlots.get(index).getItem()).toLowerCase(Locale.ROOT);
            String id = itemId(boardSlots.get(index).getItem());
            if (text.contains("x") || id.contains("red")) {
                board[index] = 'x';
            } else if (text.contains("o") || id.contains("green") || id.contains("lime")) {
                board[index] = 'o';
            } else {
                board[index] = ' ';
            }
        }

        char next = nextTicTacToeSide(board);
        int move = bestTicTacToeMove(board, next);
        if (move < 0 || move >= boardSlots.size()) {
            return new SolveResult("Tic Tac Toe", "Kein sicherer Zug erkannt.", List.of());
        }
        return new SolveResult("Tic Tac Toe", "Bester Zug fuer " + Character.toUpperCase(next) + " markiert.", List.of(new Highlight(boardSlots.get(move), CURRENT, "Move")));
    }

    private char nextTicTacToeSide(char[] board) {
        int x = 0;
        int o = 0;
        for (char cell : board) {
            if (cell == 'x') {
                x++;
            } else if (cell == 'o') {
                o++;
            }
        }
        return x <= o ? 'x' : 'o';
    }

    private int bestTicTacToeMove(char[] board, char side) {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;
        for (int index = 0; index < board.length; index++) {
            if (board[index] != ' ') {
                continue;
            }
            board[index] = side;
            int score = minimax(board, opposite(side), side);
            board[index] = ' ';
            if (score > bestScore) {
                bestScore = score;
                bestMove = index;
            }
        }
        return bestMove;
    }

    private int minimax(char[] board, char currentSide, char maximizingSide) {
        char winner = ticTacToeWinner(board);
        if (winner == maximizingSide) {
            return 10;
        }
        if (winner == opposite(maximizingSide)) {
            return -10;
        }
        if (isBoardFull(board)) {
            return 0;
        }

        int bestScore = currentSide == maximizingSide ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int index = 0; index < board.length; index++) {
            if (board[index] != ' ') {
                continue;
            }
            board[index] = currentSide;
            int score = minimax(board, opposite(currentSide), maximizingSide);
            board[index] = ' ';
            if (currentSide == maximizingSide) {
                bestScore = Math.max(bestScore, score);
            } else {
                bestScore = Math.min(bestScore, score);
            }
        }
        return bestScore;
    }

    private char ticTacToeWinner(char[] board) {
        int[][] lines = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
        };
        for (int[] line : lines) {
            if (board[line[0]] != ' ' && board[line[0]] == board[line[1]] && board[line[1]] == board[line[2]]) {
                return board[line[0]];
            }
        }
        return ' ';
    }

    private boolean isBoardFull(char[] board) {
        for (char cell : board) {
            if (cell == ' ') {
                return false;
            }
        }
        return true;
    }

    private char opposite(char side) {
        return side == 'x' ? 'o' : 'x';
    }

    private SolveResult navigateMaze(List<Slot> slots) {
        if (slots.isEmpty()) {
            return new SolveResult("Maze", "Maze-Board nicht erkannt.", List.of());
        }

        Grid grid = grid(slots);
        Slot start = null;
        Slot end = null;
        for (Slot slot : slots) {
            String id = itemId(slot.getItem());
            if (id.contains("lime") || id.contains("green")) {
                start = slot;
            }
            if (id.contains("red") || id.contains("orange")) {
                end = slot;
            }
        }
        if (start == null || end == null) {
            return new SolveResult("Maze", "Start/Ziel nicht erkannt. Gruene Route manuell folgen.", List.of());
        }

        List<Slot> path = shortestPath(start, end, grid, this::isMazeWalkable);
        if (path.isEmpty()) {
            return new SolveResult("Maze", "Kein sicherer Pfad erkannt.", List.of());
        }

        List<Highlight> highlights = new ArrayList<>();
        for (int index = 0; index < path.size(); index++) {
            highlights.add(new Highlight(path.get(index), index == 0 ? GOOD : WARN, index == 0 ? "Start" : String.valueOf(index)));
        }
        return new SolveResult("Maze", "Markierten Pfad vom Start zum Ziel nutzen.", highlights);
    }

    private SolveResult iceFill(List<Slot> slots) {
        List<Slot> walkable = new ArrayList<>();
        for (Slot slot : slots) {
            String id = itemId(slot.getItem());
            if (id.contains("ice") || id.contains("packed_ice") || id.contains("blue_ice") || id.contains("light_blue")) {
                walkable.add(slot);
            }
        }
        if (walkable.isEmpty()) {
            return new SolveResult("Ice Fill", "Ice-Board nicht erkannt.", List.of());
        }

        walkable.sort(Comparator.comparingInt((Slot slot) -> slot.y).thenComparingInt(slot -> slot.x));
        List<Slot> path = new ArrayList<>();
        int currentY = Integer.MIN_VALUE;
        List<Slot> row = new ArrayList<>();
        boolean reverse = false;
        for (Slot slot : walkable) {
            if (currentY != Integer.MIN_VALUE && slot.y != currentY) {
                appendRow(path, row, reverse);
                row.clear();
                reverse = !reverse;
            }
            currentY = slot.y;
            row.add(slot);
        }
        appendRow(path, row, reverse);

        List<Highlight> highlights = new ArrayList<>();
        for (int index = 0; index < path.size(); index++) {
            String label = index == 0 ? "1" : index == path.size() - 1 ? "End" : "";
            highlights.add(new Highlight(path.get(index), index == 0 ? GOOD : WARN, label));
        }
        return new SolveResult("Ice Fill", "Snake-Pfad ueber erkannte Eisfelder markiert.", highlights);
    }

    private void appendRow(List<Slot> path, List<Slot> row, boolean reverse) {
        row.sort(Comparator.comparingInt(slot -> slot.x));
        if (reverse) {
            for (int index = row.size() - 1; index >= 0; index--) {
                path.add(row.get(index));
            }
            return;
        }
        path.addAll(row);
    }

    private void handleNpcPuzzleLine(String npc, String message) {
        if (isCorrectWeirdoLine(message)) {
            correctWeirdoNpc = npc;
            wrongWeirdoNpc = "";
            lastWeirdosHint = "Three Weirdos: " + npc;
            chatHintTicks = 20 * 20;
            return;
        }
        if (isWrongWeirdoLine(message)) {
            wrongWeirdoNpc = npc;
            chatHintTicks = 20 * 20;
        }
    }

    private void handleQuizOption(String rawMessage) {
        if (lastQuizAnswers.isEmpty()) {
            return;
        }
        String trimmed = clean(rawMessage).trim();
        if (trimmed.length() < 2) {
            return;
        }

        String option = "";
        if (trimmed.startsWith("\u24D0") || trimmed.toLowerCase(Locale.ROOT).startsWith("a)")) {
            option = "A";
        } else if (trimmed.startsWith("\u24D1") || trimmed.toLowerCase(Locale.ROOT).startsWith("b)")) {
            option = "B";
        } else if (trimmed.startsWith("\u24D2") || trimmed.toLowerCase(Locale.ROOT).startsWith("c)")) {
            option = "C";
        }
        if (option.isBlank()) {
            return;
        }

        String normalized = trimmed.toLowerCase(Locale.ROOT);
        for (String answer : lastQuizAnswers) {
            if (normalized.endsWith(answer.toLowerCase(Locale.ROOT)) || normalized.contains(answer.toLowerCase(Locale.ROOT))) {
                lastQuizAnswer = answer;
                lastQuizOption = option;
                chatHintTicks = 20 * 30;
                return;
            }
        }
    }

    private boolean isCorrectWeirdoLine(String message) {
        String normalized = clean(message).toLowerCase(Locale.ROOT);
        return normalized.contains("the reward is not in my chest")
            || normalized.contains("my chest has the reward and i'm telling the truth")
            || normalized.contains("the reward isn't in any of our chests")
            || normalized.contains("both of them are telling the truth")
            || normalized.contains("we are all telling the truth");
    }

    private boolean isWrongWeirdoLine(String message) {
        String normalized = clean(message).toLowerCase(Locale.ROOT);
        return normalized.contains("the reward is in my chest")
            || normalized.contains("they are both lying")
            || normalized.contains("one of us is telling the truth")
            || normalized.contains("one of the others is lying");
    }

    private void renderBlazeWorldSolver(Minecraft client, DetectedDungeonRoom currentRoom) {
        List<BlazeTarget> targets = blazeTargets(client, currentRoom);
        if (targets.isEmpty() && !cachedBlazeTargets.isEmpty() && solverTick - cachedBlazeTargetsTick <= 30) {
            targets = cachedBlazeTargets.stream()
                .filter(target -> target.label() != null && !target.label().isRemoved())
                .toList();
        }
        if (targets.isEmpty()) {
            if (cachedBlazeBox != null && solverTick - cachedBlazeBoxTick <= 40) {
                worldBoxes.add(new WorldBox(cachedBlazeBox, CURRENT));
                solverLines.add("Blaze: letzter Trefferpunkt gehalten");
            }
            return;
        }
        cachedBlazeTargets = targets;
        cachedBlazeTargetsTick = solverTick;

        BlazeTarget target = targets.get(0);
        currentBlazeTarget = target;
        cachedBlazeBox = blazeRenderBox(target.label());
        cachedBlazeBoxTick = solverTick;
        target.label().setGlowingTag(true);
        glowingEntities.add(target.label().getId());
        worldBoxes.add(new WorldBox(blazeRenderBox(target.label()), CURRENT));
        if (targets.size() > 1) {
            worldBoxes.add(new WorldBox(blazeRenderBox(targets.get(1).label()), NEXT));
        }
        solverLines.add("Blaze: SHOOT marked target");
        solverLines.add("Aim: " + directionTo(client, target.label()));
    }

    private void renderQuizWorldSolver(Minecraft client) {
        if ((lastQuizAnswer.isBlank() && lastQuizOption.isBlank()) || chatHintTicks <= 0) {
            return;
        }
        Entity answerEntity = findNamedEntity(client, lastQuizAnswer);
        if (answerEntity != null) {
            answerEntity.setGlowingTag(true);
            glowingEntities.add(answerEntity.getId());
            worldBoxes.add(new WorldBox(answerEntity.getBoundingBox().inflate(0.3D), 0xFF32FF7A));
        }
        String answer = lastQuizOption.isBlank() ? lastQuizAnswer : lastQuizOption + ": " + lastQuizAnswer;
        solverLines.add("Quiz: " + answer);
    }

    private void renderWeirdosWorldSolver(Minecraft client) {
        if (correctWeirdoNpc.isBlank() && wrongWeirdoNpc.isBlank()) {
            return;
        }
        if (!correctWeirdoNpc.isBlank()) {
            Entity correct = findNamedEntity(client, correctWeirdoNpc);
            if (correct != null) {
                correct.setGlowingTag(true);
                glowingEntities.add(correct.getId());
                worldBoxes.add(new WorldBox(correct.getBoundingBox().inflate(0.35D), 0xFF32FF7A));
                solverLines.add("Weirdos: " + correctWeirdoNpc);
            }
        }
    }

    private void renderCreeperBeamsWorldSolver(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        if (currentRoom == null || roomDetector == null) {
            return;
        }
        int found = 0;
        for (RelativePair pair : CREEPER_BEAM_PAIRS) {
            BlockPos first = roomDetector.relativeToWorld(currentRoom, pair.first());
            BlockPos second = roomDetector.relativeToWorld(currentRoom, pair.second());
            if (client.level.getBlockState(first).is(Blocks.SEA_LANTERN) && client.level.getBlockState(second).is(Blocks.SEA_LANTERN)) {
                found++;
                worldBoxes.add(new WorldBox(new AABB(first).inflate(0.03D), found == 1 ? 0x664DFF9A : 0x5500D1D1));
                worldBoxes.add(new WorldBox(new AABB(second).inflate(0.03D), found == 1 ? 0x664DFF9A : 0x5500D1D1));
                if (found <= 4) {
                    solverLines.add("Beams " + found + ": " + compactPos(first) + " <-> " + compactPos(second));
                }
            }
        }
        if (found > 0) {
            solverLines.add("Creeper Beams: " + found + " Paare erkannt");
        }
    }

    private void renderIceFillWorldSolver(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        if (currentRoom == null || roomDetector == null || ICE_FILL_DATA.identifier().isEmpty()) {
            return;
        }
        List<BlockPos> path = detectIceFillPath(client, currentRoom, roomDetector);
        if (path.size() < 2) {
            return;
        }
        solverLines.add("Ice Fill: " + path.size() + " Schritte");
        worldBoxes.add(new WorldBox(pathNodeBox(path.get(0)), CURRENT));
        for (int index = 0; index < path.size() - 1; index++) {
            int color;
            if (index == 0) {
                color = CURRENT;
            } else if (index < 4) {
                color = NEXT;
            } else {
                color = 0x33FF66D9;
            }
            worldBoxes.add(new WorldBox(pathSegmentBox(path.get(index), path.get(index + 1)), color));
        }
        worldBoxes.add(new WorldBox(pathNodeBox(path.get(path.size() - 1)), NEXT));
    }

    private void renderBoulderWorldSolver(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        if (currentRoom == null || roomDetector == null) {
            return;
        }
        String layout = currentBoulderLayout(client, currentRoom, roomDetector);
        List<int[]> solution = BOULDER_SOLUTIONS.get(layout);
        if (!currentRoom.origin().equals(activeBoulderOrigin)) {
            activeBoulderOrigin = currentRoom.origin();
            activeBoulderLayout = "";
            activeBoulderSolution = List.of();
            activeBoulderStep = 0;
        }
        if (solution != null && !solution.isEmpty() && (!layout.equals(activeBoulderLayout) || activeBoulderSolution.isEmpty())) {
            activeBoulderLayout = layout;
            activeBoulderSolution = solution;
            if (activeBoulderStep >= activeBoulderSolution.size()) {
                activeBoulderStep = 0;
            }
        }
        if (activeBoulderSolution.isEmpty() || activeBoulderStep >= activeBoulderSolution.size()) {
            return;
        }
        solverLines.add("Boulder: Schritt " + (activeBoulderStep + 1) + "/" + activeBoulderSolution.size());
        for (int index = activeBoulderStep; index < Math.min(activeBoulderStep + 4, activeBoulderSolution.size()); index++) {
            int[] step = activeBoulderSolution.get(index);
            BlockPos renderPos = roomDetector.relativeToWorld(currentRoom, new BlockPos(step[0], 65, step[1]));
            BlockPos clickPos = roomDetector.relativeToWorld(currentRoom, new BlockPos(step[2], 65, step[3]));
            boolean current = index == activeBoulderStep;
            worldBoxes.add(new WorldBox(new AABB(renderPos).inflate(0.08D, 0.35D, 0.08D), current ? CURRENT : NEXT));
            worldBoxes.add(new WorldBox(new AABB(clickPos).inflate(0.08D, 0.1D, 0.08D), current ? CURRENT : NEXT));
            solverLines.add("Boulder " + (index - activeBoulderStep + 1) + ": " + compactPos(clickPos));
        }
    }

    private void renderIcePathWorldSolver(Minecraft client) {
        List<BlockPos> icePath = nearbyIce(client, 12);
        if (icePath.size() < 4 || icePath.size() > 180) {
            return;
        }
        solverLines.add("Ice Path: " + icePath.size() + " Eisfelder erkannt");
        for (int index = 0; index < Math.min(40, icePath.size()); index++) {
            worldBoxes.add(new WorldBox(new AABB(icePath.get(index)).inflate(0.01D), index == 0 ? 0xFF32FF7A : 0xFF00D1D1));
        }
    }

    private void renderTeleportMazeWorldSolver(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        if (currentRoom == null || roomDetector == null) {
            return;
        }
        if (!currentRoom.origin().equals(activeTeleportOrigin)) {
            activeTeleportOrigin = currentRoom.origin();
            visitedTeleportPads.clear();
        }
        BlockPos currentPad = null;
        for (BlockPos relativePad : TELEPORT_MAZE_PADS) {
            BlockPos worldPad = roomDetector.relativeToWorld(currentRoom, relativePad);
            if (client.player.getBoundingBox().inflate(0.75D, 0.1D, 0.75D).intersects(new AABB(worldPad))) {
                currentPad = worldPad;
                visitedTeleportPads.add(worldPad.immutable());
                break;
            }
        }
        int currentIndex = currentPad == null ? -1 : teleportPadIndex(currentRoom, roomDetector, currentPad);
        int groupStart = currentIndex >= 0 ? (currentIndex / 4) * 4 : -1;
        List<BlockPos> likelyPads = new ArrayList<>();
        for (int index = 0; index < TELEPORT_MAZE_PADS.size(); index++) {
            if (index >= 28 && currentIndex >= 28) {
                BlockPos worldPad = roomDetector.relativeToWorld(currentRoom, TELEPORT_MAZE_PADS.get(index));
                if (!visitedTeleportPads.contains(worldPad)) {
                    likelyPads.add(worldPad);
                }
                continue;
            }
            if (groupStart >= 0 && index >= groupStart && index < Math.min(groupStart + 4, TELEPORT_MAZE_PADS.size())) {
                BlockPos worldPad = roomDetector.relativeToWorld(currentRoom, TELEPORT_MAZE_PADS.get(index));
                if (!visitedTeleportPads.contains(worldPad) && !worldPad.equals(currentPad)) {
                    likelyPads.add(worldPad);
                }
            }
        }
        BlockPos bestPad = likelyPads.stream()
            .min(Comparator.comparingDouble(pad -> yawDeltaTo(client, pad)))
            .orElse(null);
        for (int index = 0; index < TELEPORT_MAZE_PADS.size(); index++) {
            BlockPos worldPad = roomDetector.relativeToWorld(currentRoom, TELEPORT_MAZE_PADS.get(index));
            int color = 0x33FFFFFF;
            if (visitedTeleportPads.contains(worldPad)) {
                color = VISITED;
            } else if (worldPad.equals(bestPad)) {
                color = CURRENT;
            } else if (likelyPads.contains(worldPad)) {
                color = NEXT;
            }
            worldBoxes.add(new WorldBox(new AABB(worldPad).inflate(0.02D, 0.35D, 0.02D), color));
        }
        solverLines.add("Teleport Maze: " + visitedTeleportPads.size() + " Pads besucht");
        if (bestPad != null) {
            solverLines.add("Teleport jetzt: " + compactPos(bestPad));
        }
    }

    private String puzzleHint(String title, HorizonConfig config, DetectedDungeonRoom currentRoom) {
        if (title.contains("water") && config.isPuzzleWaterBoardEnabled() && matchesPuzzleRoom(currentRoom, "Water Board")) {
            return "Water Board: Hebel so stellen, dass Wasser alle Kanaele bis zum Ziel fuellt.";
        }
        if (title.contains("weirdo") && config.isPuzzleThreeWeirdosEnabled() && matchesPuzzleRoom(currentRoom, "Three Weirdos")) {
            return "Three Weirdos erkannt. Textantworten im Chat beachten.";
        }
        if (title.contains("blaze") && config.isPuzzleBlazeEnabled() && shouldRunRoomSolver(currentRoom, "Blaze")) {
            return "Blaze erkannt. HP-Reihenfolge wird im HUD angezeigt.";
        }
        if ((title.contains("quiz") || title.contains("riddle")) && config.isPuzzleQuizEnabled() && matchesPuzzleRoom(currentRoom, "Quiz")) {
            return lastQuizAnswer.isBlank() ? "Quiz erkannt. Frage im Chat wird gelesen." : "Antwort: " + lastQuizAnswer;
        }
        if (title.contains("tic tac toe") && config.isPuzzleTicTacToeEnabled() && matchesPuzzleRoom(currentRoom, "Tic Tac Toe")) {
            return "Tic Tac Toe erkannt.";
        }
        if (title.contains("creeper") && config.isPuzzleCreeperBeamsEnabled() && matchesPuzzleRoom(currentRoom, "Creeper Beams")) {
            return "Creeper Beams: Strahlen auf den Creeper ausrichten.";
        }
        if (title.contains("boulder") && config.isPuzzleBoulderEnabled() && matchesPuzzleRoom(currentRoom, "Boulder")) {
            return "Boulder: Steinblock ueber Druckplatten zum Ziel schieben.";
        }
        if (title.contains("ice path") && config.isPuzzleIcePathEnabled() && matchesPuzzleRoom(currentRoom, "Ice Path")) {
            return "Ice Path erkannt. Exakter Pfad braucht Raum-Koordinaten.";
        }
        if (title.contains("teleport") && config.isPuzzleTeleportMazeEnabled() && matchesPuzzleRoom(currentRoom, "Teleport Maze")) {
            return "Teleport Maze: Teleporter-Pfad merken, falsche Pads vermeiden.";
        }
        return null;
    }

    private void renderWaterBoardWorldSolver(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        if (currentRoom == null || roomDetector == null || WATER_BOARD_DATA.patterns().isEmpty()) {
            return;
        }
        int pattern = detectWaterBoardPattern(client, currentRoom, roomDetector);
        String extensions = waterBoardExtensions(client, currentRoom, roomDetector);
        if (pattern < 0 || extensions.length() != 3) {
            return;
        }
        Map<String, List<Double>> solution = WATER_BOARD_DATA.solution("false", pattern, extensions);
        if (solution.isEmpty()) {
            return;
        }
        solverLines.add("Water Board: Pattern " + pattern + " / " + extensions);
        List<WaterStep> orderedSteps = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : solution.entrySet()) {
            for (Double time : entry.getValue()) {
                orderedSteps.add(new WaterStep(entry.getKey(), time));
            }
        }
        orderedSteps.sort(Comparator.comparingDouble(WaterStep::time).thenComparing(step -> step.time() == 0.0D ? 0 : 1));
        for (int index = 0; index < Math.min(4, orderedSteps.size()); index++) {
            WaterStep step = orderedSteps.get(index);
            BlockPos leverPos = waterLeverPos(step.lever(), currentRoom, roomDetector);
            if (leverPos != null) {
                worldBoxes.add(new WorldBox(new AABB(leverPos).inflate(0.12D, 0.45D, 0.12D), index == 0 ? CURRENT : NEXT));
            }
            solverLines.add("Water " + (index + 1) + ": " + waterLeverName(step.lever()) + (step.time() <= 0.0D ? " jetzt" : " @" + step.time() + "s"));
        }
    }

    private int detectWaterBoardPattern(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        if (client.level.getBlockState(roomDetector.relativeToWorld(currentRoom, new BlockPos(14, 77, 27))).is(Blocks.TERRACOTTA)) {
            return 0;
        }
        if (client.level.getBlockState(roomDetector.relativeToWorld(currentRoom, new BlockPos(16, 78, 27))).is(Blocks.EMERALD_BLOCK)) {
            return 1;
        }
        if (client.level.getBlockState(roomDetector.relativeToWorld(currentRoom, new BlockPos(14, 78, 27))).is(Blocks.DIAMOND_BLOCK)) {
            return 2;
        }
        if (client.level.getBlockState(roomDetector.relativeToWorld(currentRoom, new BlockPos(14, 78, 27))).is(Blocks.QUARTZ_BLOCK)) {
            return 3;
        }
        return -1;
    }

    private String waterBoardExtensions(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        int[][] wools = {
            {15, 56, 18, 1},
            {15, 56, 17, 2},
            {15, 56, 16, 3},
            {15, 56, 15, 4}
        };
        StringBuilder builder = new StringBuilder();
        for (int[] wool : wools) {
            BlockPos pos = roomDetector.relativeToWorld(currentRoom, new BlockPos(wool[0], wool[1], wool[2]));
            if (!client.level.getBlockState(pos).isAir()) {
                builder.append(wool[3]);
            }
        }
        return builder.toString();
    }

    private List<BlockPos> detectIceFillPath(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        List<BlockPos> worldPath = new ArrayList<>();
        List<List<List<BlockPos>>> identifiers = ICE_FILL_DATA.identifier();
        List<List<List<BlockPos>>> patterns = ICE_FILL_DATA.easy();
        for (int floor = 0; floor < Math.min(identifiers.size(), patterns.size()); floor++) {
            List<List<BlockPos>> floorIdentifiers = identifiers.get(floor);
            List<List<BlockPos>> floorPatterns = patterns.get(floor);
            boolean matched = false;
            for (int patternIndex = 0; patternIndex < Math.min(floorIdentifiers.size(), floorPatterns.size()); patternIndex++) {
                List<BlockPos> identifier = floorIdentifiers.get(patternIndex);
                if (identifier.size() < 2) {
                    continue;
                }
                BlockPos first = roomDetector.relativeToWorld(currentRoom, identifier.get(0));
                BlockPos second = roomDetector.relativeToWorld(currentRoom, identifier.get(1));
                boolean firstAir = client.level.getBlockState(first).isAir();
                boolean secondAir = client.level.getBlockState(second).isAir();
                if (firstAir && !secondAir) {
                    for (BlockPos relative : floorPatterns.get(patternIndex)) {
                        worldPath.add(roomDetector.relativeToWorld(currentRoom, relative));
                    }
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return List.of();
            }
        }
        return worldPath;
    }

    private AABB pathSegmentBox(BlockPos from, BlockPos to) {
        double thickness = 0.12D;
        double half = thickness / 2.0D;
        double minX = Math.min(from.getX() + 0.5D, to.getX() + 0.5D) - half;
        double maxX = Math.max(from.getX() + 0.5D, to.getX() + 0.5D) + half;
        double minZ = Math.min(from.getZ() + 0.5D, to.getZ() + 0.5D) - half;
        double maxZ = Math.max(from.getZ() + 0.5D, to.getZ() + 0.5D) + half;
        double minY = Math.min(from.getY(), to.getY()) + 0.08D;
        double maxY = minY + 0.08D;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private AABB pathNodeBox(BlockPos pos) {
        double half = 0.10D;
        return new AABB(
            pos.getX() + 0.5D - half,
            pos.getY() + 0.08D,
            pos.getZ() + 0.5D - half,
            pos.getX() + 0.5D + half,
            pos.getY() + 0.16D,
            pos.getZ() + 0.5D + half
        );
    }

    private String currentBoulderLayout(Minecraft client, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        StringBuilder layout = new StringBuilder();
        for (int z = 24; z >= 9; z -= 3) {
            for (int x = 24; x >= 6; x -= 3) {
                BlockPos sample = roomDetector.relativeToWorld(currentRoom, new BlockPos(x, 66, z));
                layout.append(client.level.getBlockState(sample).isAir() ? '0' : '1');
            }
        }
        return layout.toString();
    }

    private List<BlazeTarget> blazeTargets(Minecraft client, DetectedDungeonRoom currentRoom) {
        List<BlazeTarget> targets = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand) && !entity.hasCustomName()) {
                continue;
            }
            String name = clean(entity.getName().getString());
            Integer hp = blazeHealth(name);
            if (hp == null || hp <= 0) {
                continue;
            }
            double distance = client.player.distanceTo(entity);
            if (distance <= 40.0D) {
                targets.add(new BlazeTarget(entity, hp, distance));
            }
        }
        boolean lowerBlaze = inferLowerBlaze(client, targets, currentRoom);
        targets.sort(lowerBlaze ? Comparator.comparingInt(BlazeTarget::health).reversed() : Comparator.comparingInt(BlazeTarget::health));
        if (!targets.isEmpty()) {
            solverLines.add("Blaze Mode: " + (lowerBlaze ? "LOWER" : "HIGHER"));
        }
        return targets;
    }

    private Integer blazeHealth(String name) {
        Matcher matcher = BLAZE_HEALTH_PATTERN.matcher(name.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int nearbyCreepers(Minecraft client) {
        int count = 0;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Creeper && client.player.distanceTo(entity) <= 35.0D) {
                count++;
            }
        }
        return count;
    }

    private boolean inferLowerBlaze(Minecraft client, List<BlazeTarget> targets, DetectedDungeonRoom currentRoom) {
        if (currentRoom != null && currentRoom.containsName("Lower Blaze")) {
            return true;
        }
        if (currentRoom != null && currentRoom.containsName("Higher Blaze")) {
            return false;
        }
        if (targets.isEmpty()) {
            return false;
        }
        double averageY = 0.0D;
        for (BlazeTarget target : targets) {
            averageY += target.label().getY();
        }
        averageY /= targets.size();
        return client.player.getY() > averageY;
    }

    private boolean shouldRunRoomSolver(DetectedDungeonRoom currentRoom, String... names) {
        if (currentRoom == null) {
            return false;
        }
        for (String name : names) {
            if (currentRoom.containsName(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPuzzleRoom(DetectedDungeonRoom currentRoom, String name) {
        return currentRoom != null && currentRoom.isPuzzle(name);
    }

    private AABB blazeRenderBox(Entity label) {
        return label.getBoundingBox().inflate(0.65D, 1.1D, 0.65D).move(0.0D, -1.2D, 0.0D);
    }

    private String blazeSummary(List<BlazeTarget> targets) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < Math.min(6, targets.size()); index++) {
            if (index > 0) {
                builder.append(" > ");
            }
            builder.append(targets.get(index).health()).append("HP");
        }
        return builder.toString();
    }

    private String directionTo(Minecraft client, Entity entity) {
        double dx = entity.getX() - client.player.getX();
        double dz = entity.getZ() - client.player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
        double yawDelta = Mth.wrapDegrees(targetYaw - client.player.getYRot(1.0F));
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        String side;
        if (Math.abs(yawDelta) < 8.0D) {
            side = "CENTER";
        } else if (yawDelta > 0.0D) {
            side = "RIGHT " + Math.round(Math.abs(yawDelta)) + " deg";
        } else {
            side = "LEFT " + Math.round(Math.abs(yawDelta)) + " deg";
        }
        return side + " / " + Math.round(distance) + "m";
    }

    private double yawDeltaTo(Minecraft client, BlockPos pos) {
        double dx = (pos.getX() + 0.5D) - client.player.getX();
        double dz = (pos.getZ() + 0.5D) - client.player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
        return Math.abs(Mth.wrapDegrees(targetYaw - client.player.getYRot(1.0F)));
    }

    private void addQuizAnswer(String question, String... answers) {
        quizAnswers.put(question.toLowerCase(Locale.ROOT), List.of(answers));
    }

    private void drawHighlights(AbstractContainerScreen<?> screen, GuiGraphicsExtractor context, SolveResult result) {
        ScreenPosition position = screenPosition(screen);
        int left = position.x();
        int top = position.y();
        for (Highlight highlight : result.highlights()) {
            Slot slot = highlight.slot();
            int x = left + slot.x;
            int y = top + slot.y;
            context.fill(x, y, x + 16, y + 16, highlight.color());
            context.outline(x, y, 16, 16, HudStyle.border());
            if (!highlight.label().isBlank()) {
                context.text(Minecraft.getInstance().font, Component.literal(highlight.label()), x + 1, y + 1, HudStyle.text(), true);
            }
        }
    }

    private ScreenPosition screenPosition(AbstractContainerScreen<?> screen) {
        if (!screenPositionReflectionFailed) {
            try {
                if (screenXField == null || screenYField == null) {
                    screenXField = findField(screen.getClass(), "x", "field_2776");
                    screenYField = findField(screen.getClass(), "y", "field_2800");
                }
                if (screenXField != null && screenYField != null) {
                    return new ScreenPosition(screenXField.getInt(screen), screenYField.getInt(screen));
                }
            } catch (ReflectiveOperationException ignored) {
                screenPositionReflectionFailed = true;
            }
        }

        SlotBounds bounds = slotBounds(screen);
        return new ScreenPosition((screen.width - bounds.width()) / 2 - bounds.minX(), (screen.height - bounds.height()) / 2 - bounds.minY());
    }

    private Field findField(Class<?> type, String... names) {
        Class<?> current = type;
        while (current != null) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private void drawPanel(AbstractContainerScreen<?> screen, GuiGraphicsExtractor context, Minecraft client, SolveResult result) {
        int width = 276;
        int height = 56;
        int x = 16;
        int y = screen.height - height - 16;
        context.fill(x, y, x + width, y + height, HudStyle.panel());
        context.outline(x, y, width, height, HudStyle.border());
        context.text(client.font, Component.literal("Horizon Solver: " + result.name()), x + 12, y + 10, HudStyle.accent());
        context.text(client.font, Component.literal(result.hint()), x + 12, y + 30, HudStyle.muted());
    }

    private List<Slot> containerSlots(AbstractContainerScreen<?> screen) {
        List<Slot> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem() && slot.index < screen.getMenu().slots.size() - 36 && seen.add(slot.index)) {
                result.add(slot);
            }
        }
        return result;
    }

    private SlotBounds slotBounds(AbstractContainerScreen<?> screen) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Slot slot : screen.getMenu().slots) {
            minX = Math.min(minX, slot.x);
            minY = Math.min(minY, slot.y);
            maxX = Math.max(maxX, slot.x + 16);
            maxY = Math.max(maxY, slot.y + 16);
        }
        if (minX == Integer.MAX_VALUE) {
            return new SlotBounds(0, 0, 176, 166);
        }
        return new SlotBounds(minX, minY, maxX - minX, maxY - minY);
    }

    private String stackText(ItemStack stack) {
        StringBuilder builder = new StringBuilder(clean(stack.getHoverName().getString()));
        if (stack.has(DataComponents.LORE)) {
            for (Component line : stack.get(DataComponents.LORE).lines()) {
                builder.append(' ').append(clean(line.getString()));
            }
        }
        return builder.toString();
    }

    private Integer firstInteger(String text) {
        Matcher matcher = Pattern.compile("\\d+").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group()) : null;
    }

    private String targetColor(String text) {
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        String normalized = text.replace(' ', '_');
        for (String color : colors) {
            if (normalized.contains(color)) {
                return color;
            }
        }
        return null;
    }

    private String colorName(ItemStack stack) {
        String id = itemId(stack);
        String fullColor = targetColor(id);
        if (fullColor != null) {
            return fullColor;
        }
        for (String part : id.split("_")) {
            if (targetColor(part) != null) {
                return part;
            }
        }
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        return dyed == null ? null : Integer.toHexString(dyed.rgb());
    }

    private String itemId(ItemStack stack) {
        Item item = stack.getItem();
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "").strip();
    }

    private String compactPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private void advanceBoulderStep(BlockPos clickPos, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }
        if (!currentRoom.origin().equals(activeBoulderOrigin)) {
            activeBoulderOrigin = currentRoom.origin();
            activeBoulderLayout = currentBoulderLayout(client, currentRoom, roomDetector);
            activeBoulderSolution = BOULDER_SOLUTIONS.getOrDefault(activeBoulderLayout, List.of());
            activeBoulderStep = 0;
        }
        if (activeBoulderSolution.isEmpty() || activeBoulderStep >= activeBoulderSolution.size()) {
            return;
        }
        int[] step = activeBoulderSolution.get(activeBoulderStep);
        BlockPos expected = roomDetector.relativeToWorld(currentRoom, new BlockPos(step[2], 65, step[3]));
        if (expected.equals(clickPos)) {
            activeBoulderStep++;
        }
    }

    private int teleportPadIndex(DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector, BlockPos worldPad) {
        for (int index = 0; index < TELEPORT_MAZE_PADS.size(); index++) {
            if (roomDetector.relativeToWorld(currentRoom, TELEPORT_MAZE_PADS.get(index)).equals(worldPad)) {
                return index;
            }
        }
        return -1;
    }

    private void clearGlowing(Minecraft client) {
        if (client == null || client.level == null || glowingEntities.isEmpty()) {
            glowingEntities.clear();
            return;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (glowingEntities.contains(entity.getId())) {
                entity.setGlowingTag(false);
            }
        }
        glowingEntities.clear();
    }

    private Entity findNamedEntity(Minecraft client, String needle) {
        String normalizedNeedle = clean(needle).toLowerCase(Locale.ROOT);
        for (Entity entity : client.level.entitiesForRendering()) {
            String name = clean(entity.getName().getString()).toLowerCase(Locale.ROOT);
            if (!normalizedNeedle.isBlank() && name.contains(normalizedNeedle) && client.player.distanceTo(entity) <= 40.0D) {
                return entity;
            }
        }
        return null;
    }

    private List<BlockPos> nearbyBlocks(Minecraft client, int radius, net.minecraft.world.level.block.Block block) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = client.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -6, -radius), center.offset(radius, 8, radius))) {
            if (client.level.getBlockState(pos).is(block)) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    private List<BlockPos> nearbyIce(Minecraft client, int radius) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = client.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            if (client.level.getBlockState(pos).is(Blocks.ICE)
                || client.level.getBlockState(pos).is(Blocks.PACKED_ICE)
                || client.level.getBlockState(pos).is(Blocks.BLUE_ICE)
                || client.level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    private List<BlockPos> nearbyBoulders(Minecraft client, int radius) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = client.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 6, radius))) {
            if ((client.level.getBlockState(pos).is(Blocks.STONE)
                || client.level.getBlockState(pos).is(Blocks.COBBLESTONE)
                || client.level.getBlockState(pos).is(Blocks.MOSSY_COBBLESTONE))
                && client.level.getBlockState(pos.above()).isAir()
                && client.level.getBlockState(pos.below()).isRedstoneConductor(client.level, pos.below())) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    private List<LampPair> logicalLampPairs(List<BlockPos> lanterns) {
        List<LampPair> pairs = new ArrayList<>();
        Set<BlockPos> used = new HashSet<>();
        lanterns.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        for (BlockPos first : lanterns) {
            if (used.contains(first)) {
                continue;
            }
            BlockPos best = null;
            int bestDistance = Integer.MAX_VALUE;
            for (BlockPos second : lanterns) {
                if (first.equals(second) || used.contains(second) || first.getY() != second.getY()) {
                    continue;
                }
                boolean aligned = first.getX() == second.getX() || first.getZ() == second.getZ();
                if (!aligned) {
                    continue;
                }
                int distance = Math.abs(first.getX() - second.getX()) + Math.abs(first.getZ() - second.getZ());
                if (distance < bestDistance && hasClearLampLine(first, second, lanterns)) {
                    best = second;
                    bestDistance = distance;
                }
            }
            if (best != null) {
                used.add(first);
                used.add(best);
                pairs.add(new LampPair(first, best));
            }
        }
        return pairs;
    }

    private boolean hasClearLampLine(BlockPos first, BlockPos second, List<BlockPos> lanterns) {
        for (BlockPos other : lanterns) {
            if (other.equals(first) || other.equals(second) || other.getY() != first.getY()) {
                continue;
            }
            if (first.getX() == second.getX() && other.getX() == first.getX() && between(other.getZ(), first.getZ(), second.getZ())) {
                return false;
            }
            if (first.getZ() == second.getZ() && other.getZ() == first.getZ() && between(other.getX(), first.getX(), second.getX())) {
                return false;
            }
        }
        return true;
    }

    private boolean between(int value, int a, int b) {
        return value > Math.min(a, b) && value < Math.max(a, b);
    }

    private boolean isMazeWalkable(Slot slot) {
        String id = itemId(slot.getItem());
        return !id.contains("black") && !id.contains("gray") && !id.contains("barrier");
    }

    private List<Slot> shortestPath(Slot start, Slot end, Grid grid, SlotPredicate walkable) {
        Queue<Slot> queue = new ArrayDeque<>();
        Map<Slot, Slot> previous = new HashMap<>();
        Set<Slot> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Slot current = queue.poll();
            if (current == end) {
                break;
            }
            for (Slot next : grid.neighbors(current)) {
                if (!visited.contains(next) && (next == end || walkable.test(next))) {
                    visited.add(next);
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }

        if (!visited.contains(end)) {
            return List.of();
        }

        List<Slot> reversed = new ArrayList<>();
        Slot current = end;
        while (current != null) {
            reversed.add(current);
            current = previous.get(current);
        }

        List<Slot> ordered = new ArrayList<>();
        for (int index = reversed.size() - 1; index >= 0; index--) {
            ordered.add(reversed.get(index));
        }
        return ordered;
    }

    private Grid grid(List<Slot> slots) {
        Map<Integer, List<Slot>> byY = new HashMap<>();
        for (Slot slot : slots) {
            byY.computeIfAbsent(slot.y, ignored -> new ArrayList<>()).add(slot);
        }
        for (List<Slot> row : byY.values()) {
            row.sort(Comparator.comparingInt(slot -> slot.x));
        }
        return new Grid(byY);
    }

    private record SolveResult(String name, String hint, List<Highlight> highlights) {
    }

    private record Highlight(Slot slot, int color, String label) {
    }

    private record NumberedSlot(Slot slot, int number) {
    }

    private record SlotBounds(int minX, int minY, int width, int height) {
    }

    private record ScreenPosition(int x, int y) {
    }

    private record BlazeTarget(Entity label, int health, double distance) {
    }

    private record WorldBox(AABB box, int color) {
    }

    private record LampPair(BlockPos first, BlockPos second) {
    }

    private record RelativePair(BlockPos first, BlockPos second) {
    }

    private record WaterStep(String lever, double time) {
    }

    private record WaterBoardData(Map<String, Map<Integer, Map<String, Map<String, List<Double>>>>> patterns) {
        private Map<String, List<Double>> solution(String optimized, int pattern, String extensions) {
            return patterns.getOrDefault(optimized, Collections.emptyMap())
                .getOrDefault(pattern, Collections.emptyMap())
                .getOrDefault(extensions, Collections.emptyMap());
        }
    }

    private record IceFillData(List<List<List<BlockPos>>> identifier, List<List<List<BlockPos>>> easy) {
    }

    private record Grid(Map<Integer, List<Slot>> byY) {
        private List<Slot> neighbors(Slot slot) {
            List<Slot> result = new ArrayList<>();
            List<Integer> rows = new ArrayList<>(byY.keySet());
            rows.sort(Integer::compareTo);
            int rowIndex = rows.indexOf(slot.y);
            List<Slot> row = byY.get(slot.y);
            int columnIndex = row.indexOf(slot);
            if (columnIndex > 0) {
                result.add(row.get(columnIndex - 1));
            }
            if (columnIndex >= 0 && columnIndex < row.size() - 1) {
                result.add(row.get(columnIndex + 1));
            }
            if (rowIndex > 0) {
                addColumn(result, byY.get(rows.get(rowIndex - 1)), columnIndex);
            }
            if (rowIndex >= 0 && rowIndex < rows.size() - 1) {
                addColumn(result, byY.get(rows.get(rowIndex + 1)), columnIndex);
            }
            return result;
        }

        private void addColumn(List<Slot> result, List<Slot> row, int columnIndex) {
            if (columnIndex >= 0 && columnIndex < row.size()) {
                result.add(row.get(columnIndex));
            }
        }
    }

    private interface SlotPredicate {
        boolean test(Slot slot);
    }

    private static List<RelativePair> loadCreeperBeamPairs() {
        List<RelativePair> result = new ArrayList<>();
        try (InputStream stream = DungeonSolverOverlay.class.getResourceAsStream("/assets/horizon/puzzles/creeperBeamsSolutions.json")) {
            if (stream == null) {
                return result;
            }
            JsonArray array = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : array) {
                JsonArray pair = element.getAsJsonArray();
                result.add(new RelativePair(
                    new BlockPos(pair.get(0).getAsInt(), pair.get(1).getAsInt(), pair.get(2).getAsInt()),
                    new BlockPos(pair.get(3).getAsInt(), pair.get(4).getAsInt(), pair.get(5).getAsInt())
                ));
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return result;
    }

    private static Map<String, List<int[]>> loadBoulderSolutions() {
        Map<String, List<int[]>> result = new HashMap<>();
        try (InputStream stream = DungeonSolverOverlay.class.getResourceAsStream("/assets/horizon/puzzles/boulderSolutions.json")) {
            if (stream == null) {
                return result;
            }
            JsonObject object = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                List<int[]> steps = new ArrayList<>();
                for (JsonElement stepElement : entry.getValue().getAsJsonArray()) {
                    JsonArray step = stepElement.getAsJsonArray();
                    steps.add(new int[] {
                        step.get(0).getAsInt(),
                        step.get(1).getAsInt(),
                        step.get(2).getAsInt(),
                        step.get(3).getAsInt()
                    });
                }
                result.put(entry.getKey(), steps);
            }
        } catch (Exception ignored) {
            return Map.of();
        }
        return result;
    }

    private static WaterBoardData loadWaterBoardData() {
        Map<String, Map<Integer, Map<String, Map<String, List<Double>>>>> root = new HashMap<>();
        try (InputStream stream = DungeonSolverOverlay.class.getResourceAsStream("/assets/horizon/puzzles/waterSolutions.json")) {
            if (stream == null) {
                return new WaterBoardData(root);
            }
            JsonObject object = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> optimizedEntry : object.entrySet()) {
                Map<Integer, Map<String, Map<String, List<Double>>>> byPattern = new HashMap<>();
                for (Map.Entry<String, JsonElement> patternEntry : optimizedEntry.getValue().getAsJsonObject().entrySet()) {
                    Map<String, Map<String, List<Double>>> byExtension = new HashMap<>();
                    for (Map.Entry<String, JsonElement> extensionEntry : patternEntry.getValue().getAsJsonObject().entrySet()) {
                        Map<String, List<Double>> levers = new LinkedHashMap<>();
                        for (Map.Entry<String, JsonElement> leverEntry : extensionEntry.getValue().getAsJsonObject().entrySet()) {
                            List<Double> times = new ArrayList<>();
                            for (JsonElement time : leverEntry.getValue().getAsJsonArray()) {
                                times.add(time.getAsDouble());
                            }
                            levers.put(leverEntry.getKey(), times);
                        }
                        byExtension.put(extensionEntry.getKey(), levers);
                    }
                    byPattern.put(Integer.parseInt(patternEntry.getKey()), byExtension);
                }
                root.put(optimizedEntry.getKey(), byPattern);
            }
        } catch (Exception ignored) {
            return new WaterBoardData(Map.of());
        }
        return new WaterBoardData(root);
    }

    private static IceFillData loadIceFillData() {
        try (InputStream stream = DungeonSolverOverlay.class.getResourceAsStream("/assets/horizon/puzzles/iceFillFloors.json")) {
            if (stream == null) {
                return new IceFillData(List.of(), List.of());
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return new IceFillData(
                parseIceFillFloors(root.getAsJsonArray("identifier")),
                parseIceFillFloors(root.getAsJsonArray("easy"))
            );
        } catch (Exception ignored) {
            return new IceFillData(List.of(), List.of());
        }
    }

    private static List<List<List<BlockPos>>> parseIceFillFloors(JsonArray array) {
        List<List<List<BlockPos>>> floors = new ArrayList<>();
        for (JsonElement floorElement : array) {
            List<List<BlockPos>> floor = new ArrayList<>();
            for (JsonElement patternElement : floorElement.getAsJsonArray()) {
                List<BlockPos> pattern = new ArrayList<>();
                for (JsonElement posElement : patternElement.getAsJsonArray()) {
                    JsonObject pos = posElement.getAsJsonObject();
                    pattern.add(new BlockPos(pos.get("x").getAsInt(), pos.get("y").getAsInt(), pos.get("z").getAsInt()));
                }
                floor.add(pattern);
            }
            floors.add(floor);
        }
        return floors;
    }

    private String waterLeverName(String key) {
        return switch (key) {
            case "diamond_block" -> "Diamond";
            case "emerald_block" -> "Emerald";
            case "hardened_clay" -> "Clay";
            case "quartz_block" -> "Quartz";
            case "gold_block" -> "Gold";
            case "coal_block" -> "Coal";
            case "water" -> "Water";
            default -> key;
        };
    }

    private BlockPos waterLeverPos(String key, DetectedDungeonRoom currentRoom, DungeonRoomDetector roomDetector) {
        if (currentRoom == null || roomDetector == null) {
            return null;
        }
        return switch (key) {
            case "coal_block" -> roomDetector.relativeToWorld(currentRoom, new BlockPos(20, 61, 10));
            case "gold_block" -> roomDetector.relativeToWorld(currentRoom, new BlockPos(20, 61, 15));
            case "quartz_block" -> roomDetector.relativeToWorld(currentRoom, new BlockPos(20, 61, 20));
            case "diamond_block" -> roomDetector.relativeToWorld(currentRoom, new BlockPos(10, 61, 20));
            case "emerald_block" -> roomDetector.relativeToWorld(currentRoom, new BlockPos(10, 61, 15));
            case "hardened_clay" -> roomDetector.relativeToWorld(currentRoom, new BlockPos(10, 61, 10));
            case "water" -> roomDetector.relativeToWorld(currentRoom, new BlockPos(15, 60, 5));
            default -> null;
        };
    }
}
