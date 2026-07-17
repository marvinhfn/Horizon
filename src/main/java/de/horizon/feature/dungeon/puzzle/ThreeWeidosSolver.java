package de.horizon.feature.dungeon.puzzle;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Three Weirdos puzzle solver.
 * Uses regex patterns for correct/wrong statements,
 * finds NPC by name via entity scan, locates adjacent chest.
 */
public final class ThreeWeidosSolver {
    private static final Pattern NPC_PATTERN = Pattern.compile("\\[NPC] (\\w+): (.+)");
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    // Correct answer patterns (speaker tells truth → reward IS in their chest)
    private static final List<Pattern> SOLUTION_PATTERNS = List.of(
        Pattern.compile("The reward is not in my chest!"),
        Pattern.compile("At least one of them is lying, and the reward is not in \\w+'s chest.?"),
        Pattern.compile("My chest doesn't have the reward\\. We are all telling the truth.?"),
        Pattern.compile("My chest has the reward and I'm telling the truth!"),
        Pattern.compile("The reward isn't in any of our chests.?"),
        Pattern.compile("Both of them are telling the truth\\. Also, \\w+ has the reward in their chest.?")
    );

    // Wrong answer patterns
    private static final List<Pattern> WRONG_PATTERNS = List.of(
        Pattern.compile("One of us is telling the truth!"),
        Pattern.compile("They are both telling the truth\\. The reward isn't in \\w+'s chest."),
        Pattern.compile("We are all telling the truth!"),
        Pattern.compile("\\w+ is telling the truth and the reward is in his chest."),
        Pattern.compile("My chest doesn't have the reward. At least one of the others is telling the truth!"),
        Pattern.compile("One of the others is lying."),
        Pattern.compile("They are both telling the truth, the reward is in \\w+'s chest."),
        Pattern.compile("They are both lying, the reward is in my chest!"),
        Pattern.compile("The reward is in my chest."),
        Pattern.compile("The reward is not in my chest\\. They are both lying."),
        Pattern.compile("\\w+ is telling the truth."),
        Pattern.compile("My chest has the reward.")
    );

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final CopyOnWriteArrayList<AnswerData> answers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Integer> entityNames = new ConcurrentHashMap<>();

    private record AnswerData(BlockPos chestPos, boolean isCorrect) {}

    public void onRoomEnter() {
        reset();
    }

    public void onChatMessage(String raw, Minecraft mc) {
        if (mc == null || mc.level == null) return;
        String stripped = FORMATTING.matcher(raw).replaceAll("").strip();
        Matcher m = NPC_PATTERN.matcher(stripped);
        if (!m.find()) return;

        String npcName = m.group(1).strip();
        String message = m.group(2).strip();

        boolean isSolution = SOLUTION_PATTERNS.stream().anyMatch(p -> p.matcher(message).matches());
        boolean isWrong = WRONG_PATTERNS.stream().anyMatch(p -> p.matcher(message).matches());
        if (!isSolution && !isWrong) return;

        BlockPos chest = findChestNearNpc(npcName, mc);
        if (chest == null) return;

        // Avoid duplicates
        for (AnswerData existing : answers) {
            if (existing.chestPos.equals(chest)) return;
        }
        answers.add(new AnswerData(chest, isSolution));
    }

    /** Track entity name changes to map NPC names to entity IDs. */
    public void onEntityNameChange(String name, int entityId) {
        if (name != null && !name.isEmpty()) {
            entityNames.put(name, entityId);
        }
    }

    private BlockPos findChestNearNpc(String npcName, Minecraft mc) {
        // Try entity name lookup first
        Integer entityId = entityNames.get(npcName);
        Entity npcEntity = null;
        if (entityId != null && mc.level != null) {
            npcEntity = mc.level.getEntity(entityId);
        }

        // Fallback: scan all entities for matching name
        if (npcEntity == null) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof ArmorStand as)) continue;
                String name = FORMATTING.matcher(
                    as.getCustomName() != null ? as.getCustomName().getString() : as.getName().getString()
                ).replaceAll("").strip();
                if (name.equalsIgnoreCase(npcName)) {
                    npcEntity = as;
                    break;
                }
            }
        }
        if (npcEntity == null) return null;

        int ex = (int) Math.floor(npcEntity.getX());
        int ez = (int) Math.floor(npcEntity.getZ());

        for (int[] dir : DIRS) {
            BlockPos candidate = new BlockPos(ex + dir[0], 69, ez + dir[1]);
            if (mc.level.getBlockState(candidate).is(Blocks.CHEST)) {
                return candidate;
            }
        }
        return null;
    }

    public void renderWorld(LevelRenderContext ctx, int style) {
        for (AnswerData data : answers) {
            int color = data.isCorrect ? 0xAA00FF44 : 0xAAFF2222;
            DungeonRenderUtil.drawBox(ctx, new AABB(data.chestPos), color, style, false);
        }
    }

    public void reset() {
        answers.clear();
        entityNames.clear();
    }
}
