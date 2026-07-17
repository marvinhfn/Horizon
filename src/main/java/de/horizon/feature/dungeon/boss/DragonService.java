package de.horizon.feature.dungeon.boss;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.regex.Pattern;

/**
 * M7 Dragon spawn priority, boxes, timer, and title overlay.
 * Detects dragon spawns via FLAME particles instead of scanning all entities every tick.
 */
public final class DragonService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    public enum DragonType {
        POWER ("Power",  'r', 27, 59, 0xFFFF5555, ChatFormatting.RED,
            new AABB(14.5, 13.0, 45.5, 37.5, 28.0, 70.5)),
        APEX  ("Apex",   'g', 27, 94, 0xFF55FF55, ChatFormatting.GREEN,
            new AABB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0)),
        SOUL  ("Soul",   'p', 56, 125, 0xFFAA00AA, ChatFormatting.DARK_PURPLE,
            new AABB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5)),
        ICE   ("Ice",    'b', 84, 94, 0xFF55FFFF, ChatFormatting.AQUA,
            new AABB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5)),
        FLAME ("Flame",  'o', 85, 56, 0xFFFFAA00, ChatFormatting.GOLD,
            new AABB(72.0, 8.0, 47.0, 102.0, 28.0, 76.0));

        public final String displayName;
        public final char letter;
        public final int particleX, particleZ;
        public final int color;
        public final ChatFormatting chatColor;
        public final AABB box;

        DragonType(String displayName, char letter, int particleX, int particleZ,
                   int color, ChatFormatting chatColor, AABB box) {
            this.displayName = displayName;
            this.letter = letter;
            this.particleX = particleX;
            this.particleZ = particleZ;
            this.color = color;
            this.chatColor = chatColor;
            this.box = box;
        }

        static DragonType fromParticlePos(int x, int z) {
            for (DragonType d : values()) {
                if (d.particleX == x && d.particleZ == z) return d;
            }
            return null;
        }

        static DragonType fromLetter(char c) {
            for (DragonType d : values()) {
                if (d.letter == c) return d;
            }
            return null;
        }

        /** Get the dragon type closest to an entity position. */
        static DragonType fromEntityPos(double ex, double ez) {
            DragonType best = null;
            double bestDist = 30;
            for (DragonType d : values()) {
                double dist = Math.sqrt((ex - d.particleX) * (ex - d.particleX) + (ez - d.particleZ) * (ez - d.particleZ));
                if (dist < bestDist) {
                    bestDist = dist;
                    best = d;
                }
            }
            return best;
        }
    }

    private enum State { INACTIVE, WAITING, DRAGONS_SPAWNING }

    private State state = State.INACTIVE;
    private final Set<DragonType> spawned = EnumSet.noneOf(DragonType.class);
    private final Set<DragonType> alive = EnumSet.noneOf(DragonType.class);
    private final Map<DragonType, Long> spawnTimes = new EnumMap<>(DragonType.class);
    private final Map<DragonType, Long> particleCooldowns = new EnumMap<>(DragonType.class);
    private boolean titleShown = false;
    private int entityScanCooldown = 0;

    /**
     * Called from the particle packet handler when a FLAME particle is received.
     * Detection: FLAME particles with count=20, xDist=2, yDist=3, zDist=2, maxSpeed=0.
     * Particle Y=19 means low spawn, Y=27 means high spawn.
     */
    public void onDragonParticle(int x, int z) {
        if (state == State.INACTIVE) return;

        DragonType dragon = DragonType.fromParticlePos(x, z);
        if (dragon == null) return;

        // Cooldown: don't re-detect same dragon within 5 seconds
        long now = System.currentTimeMillis();
        Long lastSeen = particleCooldowns.get(dragon);
        if (lastSeen != null && now - lastSeen < 5000) return;
        particleCooldowns.put(dragon, now);

        if (!spawned.contains(dragon)) {
            spawned.add(dragon);
            alive.add(dragon);
            spawnTimes.put(dragon, now);
            state = State.DRAGONS_SPAWNING;
        }
    }

    public void handleChatMessage(String rawMessage, DungeonStateService dungeonState) {
        if (rawMessage == null) return;
        String plain = FORMATTING.matcher(rawMessage).replaceAll("").strip().toLowerCase(Locale.ROOT);

        // P5 start
        if (plain.contains("[boss] necron: you were right")
            || plain.contains("the wither king is respawning")) {
            if (dungeonState.isF7() && dungeonState.isInBoss()) {
                state = State.WAITING;
                titleShown = false;
            }
        }

        // Dungeon complete
        if (plain.contains("dungeon complete") || plain.contains("team score:")) {
            reset();
        }
    }

    public void tick(Minecraft mc, DungeonStateService dungeonState, HorizonConfig config) {
        if (!config.isDragonEnabled()) return;
        if (!dungeonState.isInBoss() || !dungeonState.isF7() || mc.level == null || mc.player == null) {
            if (state != State.INACTIVE) reset();
            return;
        }
        // Auto-activate when P5 is detected (in case chat trigger was missed)
        if (state == State.INACTIVE
            && dungeonState.getF7Phase() == DungeonStateService.F7Phase.P5) {
            state = State.WAITING;
            titleShown = false;
        }
        if (state == State.INACTIVE) return;

        // Show title when dragons first spawn
        if (state == State.DRAGONS_SPAWNING && !titleShown && !alive.isEmpty()
            && config.isDragonSpawnAlert()) {
            titleShown = true;
            showDragonTitle(mc, alive, config);
        }

        // Periodic entity scan (every 10 ticks = 0.5s) to track alive/dead dragons
        if (--entityScanCooldown <= 0) {
            entityScanCooldown = 10;
            updateAliveFromEntities(mc);
        }
    }

    private void updateAliveFromEntities(Minecraft mc) {
        if (mc.level == null) return;
        Set<DragonType> found = EnumSet.noneOf(DragonType.class);
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getType() != EntityType.ENDER_DRAGON) continue;
            if (!e.isAlive()) continue;
            DragonType type = DragonType.fromEntityPos(e.getX(), e.getZ());
            if (type != null && spawned.contains(type)) {
                found.add(type);
            }
        }
        // Only update alive set if we've seen at least one dragon spawn
        if (!spawned.isEmpty()) {
            alive.clear();
            alive.addAll(found);
        }
    }

    private void showDragonTitle(Minecraft mc, Set<DragonType> dragons, HorizonConfig config) {
        if (mc.gui == null) return;

        // Determine priority dragon
        DragonType priority = getPriorityDragon(dragons, config);
        if (priority == null) return;

        StringBuilder sb = new StringBuilder();
        for (DragonType d : DragonType.values()) {
            if (!dragons.contains(d)) continue;
            if (!sb.isEmpty()) sb.append(" + ");
            sb.append(d.displayName);
        }

        mc.gui.setTitle(Component.literal(priority.displayName + " Dragon!").withStyle(priority.chatColor));
        mc.gui.setSubtitle(Component.literal(sb.toString()).withStyle(ChatFormatting.WHITE));
        mc.gui.setTimes(5, 40, 10);
    }

    private DragonType getPriorityDragon(Set<DragonType> dragons, HorizonConfig config) {
        if (!config.isDragonPriority()) {
            // No priority sorting — just return the first spawned
            return dragons.stream().findFirst().orElse(null);
        }
        // Use split priority by default (most common strategy)
        String prio = config.getDragonSplitPrio();
        for (int i = 0; i < prio.length(); i++) {
            DragonType d = DragonType.fromLetter(prio.charAt(i));
            if (d != null && dragons.contains(d)) return d;
        }
        return dragons.stream().findFirst().orElse(null);
    }

    public void renderWorld(LevelRenderContext ctx, HorizonConfig config) {
        if (!config.isDragonEnabled() || state == State.INACTIVE) return;

        long now = System.currentTimeMillis();

        for (DragonType d : DragonType.values()) {
            boolean isAlive = alive.contains(d);
            boolean wasSpawned = spawned.contains(d);

            // Draw statue box
            if (config.isDragonBoxes()) {
                int boxColor = isAlive ? (d.color & 0x00FFFFFF) | 0x40000000
                             : wasSpawned ? 0x20555555
                             : (d.color & 0x00FFFFFF) | 0x18000000;
                DungeonRenderUtil.drawBox(ctx, d.box, boxColor, 1, false);
            }

            // Draw label above the statue
            double labelX = (d.box.minX + d.box.maxX) / 2.0;
            double labelY = d.box.maxY + 2.0;
            double labelZ = (d.box.minZ + d.box.maxZ) / 2.0;

            String colorCode = isAlive ? d.chatColor.toString()
                             : wasSpawned ? "\u00a78"
                             : "\u00a77";

            String label = colorCode + d.displayName;
            if (isAlive && config.isDragonTimer()) {
                Long spawnTime = spawnTimes.get(d);
                if (spawnTime != null) {
                    float elapsed = (now - spawnTime) / 1000f;
                    label += String.format(" %.1fs", elapsed);
                }
            } else if (wasSpawned && !isAlive) {
                label += " \u2714"; // checkmark = dead
            }

            DungeonRenderUtil.drawString(ctx, label, labelX, labelY, labelZ);
        }
    }

    public boolean isActive() { return state != State.INACTIVE; }

    public void reset() {
        state = State.INACTIVE;
        spawned.clear();
        alive.clear();
        spawnTimes.clear();
        particleCooldowns.clear();
        titleShown = false;
        entityScanCooldown = 0;
    }
}
