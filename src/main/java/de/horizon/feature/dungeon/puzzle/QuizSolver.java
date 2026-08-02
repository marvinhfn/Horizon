package de.horizon.feature.dungeon.puzzle;

import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quiz puzzle solver.
 * Hardcoded answers, chat-based detection, highlights the correct answer block.
 */
public final class QuizSolver {
    // Match ⓐⓑⓒ anywhere in the string (not anchored to start — NPC prefix like "[STATUE] Oruo:" comes before)
    private static final Pattern OPTION_PATTERN = Pattern.compile("([ⓐⓑⓒ])\\s+(.+?)\\s*$");

    // Answer block positions in room-component coords at y=70
    private static final Map<String, int[]> TYPE_BLOCKS = Map.of(
        "ⓐ", new int[]{20, 6},
        "ⓑ", new int[]{15, 9},
        "ⓒ", new int[]{10, 6}
    );

    // Quiz answers loaded from JSON + hardcoded fallbacks
    private static final Map<String, List<String>> ANSWERS = loadAnswers();

    private static Map<String, List<String>> loadAnswers() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        // Load from JSON resource
        try (InputStream is = QuizSolver.class.getResourceAsStream("/assets/horizon/puzzles/quizAnswers.json")) {
            if (is != null) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var entry : json.entrySet()) {
                    List<String> answers = new ArrayList<>();
                    for (JsonElement el : entry.getValue().getAsJsonArray()) {
                        answers.add(el.getAsString());
                    }
                    map.put(entry.getKey(), answers);
                }
            }
        } catch (Exception ignored) {}
        // Ensure known answers are present (fallback for missing JSON entries)
        map.putIfAbsent("What is the name of the vendor in the Hub who sells stained glass?", List.of("Wool Weaver"));
        return map;
    }

    private List<String> currentSolution = null;
    private String currentAnswer = null; // "ⓐ", "ⓑ", or "ⓒ"
    private DetectedDungeonRoom currentRoom;
    private DungeonRoomDetector currentDetector;

    public void onRoomEnter(DetectedDungeonRoom room, DungeonRoomDetector detector) {
        // Don't reset solution — it may have been detected from chat before room detection
        currentRoom = room;
        currentDetector = detector;
    }

    public void onChatMessage(String raw, Minecraft mc) {
        String plain = stripFormatting(raw).trim();

        // "answered ... correctly" → reset for next question
        if (plain.contains("answered") && plain.contains("correctly")) {
            resetSolution();
            return;
        }
        if (plain.contains("Yikes")) {
            resetSolution();
            return;
        }

        // Detect question (ends with ?)
        if (plain.endsWith("?")) {
            String question = plain.trim();
            // Handle truncated question
            if (question.trim().equals("glass?")) {
                question = "What is the name of the vendor in the Hub who sells stained glass?";
            }

            if (question.contains("What SkyBlock year is it")) {
                currentSolution = List.of(currentSkyBlockYear());
            } else {
                // Try exact match first, then partial
                currentSolution = ANSWERS.get(question);
                if (currentSolution == null) {
                    for (Map.Entry<String, List<String>> entry : ANSWERS.entrySet()) {
                        if (question.contains(entry.getKey()) || entry.getKey().contains(question)) {
                            currentSolution = entry.getValue();
                            break;
                        }
                    }
                }
            }
            currentAnswer = null;
        }

        // Detect answer options
        Matcher m = OPTION_PATTERN.matcher(plain);
        if (m.find() && currentSolution != null) {
            String prefix = m.group(1);
            String option = m.group(2).trim();
            if (currentSolution.stream().anyMatch(s -> option.contains(s) || s.contains(option))) {
                currentAnswer = prefix;
            }
        }
    }

    /**
     * Returns a colored Component if this message is a quiz answer option and we know the answer.
     * Correct answer → green, wrong answers → red. Returns null if not a quiz option or unknown.
     */
    public Component colorQuizOption(String raw) {
        if (currentSolution == null) return null;
        String plain = stripFormatting(raw).trim();
        Matcher m = OPTION_PATTERN.matcher(plain);
        if (!m.find()) return null;

        String option = m.group(2).trim();
        boolean isCorrect = currentSolution.stream().anyMatch(s -> option.contains(s) || s.contains(option));
        ChatFormatting color = isCorrect ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.literal(plain).withStyle(color);
    }

    /** True if {@code worldPos} is one of the two WRONG answer blocks (right-click should be cancelled). */
    public boolean shouldBlockInteract(BlockPos worldPos) {
        if (currentAnswer == null || currentRoom == null || currentDetector == null) return false;
        for (var entry : TYPE_BLOCKS.entrySet()) {
            if (entry.getKey().equals(currentAnswer)) continue; // the correct block — allow
            int[] rel = entry.getValue();
            BlockPos w = currentDetector.relativeToWorld(currentRoom, new BlockPos(rel[0], 70, rel[1]));
            if (w.getX() == worldPos.getX() && w.getZ() == worldPos.getZ()) return true;
        }
        return false;
    }

    public void renderWorld(LevelRenderContext ctx, int style) {
        if (currentAnswer == null || currentRoom == null || currentDetector == null) return;
        int[] pos = TYPE_BLOCKS.get(currentAnswer);
        if (pos == null) return;
        BlockPos worldPos = currentDetector.relativeToWorld(currentRoom, new BlockPos(pos[0], 70, pos[1]));
        DungeonRenderUtil.drawBox(ctx, new AABB(worldPos), 0xAA00FF44, style, true);
    }

    public void reset() {
        resetSolution();
        currentRoom = null;
        currentDetector = null;
    }

    private void resetSolution() {
        currentSolution = null;
        currentAnswer = null;
    }

    private static String stripFormatting(String s) {
        return s == null ? "" : s.replaceAll("(?i)\u00a7[0-9a-fk-or]", "");
    }

    private static String currentSkyBlockYear() {
        long year = ((System.currentTimeMillis() / 1000) - 1560276000) / 446400 + 1;
        return "Year " + year;
    }
}
