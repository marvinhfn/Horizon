package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Relic features for M7 P5 (Wither King):
 *
 * <ul>
 *   <li><b>Spawn timer</b> — triggers on "[BOSS] Necron: All this, for nothing..." and counts down
 *       until the relics spawn 45 game ticks later (shown by the HUD element).</li>
 *   <li><b>Place timer</b> — tracks who picked up which Corrupted relic, detects when each relic is
 *       placed at its cauldron (a relic-headed armor stand appears there), and — once all five are
 *       placed — reports in chat how long each one took, sorted fastest-first, with the {@code [HRZN]}
 *       prefix.</li>
 * </ul>
 *
 * <p>All timing is anchored to the server game tick ({@code level.getGameTime()}) so it stays aligned
 * with the actual (server-side) spawn/placement under lag, instead of drifting on the client loop.
 */
public final class RelicTimerService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final String NECRON_TRIGGER = "[boss] necron: all this, for nothing...";
    private static final int SPAWN_TICKS = 42;

    /** "PlayerName picked the Corrupted Red Relic!" */
    private static final Pattern PICKUP =
        Pattern.compile("^(\\w{3,16}) picked the Corrupted (\\w{3,6}) Relic!$");

    /** The five corrupted relics: armor-stand match XZ, cauldron block position, colour. */
    private enum Relic {
        RED("Red", ChatFormatting.RED, 0xFFFF5555, 52, 43, 51, 7, 42),
        ORANGE("Orange", ChatFormatting.GOLD, 0xFFFFAA00, 58, 43, 57, 7, 42),
        GREEN("Green", ChatFormatting.GREEN, 0xFF55FF55, 50, 45, 49, 7, 44),
        BLUE("Blue", ChatFormatting.AQUA, 0xFF55FFFF, 60, 45, 59, 7, 44),
        PURPLE("Purple", ChatFormatting.DARK_PURPLE, 0xFFAA00AA, 55, 42, 54, 7, 41);

        final String colour;
        final ChatFormatting chatColor;
        final int argb;
        final int x, z;            // armor-stand cauldron centre (XZ) for placement detection
        final int cx, cy, cz;      // cauldron block position for the box + tracer

        Relic(String colour, ChatFormatting chatColor, int argb, int x, int z, int cx, int cy, int cz) {
            this.colour = colour;
            this.chatColor = chatColor;
            this.argb = argb;
            this.x = x;
            this.z = z;
            this.cx = cx; this.cy = cy; this.cz = cz;
        }

        static Relic byColour(String colour) {
            for (Relic r : values()) if (r.colour.equalsIgnoreCase(colour)) return r;
            return null;
        }

        /** Matches a held item name like "Corrupted Red Relic". */
        static Relic byHeldName(String name) {
            if (name == null) return null;
            for (Relic r : values()) {
                if (name.equalsIgnoreCase("Corrupted " + r.colour + " Relic")) return r;
            }
            return null;
        }

        boolean atCauldron(double px, double pz) {
            double dx = px - x, dz = pz - z;
            return Math.sqrt(dx * dx + dz * dz) < 1.5;
        }

        AABB cauldronBox() {
            return new AABB(cx, cy, cz, cx + 1, cy + 1, cz + 1);
        }
    }

    private static final class Entry {
        final Relic relic;
        final String player;
        double placeSeconds = 0.0;
        boolean placed = false;

        Entry(Relic relic, String player) {
            this.relic = relic;
            this.player = player;
        }
    }

    private long spawnTick = -1L; // game tick at which the relics spawn
    private long p5StartTick = -1L;
    private final List<Entry> entries = new ArrayList<>();

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
    }

    public void handleChatMessage(String rawMessage, DungeonStateService dungeonState, HorizonConfig config) {
        if (rawMessage == null) return;
        // Gate on the floor only — the boss triggers only appear in the boss, and isInBoss()
        // flickers false in the boss room (which was swallowing them).
        if (!dungeonState.isF7()) return;

        String plain = FORMATTING.matcher(rawMessage).replaceAll("").strip();
        String lower = plain.toLowerCase(Locale.ROOT);

        if (lower.contains(NECRON_TRIGGER)) {
            p5StartTick = gameTime();
            if (config.isRelicTimerEnabled()) spawnTick = gameTime() + SPAWN_TICKS;
            entries.clear();
            return;
        }

        if (config.isRelicPlaceTimerEnabled()) {
            Matcher m = PICKUP.matcher(plain);
            if (m.matches()) {
                Relic relic = Relic.byColour(m.group(2));
                if (relic != null && entries.stream().noneMatch(e -> e.relic == relic)) {
                    entries.add(new Entry(relic, m.group(1)));
                }
            }
        }
    }

    /** Detects relic placements and, once all five are placed, reports the times in chat. */
    public void tick(Minecraft mc, DungeonStateService dungeonState, HorizonConfig config) {
        if (!config.isRelicPlaceTimerEnabled() || mc.level == null) return;
        // p5StartTick is only set by the Necron P5 trigger, so it already implies we're in P5 — don't
        // also gate on f7Phase (that could be flaky and swallow the whole report).
        if (entries.isEmpty() || p5StartTick < 0) return;

        // Report each relic the moment it's placed (waiting for all five often never completed, so
        // nothing was ever written to chat).
        for (Entry entry : entries) {
            if (entry.placed) continue;
            if (!relicPlaced(mc, entry.relic)) continue;
            entry.placed = true;
            entry.placeSeconds = round2((gameTime() - p5StartTick) / 20.0);
            reportOne(mc, entry);
        }
    }

    /** True if a relic-headed armor stand is standing on this relic's cauldron. */
    private static boolean relicPlaced(Minecraft mc, Relic relic) {
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand stand)) continue;
            String head = stand.getItemBySlot(EquipmentSlot.HEAD).getHoverName().getString();
            if (!head.contains("Relic")) continue;
            if (relic.atCauldron(stand.getX(), stand.getZ())) return true;
        }
        return false;
    }

    private void reportOne(Minecraft mc, Entry entry) {
        if (mc.player == null) return;
        Component line = Component.literal("[HRZN] ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(entry.relic.colour + " Relic").withStyle(entry.relic.chatColor))
            .append(Component.literal(" placed in ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.format(Locale.ROOT, "%.2fs", entry.placeSeconds))
                .withStyle(ChatFormatting.YELLOW));
        mc.player.sendSystemMessage(line);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ── World render: box + tracer to the held relic's cauldron ──────────────────

    public void renderWorld(LevelRenderContext ctx, DungeonStateService dungeonState, HorizonConfig config) {
        if (!config.isRelicTimerEnabled()) return;
        if (dungeonState.getF7Phase() != DungeonStateService.F7Phase.P5) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        String held = mc.player.getInventory().getItem(8).getHoverName().getString();
        Relic relic = Relic.byHeldName(held);
        if (relic == null) return;

        int fill = (relic.argb & 0x00FFFFFF) | 0x40000000;
        int outline = relic.argb | 0xFF000000;
        DungeonRenderUtil.drawBoxesBatched(ctx,
            List.of(new DungeonRenderUtil.BoxSpec(relic.cauldronBox(), fill, outline)), true, 3.5f);

        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        Vec3 target = new Vec3(relic.cx + 0.5, relic.cy + 0.5, relic.cz + 0.5);
        DungeonRenderUtil.drawLine(ctx, List.of(cam, target), outline, true, 3.0f);
    }

    // ── Spawn timer (HUD) ────────────────────────────────────────────────────────

    public boolean isActive() {
        return spawnTick > 0 && gameTime() < spawnTick;
    }

    public float getSecondsRemaining() {
        long remaining = spawnTick - gameTime();
        return remaining > 0 ? remaining * 0.05f : 0f;
    }

    public void reset() {
        spawnTick = -1L;
        p5StartTick = -1L;
        entries.clear();
    }
}
