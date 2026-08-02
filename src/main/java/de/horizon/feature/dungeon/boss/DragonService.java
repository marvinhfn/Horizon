package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.TeammateGlowService;
import de.horizon.feature.dungeon.TeammateGlowService.DungeonClass;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * M7 Phase 5 Wither Dragons ("Wither King") — spawn prediction, ESP, health and priority.
 *
 * <p>Each of the five statue positions has its own state machine. A FLAME spawn particle inside the
 * statue's x/z range flips it to {@link State#SPAWNING} and starts a <b>100-tick spawn countdown</b>
 * (anchored to the server game tick so it stays lag-accurate) — so the box, name and countdown are
 * shown roughly 5 seconds <em>before</em> the dragon actually materialises. When an ENDER_DRAGON
 * entity appears inside the statue box it becomes {@link State#ALIVE} (health tracked from the
 * entity); it dies when its health hits 0, its chin slab turns to air, or the entity unloads.
 */
public final class DragonService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");

    /** Ticks from the spawn particle to the dragon materialising (server ticks). */
    private static final int SPAWN_TICKS = 100;
    /** How far past 0 the countdown may run before we treat the particle as a false alarm. */
    private static final int SPAWN_GRACE = 20;

    /** The five wither dragons (exact spawn/box data). */
    public enum WitherDragon {
        POWER("Power", 'r', ChatFormatting.RED,   0xFFFF5555,
            new Vec3(27.0, 14.0, 59.0), new BlockPos(32, 19, 59),
            new AABB(14.5, 13.0, 45.5, 39.5, 28.0, 70.5), 24.0, 30.0, 56.0, 62.0),
        FLAME("Flame", 'o', ChatFormatting.GOLD,  0xFFFFAA00,
            new Vec3(85.0, 14.0, 56.0), new BlockPos(80, 19, 56),
            new AABB(72.0, 8.0, 47.0, 102.0, 28.0, 77.0), 82.0, 88.0, 53.0, 59.0),
        APEX ("Apex",  'g', ChatFormatting.GREEN, 0xFF55FF55,
            new Vec3(27.0, 14.0, 94.0), new BlockPos(32, 18, 94),
            new AABB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0), 23.0, 29.0, 91.0, 97.0),
        ICE  ("Ice",   'b', ChatFormatting.AQUA,  0xFF55FFFF,
            new Vec3(84.0, 14.0, 94.0), new BlockPos(79, 19, 94),
            new AABB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5), 82.0, 88.0, 91.0, 97.0),
        SOUL ("Soul",  'p', ChatFormatting.DARK_PURPLE, 0xFFAA00AA,
            new Vec3(56.0, 14.0, 125.0), new BlockPos(56, 18, 128),
            new AABB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5), 53.0, 59.0, 122.0, 128.0);

        public final String displayName;
        public final char letter;
        public final ChatFormatting chatColor;
        public final int color;
        public final Vec3 spawnPos;
        public final BlockPos chin;
        public final AABB box;
        public final double minX, maxX, minZ, maxZ;

        WitherDragon(String displayName, char letter, ChatFormatting chatColor, int color,
                     Vec3 spawnPos, BlockPos chin, AABB box,
                     double minX, double maxX, double minZ, double maxZ) {
            this.displayName = displayName;
            this.letter = letter;
            this.chatColor = chatColor;
            this.color = color;
            this.spawnPos = spawnPos;
            this.chin = chin;
            this.box = box;
            this.minX = minX; this.maxX = maxX; this.minZ = minZ; this.maxZ = maxZ;
        }

        boolean particleInRange(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
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
        long spawnAnchorTick = 0L;  // game tick when the spawn particle was first seen
        long aliveTick = 0L;        // game tick when it materialised (lag-accurate time-alive)
        float health = 0f;
        float maxHealth = 0f;
        int missedScans = 0;        // consecutive entity scans where the dragon wasn't found
    }

    private final Map<WitherDragon, Info> dragons = new EnumMap<>(WitherDragon.class);
    private boolean active = false;
    private int scanCooldown = 0;

    // Cached from tick() for the power-based priority.
    private double currentPower = 0.0;
    private DungeonClass currentClass = null;

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
    }

    // ── Detection ────────────────────────────────────────────────────────────────

    /**
     * FLAME spawn particle (count=20, y=19, xDist=2, yDist=3, zDist=2, maxSpeed=0) inside a statue's
     * x/z range → that dragon starts SPAWNING with a fresh 100-tick countdown. Called from the
     * particle packet hook.
     */
    public void onDragonParticle(int x, int z) {
        for (WitherDragon d : WitherDragon.values()) {
            if (!d.particleInRange(x, z)) continue;
            Info info = dragons.get(d);
            if (info == null || info.state == State.DEAD) {
                info = new Info();
                info.state = State.SPAWNING;
                info.spawnAnchorTick = gameTime();
                dragons.put(d, info);
                // The spawn title is fired from tick() for the PRIORITY dragon (the one the timer
                // tracks), not per-particle — so it matches the timer instead of whichever spawns first.
            }
            active = true;
            return;
        }
    }

    /** Cached from tick() so {@link #onDragonParticle} (no config arg) can honour the alert toggle. */
    private boolean spawnAlertOn = true;

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

    private int timeToSpawn(Info info) {
        return SPAWN_TICKS - (int) (gameTime() - info.spawnAnchorTick);
    }

    public void tick(Minecraft mc, DungeonStateService dungeonState, TeammateGlowService glow, HorizonConfig config) {
        if (!config.isDragonEnabled() || mc.level == null || mc.player == null) return;

        // Gate ONLY on the floor (7, incl. Master). Do NOT gate/reset on isInBoss() — it flickers
        // false in the boss room and any reset() then wipes the tracked dragons, so nothing showed.
        if (!dungeonState.isF7()) {
            if (active) reset();
            return;
        }

        spawnAlertOn = config.isDragonSpawnAlert();
        currentPower = dungeonState.getPowerBlessing() + (dungeonState.hasTimeBlessing() ? 2.5 : 0.0);
        currentClass = glow == null ? null : glow.getSelfClass();

        // Expire stale SPAWNING dragons whose entity never materialised (false-alarm particle).
        for (Info info : dragons.values()) {
            if (info.state == State.SPAWNING && timeToSpawn(info) <= -SPAWN_GRACE) info.state = State.DEAD;
        }

        // Spawn title for the PRIORITY spawning dragon (identical to the timer's hudDragon), fired once
        // during the pre-spawn countdown so the title always matches the dragon whose timer is running.
        if (spawnAlertOn) {
            boolean anyAlive = false;
            for (Info info : dragons.values()) if (info.state == State.ALIVE) { anyAlive = true; break; }
            if (!anyAlive) {
                WitherDragon prio = hudDragon(config);
                if (prio != null && prio != lastTitled) maybeTitle(mc, prio);
            }
        }

        if (--scanCooldown > 0) return;
        scanCooldown = 4; // every 4 ticks (~0.2s)

        // Match live ender dragons to statue boxes → ALIVE + health. Seeing a dragon here also arms
        // the solver (P5), robust against missed phase-chat / flaky boss detection.
        boolean[] seen = new boolean[WitherDragon.values().length];
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getType() != EntityType.ENDER_DRAGON || !(e instanceof LivingEntity le)) continue;
            for (WitherDragon d : WitherDragon.values()) {
                if (!d.containsXZ(e.getX(), e.getZ())) continue;
                active = true;
                Info info = dragons.computeIfAbsent(d, k -> {
                    Info i = new Info();
                    i.spawnAnchorTick = gameTime();
                    return i;
                });
                if (info.state != State.ALIVE) {
                    // Title already fired at particle time (before spawn); just flip to ALIVE here.
                    info.state = State.ALIVE;
                    info.aliveTick = gameTime();
                }
                info.health = le.getHealth();
                info.maxHealth = le.getMaxHealth();
                info.missedScans = 0;
                if (info.health <= 0f) info.state = State.DEAD;
                seen[d.ordinal()] = true;
                break;
            }
        }

        // Also arm from the P5 phase-chat (so SPAWNING boxes appear before the entity does).
        if (!active && dungeonState.getF7Phase() == DungeonStateService.F7Phase.P5) active = true;
        if (!active) return;

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

    // Fixed default order (non-priority): Red, Orange, Blue, Purple, Green.
    private static final List<WitherDragon> DEFAULT_ORDER =
        List.of(WitherDragon.POWER, WitherDragon.FLAME, WitherDragon.ICE, WitherDragon.SOUL, WitherDragon.APEX);
    // The "power" order: Orange, Green, Red, Blue, Purple.
    private static final List<WitherDragon> POWER_ORDER =
        List.of(WitherDragon.FLAME, WitherDragon.APEX, WitherDragon.POWER, WitherDragon.ICE, WitherDragon.SOUL);

    /** Priority dragon among the currently spawning/alive ones (power-based when enabled). */
    private WitherDragon priorityDragon(HorizonConfig config) {
        List<WitherDragon> candidates = new ArrayList<>();
        for (WitherDragon d : WitherDragon.values()) {
            Info info = dragons.get(d);
            if (info != null && info.state != State.DEAD) candidates.add(d);
        }
        return computePriority(candidates, config);
    }

    /**
     * Order the candidate dragons by priority and return the top one.
     * When the priority toggle is off, uses the fixed default order. When on, chooses between the
     * power order (and its reverse) based on the current Power blessing, class and the configured
     * thresholds, with the purple solo-debuff reversal for Tank/Healer.
     */
    private WitherDragon computePriority(List<WitherDragon> candidates, HorizonConfig config) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        if (!config.isDragonPriority()) {
            return first(candidates, DEFAULT_ORDER, false);
        }

        boolean hasPurple = candidates.contains(WitherDragon.SOUL);
        double power = currentPower;
        List<WitherDragon> priorityList;
        if (power >= config.getDragonNormalPower()
            || (hasPurple && power >= config.getDragonEasyPower())) {
            boolean forward = currentClass == DungeonClass.BERSERK || currentClass == DungeonClass.MAGE;
            priorityList = forward ? POWER_ORDER : reversed(POWER_ORDER);
        } else {
            priorityList = DEFAULT_ORDER;
        }

        boolean descending = false;
        if (power >= config.getDragonEasyPower()) {
            boolean onSplit = hasPurple || config.isDragonSoloDebuffOnAll();
            if (config.getDragonSoloDebuff() == 1 && currentClass == DungeonClass.TANK && onSplit) descending = true;
            else if (currentClass == DungeonClass.HEALER && onSplit) descending = true;
        }
        return first(candidates, priorityList, descending);
    }

    private static WitherDragon first(List<WitherDragon> candidates, List<WitherDragon> order, boolean descending) {
        WitherDragon best = null;
        int bestIdx = descending ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (WitherDragon d : candidates) {
            int idx = order.indexOf(d);
            if (idx < 0) idx = order.size();
            if (descending ? idx > bestIdx : idx < bestIdx) { bestIdx = idx; best = d; }
        }
        return best;
    }

    private static List<WitherDragon> reversed(List<WitherDragon> in) {
        List<WitherDragon> out = new ArrayList<>(in);
        java.util.Collections.reverse(out);
        return out;
    }

    // ── Rendering ────────────────────────────────────────────────────────────────

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isDragonEnabled() || !active) return;

        WitherDragon priority = priorityDragon(config);
        List<DungeonRenderUtil.BoxSpec> boxes = new ArrayList<>();

        for (Map.Entry<WitherDragon, Info> entry : dragons.entrySet()) {
            WitherDragon d = entry.getKey();
            Info info = entry.getValue();
            if (info.state == State.DEAD) continue;

            boolean alive = info.state == State.ALIVE;

            if (config.isDragonBoxes()) {
                // Full-colour, thick outline (opaque) with a faint fill so it reads through the statue.
                int fill = (d.color & 0x00FFFFFF) | 0x25000000;
                int outline = d.color | 0xFF000000;
                boxes.add(new DungeonRenderUtil.BoxSpec(d.box, fill, outline));
            }

            double lx = (d.box.minX + d.box.maxX) / 2.0;
            double lz = (d.box.minZ + d.box.maxZ) / 2.0;

            if (alive) {
                StringBuilder label = new StringBuilder();
                label.append(d.chatColor).append(d.displayName);
                if (config.isDragonHealth() && info.maxHealth > 0f) {
                    label.append(' ').append(healthColor(info.health, info.maxHealth)).append(abbreviate(info.health));
                }
                if (info.aliveTick > 0) {
                    label.append(String.format(Locale.ROOT, " §7%.1fs", (gameTime() - info.aliveTick) / 20f));
                }
                DungeonRenderUtil.drawString(ctx, label.toString(), lx, d.box.maxY + 2.0, lz);
            } else if (config.isDragonTimer()) {
                int tts = timeToSpawn(info);
                if (tts > 0) {
                    // Countdown shown ABOVE the spawn point, before the dragon materialises.
                    String label = String.format(Locale.ROOT, "%s%s: §e%.1fs", d.chatColor, d.displayName, tts / 20f);
                    DungeonRenderUtil.drawString(ctx, label, d.spawnPos.x, d.spawnPos.y + 3.0, d.spawnPos.z);
                }
            }
        }

        if (!boxes.isEmpty()) {
            DungeonRenderUtil.drawBoxesBatched(ctx, boxes, true, 3.5f);
        }

        // Tracer to the priority dragon's spawn point.
        if (config.isDragonTracer() && priority != null) {
            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            Vec3 target = priority.spawnPos.add(0.5, 3.5, 0.5);
            DungeonRenderUtil.drawLine(ctx, List.of(cam, target), priority.color, true, 3.0f);
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

    // ── Spawn-countdown HUD ──────────────────────────────────────────────────────

    /** The spawning dragon whose countdown the HUD should show (priority order among spawning). */
    private WitherDragon hudDragon(HorizonConfig config) {
        List<WitherDragon> spawning = new ArrayList<>();
        for (WitherDragon d : WitherDragon.values()) {
            Info info = dragons.get(d);
            if (info != null && info.state == State.SPAWNING && timeToSpawn(info) > 0) spawning.add(d);
        }
        return computePriority(spawning, config);
    }

    public boolean isSpawnTimerActive(HorizonConfig config) {
        return active && config.isDragonEnabled() && config.isDragonTimer() && hudDragon(config) != null;
    }

    /** e.g. {@code "§cPower §e2.5s"} for the priority spawning dragon, or {@code ""}. */
    public String getSpawnTimerText(HorizonConfig config) {
        WitherDragon d = hudDragon(config);
        if (d == null) return "";
        Info info = dragons.get(d);
        return String.format(Locale.ROOT, "%s%s §e%.1fs", d.chatColor, d.displayName, timeToSpawn(info) / 20f);
    }

    public boolean isActive() { return active; }

    public void reset() {
        dragons.clear();
        active = false;
        scanCooldown = 0;
        lastTitled = null;
    }
}
