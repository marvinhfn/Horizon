package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * M7 Phase 5 Wither Dragons ("Wither King") — ESP, spawn timer and priority for the M7 wither dragons.
 *
 * <p>Each of the five statue positions has its own state machine: a FLAME spawn particle flips it to
 * {@link State#SPAWNING}; when an ENDER_DRAGON entity appears inside the statue box it becomes
 * {@link State#ALIVE} (health tracked from the entity); it dies when its health hits 0, its chin
 * slab turns to air, or the entity unloads. Renders the statue box, health + time-alive label, and a
 * tracer to the priority dragon.
 */
public final class DragonService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");

    /** The five wither dragons (exact spawn/box data). */
    public enum WitherDragon {
        POWER("Power", 'r', ChatFormatting.RED,   0xFFFF5555,
            27, 59, new Vec3(27.0, 14.0, 59.0), new BlockPos(32, 19, 59),
            new AABB(14.5, 13.0, 45.5, 39.5, 28.0, 70.5)),
        FLAME("Flame", 'o', ChatFormatting.GOLD,  0xFFFFAA00,
            85, 56, new Vec3(85.0, 14.0, 56.0), new BlockPos(80, 19, 56),
            new AABB(72.0, 8.0, 47.0, 102.0, 28.0, 77.0)),
        APEX ("Apex",  'g', ChatFormatting.GREEN, 0xFF55FF55,
            27, 94, new Vec3(27.0, 14.0, 94.0), new BlockPos(32, 18, 94),
            new AABB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0)),
        ICE  ("Ice",   'b', ChatFormatting.AQUA,  0xFF55FFFF,
            84, 94, new Vec3(84.0, 14.0, 94.0), new BlockPos(79, 19, 94),
            new AABB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5)),
        SOUL ("Soul",  'p', ChatFormatting.DARK_PURPLE, 0xFFAA00AA,
            56, 125, new Vec3(56.0, 14.0, 125.0), new BlockPos(56, 18, 128),
            new AABB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5));

        public final String displayName;
        public final char letter;
        public final ChatFormatting chatColor;
        public final int color;
        public final int particleX, particleZ;
        public final Vec3 spawnPos;
        public final BlockPos chin;
        public final AABB box;

        WitherDragon(String displayName, char letter, ChatFormatting chatColor, int color,
                     int particleX, int particleZ, Vec3 spawnPos, BlockPos chin, AABB box) {
            this.displayName = displayName;
            this.letter = letter;
            this.chatColor = chatColor;
            this.color = color;
            this.particleX = particleX;
            this.particleZ = particleZ;
            this.spawnPos = spawnPos;
            this.chin = chin;
            this.box = box;
        }

        static WitherDragon byParticle(int x, int z) {
            for (WitherDragon d : values()) if (d.particleX == x && d.particleZ == z) return d;
            return null;
        }

        static WitherDragon byChin(BlockPos pos) {
            for (WitherDragon d : values()) if (d.chin.equals(pos)) return d;
            return null;
        }

        boolean containsXZ(double x, double z) {
            return x >= box.minX && x <= box.maxX && z >= box.minZ && z <= box.maxZ;
        }
    }

    private enum State { SPAWNING, ALIVE, DEAD }

    private static final class Info {
        State state = State.SPAWNING;
        long spawnedTime = 0L;   // ms when it became ALIVE
        float health = 0f;
        float maxHealth = 0f;
        int missedScans = 0;     // consecutive entity scans where the dragon wasn't found
    }

    private final Map<WitherDragon, Info> dragons = new EnumMap<>(WitherDragon.class);
    private boolean active = false;
    private int scanCooldown = 0;

    // ── Detection ────────────────────────────────────────────────────────────────

    /**
     * FLAME spawn particle (count=20, y=19, xDist=2, yDist=3, zDist=2, maxSpeed=0) at a statue's
     * x/z → that dragon is SPAWNING. Called from the particle packet hook.
     */
    public void onDragonParticle(int x, int z) {
        WitherDragon d = WitherDragon.byParticle(x, z);
        if (d == null) return;
        Info info = dragons.get(d);
        if (info == null || info.state == State.DEAD) {
            info = new Info();
            dragons.put(d, info);
        }
        active = true;
    }

    /** Chin slab → air marks the dragon dead (block-based death detection). */
    public void onBlockUpdate(BlockPos pos, BlockState newState) {
        if (!newState.isAir()) return;
        WitherDragon d = WitherDragon.byChin(pos);
        if (d == null) return;
        Info info = dragons.get(d);
        if (info != null) info.state = State.DEAD;
    }

    public void handleChatMessage(String rawMessage, DungeonStateService dungeonState) {
        if (rawMessage == null) return;
        String plain = FORMATTING.matcher(rawMessage).replaceAll("").strip().toLowerCase(Locale.ROOT);
        if (plain.contains("[boss] necron: you were right")
            || plain.contains("the wither king is respawning")) {
            if (dungeonState.isF7() && dungeonState.isInBoss()) active = true;
        }
        if (plain.contains("dungeon complete") || plain.contains("team score:")) reset();
    }

    public void tick(Minecraft mc, DungeonStateService dungeonState, HorizonConfig config) {
        if (!config.isDragonEnabled()) return;
        if (mc.level == null || mc.player == null || !dungeonState.isInBoss() || !dungeonState.isF7()) {
            if (active) reset();
            return;
        }
        // Auto-arm in P5 even if the chat trigger was missed.
        if (!active && dungeonState.getF7Phase() == DungeonStateService.F7Phase.P5) active = true;
        if (!active) return;

        if (--scanCooldown > 0) return;
        scanCooldown = 4; // every 4 ticks (~0.2s)

        // Match live ender dragons to statue boxes → ALIVE + health.
        boolean[] seen = new boolean[WitherDragon.values().length];
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getType() != EntityType.ENDER_DRAGON || !(e instanceof LivingEntity le)) continue;
            for (WitherDragon d : WitherDragon.values()) {
                if (!d.containsXZ(e.getX(), e.getZ())) continue;
                Info info = dragons.computeIfAbsent(d, k -> new Info());
                if (info.state == State.SPAWNING) {
                    info.state = State.ALIVE;
                    info.spawnedTime = System.currentTimeMillis();
                    if (config.isDragonSpawnAlert()) maybeTitle(mc, d);
                }
                if (info.state == State.ALIVE) {
                    info.health = le.getHealth();
                    info.maxHealth = le.getMaxHealth();
                    info.missedScans = 0;
                    if (info.health <= 0f) info.state = State.DEAD;
                }
                seen[d.ordinal()] = true;
                break;
            }
        }
        // An ALIVE dragon with no matching entity for several scans has died/unloaded.
        for (WitherDragon d : WitherDragon.values()) {
            Info info = dragons.get(d);
            if (info != null && info.state == State.ALIVE && !seen[d.ordinal()] && ++info.missedScans >= 3) {
                info.state = State.DEAD;
            }
        }
    }

    private WitherDragon lastTitled = null;

    private void maybeTitle(Minecraft mc, WitherDragon d) {
        if (mc.gui == null || d == lastTitled) return;
        lastTitled = d;
        mc.gui.setTitle(Component.literal(d.displayName + " Dragon!").withStyle(d.chatColor));
        mc.gui.setSubtitle(Component.empty());
        mc.gui.setTimes(3, 30, 8);
    }

    // ── Priority ─────────────────────────────────────────────────────────────────

    /** Priority dragon among the currently spawning/alive ones, by the configured letter order. */
    private WitherDragon priorityDragon(HorizonConfig config) {
        String order = config.isDragonPriority() ? config.getDragonSplitPrio() : config.getDragonNoSplitPrio();
        for (int i = 0; i < order.length(); i++) {
            for (WitherDragon d : WitherDragon.values()) {
                if (d.letter != order.charAt(i)) continue;
                Info info = dragons.get(d);
                if (info != null && info.state != State.DEAD) return d;
            }
        }
        return null;
    }

    // ── Rendering ────────────────────────────────────────────────────────────────

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isDragonEnabled() || !active) return;

        long now = System.currentTimeMillis();
        WitherDragon priority = priorityDragon(config);

        for (Map.Entry<WitherDragon, Info> entry : dragons.entrySet()) {
            WitherDragon d = entry.getKey();
            Info info = entry.getValue();
            if (info.state == State.DEAD) continue;

            boolean alive = info.state == State.ALIVE;

            if (config.isDragonBoxes()) {
                int fill = (d.color & 0x00FFFFFF) | (alive ? 0x40000000 : 0x18000000);
                DungeonRenderUtil.drawBox(ctx, d.box, fill, 1, false);
            }

            StringBuilder label = new StringBuilder();
            label.append(d.chatColor).append(d.displayName);
            if (alive && config.isDragonHealth() && info.maxHealth > 0f) {
                label.append(' ').append(healthColor(info.health, info.maxHealth)).append(abbreviate(info.health));
            }
            if (alive && config.isDragonTimer() && info.spawnedTime > 0) {
                label.append(String.format(Locale.ROOT, " §7%.1fs", (now - info.spawnedTime) / 1000f));
            } else if (!alive) {
                label.append(" §7...");
            }
            double lx = (d.box.minX + d.box.maxX) / 2.0;
            double ly = d.box.maxY + 2.0;
            double lz = (d.box.minZ + d.box.maxZ) / 2.0;
            DungeonRenderUtil.drawString(ctx, label.toString(), lx, ly, lz);
        }

        // Tracer to the priority dragon's spawn point.
        if (config.isDragonTracer() && priority != null) {
            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            Vec3 target = priority.spawnPos.add(0.5, 3.5, 0.5);
            DungeonRenderUtil.drawLine(ctx, List.of(cam, target), priority.color, true, 2.5f);
        }
    }

    private static String healthColor(float hp, float max) {
        float frac = hp / max;
        if (frac > 0.5f) return "§a";
        if (frac > 0.25f) return "§e";
        if (frac > 0.1f) return "§6";
        return "§c";
    }

    private static String abbreviate(float v) {
        if (v >= 1_000_000_000f) return String.format(Locale.ROOT, "%.1fb", v / 1_000_000_000f);
        if (v >= 1_000_000f) return String.format(Locale.ROOT, "%.1fm", v / 1_000_000f);
        if (v >= 1_000f) return String.format(Locale.ROOT, "%.1fk", v / 1_000f);
        return String.valueOf((int) v);
    }

    public boolean isActive() { return active; }

    public void reset() {
        dragons.clear();
        active = false;
        scanCooldown = 0;
        lastTitled = null;
    }
}
