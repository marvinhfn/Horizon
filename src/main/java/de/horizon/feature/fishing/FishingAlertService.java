package de.horizon.feature.fishing;

import de.horizon.HorizonSounds;
import de.horizon.config.HorizonConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class FishingAlertService {
    private static final Pattern FORMATTING_STRIP = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final double SCAN_RADIUS = 64.0;
    private static final long CHAT_DEDUP_MS = 3000L;

    /**
     * Unique spawn message fragments for regular (non-elusive) sea creatures that do NOT use
     * the standard "caught a/an X" format.
     * Atoll Croaker ("takes the bait") is already covered by the isTakesTheBait check.
     */
    private static final Set<String> SEA_CREATURE_SPAWN_MESSAGES = Set.of(
        // Lotus Atoll
        "takes hold of your bobber",           // Drowned Captain
        "emerges, ready to protect the atoll", // Lotus Guardian
        "what even is that",                   // gorF
        // Crimson Isle
        "a magma slug",                        // Magma Slug
        "a moogma appears",                    // Moogma
        "a lava leech emerges",                // Lava Leech
        "a pyroclastic worm surfaces",         // Pyroclastic Worm
        "a fire eel slithers",                 // Fire Eel
        "taurus and his steed",                // Taurus
        "a lava pigman has surfaced",          // Lava Pigman
        // Crystal Hollows
        "a lava blaze has surfaced",           // Lava Blaze
        "a flaming worm surfaces"              // Flaming Worm
    );

    /** Trophy fish names that appear in catch messages (without tier prefix/suffix). */
    private static final Set<String> TROPHY_FISH_NAMES = Set.of(
        "blobfish", "gusher", "flyfish", "lavahorse", "mahi mahi", "steaming hot flounder",
        "skeleton fish", "soul of the sea", "moldfin", "slugfish", "lava flame",
        "volcanic stonefish", "vanille"
    );

    private final Set<UUID> alertedEntities = new HashSet<>();
    private final Map<String, Long> lastAnnounced = new HashMap<>();
    private int tickCount = 0;

    // ── Chat detection ────────────────────────────────────────────────────────

    public void handleChatMessage(String raw, HorizonConfig config) {
        if (!config.isFishingRareAlertEnabled()) return;
        String plain = FORMATTING_STRIP.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT);
        // Trophy frog/fish catches can share names with elusive creatures (e.g. Puddle Jumper).
        // Exclude them from triggering an alert – the entity scan will still catch the real spawn.
        if (isTrophyFrogCatch(plain) || isTrophyFishCatch(plain)) return;
        for (ElusiveSeaCreature creature : ElusiveSeaCreature.values()) {
            if (!config.isFishingCreatureEnabled(creature.id())) continue;
            String lower = creature.displayName().toLowerCase(Locale.ROOT);
            boolean match = plain.contains("caught a " + lower)
                    || plain.contains("caught an " + lower)
                    || (creature.spawnMessageFragment() != null && plain.contains(creature.spawnMessageFragment()));
            if (match) {
                markNearbyEntitiesAsAlerted(creature);
                announce(creature, config);
                return;
            }
        }
    }

    // ── Spam filter ───────────────────────────────────────────────────────────

    public boolean shouldHideMessage(String raw, HorizonConfig config) {
        String plain = FORMATTING_STRIP.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT);

        // ── GOOD / GREAT / OUTSTANDING filter ────────────────────────────────
        if (config.isHideGoodGreatOutstandingMessages()
                && (plain.contains("good catch") || plain.contains("great catch")
                    || plain.contains("outstanding catch") || plain.contains("perfect catch"))) {
            return true;
        }

        // Everything below requires a fishing catch message.
        // Hypixel uses two formats:
        //   "You caught a X!" / "Player caught an X!"  (most areas)
        //   "An inquisitive X takes the bait! (n)"     (Lotus Atoll)
        boolean isCaughtFormat = plain.contains("caught a ") || plain.contains("caught an ");
        boolean isTakesTheBait = plain.contains("takes the bait");
        boolean isSeaCreatureSpawn = SEA_CREATURE_SPAWN_MESSAGES.stream().anyMatch(plain::contains);
        boolean isElusiveSpawn = false;
        for (ElusiveSeaCreature c : ElusiveSeaCreature.values()) {
            if (c.spawnMessageFragment() != null && plain.contains(c.spawnMessageFragment())) {
                isElusiveSpawn = true;
                break;
            }
        }
        if (!isCaughtFormat && !isTakesTheBait && !isSeaCreatureSpawn && !isElusiveSpawn) return false;

        boolean isFrog = isTrophyFrogCatch(plain);
        boolean isFish = !isFrog && isTrophyFishCatch(plain);
        boolean isTrophy = isFrog || isFish;

        // ── Elusive creature catch ────────────────────────────────────────────
        if (!isTrophy) {
            for (ElusiveSeaCreature creature : ElusiveSeaCreature.values()) {
                String lower = creature.displayName().toLowerCase(Locale.ROOT);
                boolean match = plain.contains("caught a " + lower)
                        || plain.contains("caught an " + lower)
                        || (creature.spawnMessageFragment() != null && plain.contains(creature.spawnMessageFragment()));
                if (match) {
                    return config.isHideElusiveSeaCreatureMessages();
                }
            }
        }

        // ── Trophy frog / fish filter ─────────────────────────────────────────
        if (isTrophy) {
            // Diamond exception: always show diamond-tier trophies unless diamond filter is on
            if (!config.isHideFishingDiamondTrophies() && plain.contains("diamond")) return false;
            return isFrog ? config.isHideTrophyFrogMessages() : config.isHideTrophyFishMessages();
        }

        // ── Regular sea creatures (including Atoll creatures) ────────────────
        return config.isHideSeaCreatureMessages();
    }

    // ── Entity scan (catches creatures spawned by others) ─────────────────────

    public void tick(MinecraftClient mc, HorizonConfig config) {
        if (!config.isFishingRareAlertEnabled()) return;
        if (mc == null || mc.player == null || mc.world == null) return;
        if (++tickCount % SCAN_INTERVAL_TICKS != 0) return;

        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (Math.abs(entity.getX() - px) > SCAN_RADIUS
                    || Math.abs(entity.getY() - py) > SCAN_RADIUS
                    || Math.abs(entity.getZ() - pz) > SCAN_RADIUS) continue;
            if (alertedEntities.contains(entity.getUuid())) continue;
            Text nameText = entity.getCustomName();
            if (nameText == null) continue;
            String name = FORMATTING_STRIP.matcher(nameText.getString()).replaceAll("").toLowerCase(Locale.ROOT);
            for (ElusiveSeaCreature creature : ElusiveSeaCreature.values()) {
                if (!config.isFishingCreatureEnabled(creature.id())) continue;
                if (name.contains(creature.displayName().toLowerCase(Locale.ROOT))) {
                    alertedEntities.add(entity.getUuid());
                    announce(creature, config);
                    break;
                }
            }
        }
    }

    // ── Category helpers ──────────────────────────────────────────────────────

    /**
     * Returns true if the message is a trophy frog catch.
     * Hypixel prefixes these with "♔ TROPHY FROG!" or includes a tier suffix (bronze/silver…)
     * alongside a frog-named creature.
     */
    private static boolean isTrophyFrogCatch(String plain) {
        if (plain.contains("trophy frog")) return true;
        return plain.contains("frog")
                && (plain.contains(" bronze") || plain.contains(" silver")
                    || plain.contains(" gold") || plain.contains(" diamond"));
    }

    /**
     * Returns true if the message is a trophy fish catch.
     * Hypixel prefixes these with "♔ TROPHY FISH!" or the creature name is a known trophy fish.
     */
    private static boolean isTrophyFishCatch(String plain) {
        if (plain.contains("trophy fish")) return true;
        return TROPHY_FISH_NAMES.stream().anyMatch(plain::contains);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void markNearbyEntitiesAsAlerted(ElusiveSeaCreature creature) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;
        String target = creature.displayName().toLowerCase(Locale.ROOT);
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (Math.abs(entity.getX() - px) > SCAN_RADIUS
                    || Math.abs(entity.getY() - py) > SCAN_RADIUS
                    || Math.abs(entity.getZ() - pz) > SCAN_RADIUS) continue;
            Text nameText = entity.getCustomName();
            if (nameText == null) continue;
            String name = FORMATTING_STRIP.matcher(nameText.getString()).replaceAll("").toLowerCase(Locale.ROOT);
            if (name.contains(target)) {
                alertedEntities.add(entity.getUuid());
            }
        }
    }

    private void announce(ElusiveSeaCreature creature, HorizonConfig config) {
        long now = System.currentTimeMillis();
        Long last = lastAnnounced.get(creature.id());
        if (last != null && now - last < CHAT_DEDUP_MS) return;
        lastAnnounced.put(creature.id(), now);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null || mc.player == null) return;
        mc.inGameHud.setTitle(Text.literal(creature.displayName()).formatted(Formatting.AQUA));
        mc.inGameHud.setSubtitle(Text.literal("\u2736 Elusive Sea Creature \u2736").formatted(Formatting.DARK_AQUA));
        mc.inGameHud.setTitleTicks(10, 60, 20);
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        Random rng = Random.create();
        switch (config.getFishingAlertSound()) {
            case MEOW -> mc.getSoundManager().play(new PositionedSoundInstance(
                    SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.BLOCKS, 1.0f, 1.0f, rng, x, y, z));
            case CUSTOM -> mc.getSoundManager().play(new PositionedSoundInstance(
                    HorizonSounds.FISHING_ALERT_CUSTOM, SoundCategory.BLOCKS, 8.0f, 1.0f, rng, x, y, z));
            case MR -> mc.getSoundManager().play(new PositionedSoundInstance(
                    HorizonSounds.FISHING_ALERT_MR, SoundCategory.BLOCKS, 8.0f, 1.0f, rng, x, y, z));
            default -> mc.getSoundManager().play(new PositionedSoundInstance(
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 0.8f, 1.0f, rng, x, y, z));
        }
    }
}
