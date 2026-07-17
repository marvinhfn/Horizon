package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SpiritBearService {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final int BEAR_SPAWN_DELAY = 68;
    private static final BlockPos LAST_BLOCK = new BlockPos(7, 77, 34);

    private static final Set<BlockPos> F4_BLOCKS = Set.of(
        new BlockPos(-3, 77, 33), new BlockPos(-9, 77, 31), new BlockPos(-16, 77, 26),
        new BlockPos(-20, 77, 20), new BlockPos(-23, 77, 13), new BlockPos(-24, 77, 6),
        new BlockPos(-24, 77, 0), new BlockPos(-22, 77, -7), new BlockPos(-18, 77, -13),
        new BlockPos(-12, 77, -19), new BlockPos(-5, 77, -22), new BlockPos(1, 77, -24),
        new BlockPos(8, 77, -24), new BlockPos(14, 77, -23), new BlockPos(21, 77, -19),
        new BlockPos(27, 77, -14), new BlockPos(31, 77, -8), new BlockPos(33, 77, -1),
        new BlockPos(34, 77, 5), new BlockPos(33, 77, 12), new BlockPos(31, 77, 19),
        new BlockPos(27, 77, 25), new BlockPos(20, 77, 30), new BlockPos(14, 77, 33),
        new BlockPos(7, 77, 34)
    );

    private static final Set<BlockPos> M4_BLOCKS = Set.of(
        new BlockPos(-2, 77, 33), new BlockPos(-7, 77, 32), new BlockPos(-13, 77, 28),
        new BlockPos(-17, 77, 24), new BlockPos(-21, 77, 18), new BlockPos(-23, 77, 13),
        new BlockPos(-24, 77, 7), new BlockPos(-24, 77, 2), new BlockPos(-23, 77, -4),
        new BlockPos(-21, 77, -9), new BlockPos(-17, 77, -14), new BlockPos(-12, 77, -19),
        new BlockPos(-6, 77, -22), new BlockPos(-1, 77, -23), new BlockPos(5, 77, -24),
        new BlockPos(10, 77, -24), new BlockPos(16, 77, -22), new BlockPos(21, 77, -19),
        new BlockPos(27, 77, -15), new BlockPos(30, 77, -10), new BlockPos(32, 77, -5),
        new BlockPos(34, 77, 1), new BlockPos(34, 77, 7), new BlockPos(33, 77, 12),
        new BlockPos(31, 77, 18), new BlockPos(28, 77, 23), new BlockPos(23, 77, 28),
        new BlockPos(18, 77, 31), new BlockPos(12, 77, 33), new BlockPos(7, 77, 34)
    );

    private int count;
    private long timerTarget = -1;
    private boolean inF4Boss;
    private boolean masterMode;

    public void handleChatMessage(String raw, DungeonStateService state) {
        String plain = FORMATTING_CODES.matcher(raw).replaceAll("");
        String lower = plain.toLowerCase(Locale.ROOT);

        if (lower.contains("[crowd]") && lower.contains("thorn")) {
            inF4Boss = true;
            masterMode = state.isMasterMode();
        }

        if (lower.contains("dungeon complete") || lower.contains("team score:")) {
            reset();
        }
    }

    public void onBlockUpdate(BlockPos pos, BlockState newState, BlockState oldState) {
        if (!inF4Boss) return;

        Set<BlockPos> locations = masterMode ? M4_BLOCKS : F4_BLOCKS;
        if (!locations.contains(pos)) return;

        boolean newIsLantern = newState.is(Blocks.SEA_LANTERN);
        boolean newIsCoal = newState.is(Blocks.COAL_BLOCK);

        if (newIsLantern && (oldState == null || oldState.is(Blocks.COAL_BLOCK))) {
            count = Math.min(count + 1, locations.size());
            if (pos.equals(LAST_BLOCK)) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.level != null) {
                    timerTarget = mc.level.getGameTime() + BEAR_SPAWN_DELAY;
                }
            }
        } else if (newIsCoal && (oldState == null || oldState.is(Blocks.SEA_LANTERN))) {
            count = Math.max(count - 1, 0);
            if (pos.equals(LAST_BLOCK)) {
                timerTarget = 0;
            }
        }
    }

    public void tick(Minecraft mc, HorizonConfig config) {
        if (!config.isSpiritBearTimerEnabled() || mc == null || mc.level == null) {
            if (inF4Boss) reset();
        }
    }

    public boolean isInF4Boss() {
        return inF4Boss;
    }

    public boolean hasCountdown() {
        return timerTarget > 0;
    }

    public float getCountdownSeconds(Minecraft mc) {
        if (mc == null || mc.level == null || timerTarget <= 0) return 0f;
        long remaining = timerTarget - mc.level.getGameTime();
        return Math.max(0f, remaining / 20f);
    }

    public int getCount() {
        return count;
    }

    public int getTotal() {
        return (masterMode ? M4_BLOCKS : F4_BLOCKS).size();
    }

    public static boolean isSpiritBear(Entity e) {
        if (!(e instanceof Player)) return false;
        if (!e.isAlive()) return false;
        String name = e.getName().getString().toLowerCase(Locale.ROOT);
        return name.startsWith("spirit bear");
    }

    public void reset() {
        count = 0;
        timerTarget = -1;
        inF4Boss = false;
        masterMode = false;
    }
}
