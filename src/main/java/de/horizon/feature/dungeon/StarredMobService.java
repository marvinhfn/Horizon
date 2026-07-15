package de.horizon.feature.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects and tracks starred mobs in Hypixel dungeons.
 * Uses entity metadata packets for reliable detection and handles special boss mobs.
 */
public final class StarredMobService {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    private static final Set<Integer> starredMobIds = new HashSet<>();
    private static final Set<Integer> checkedNameTags = new HashSet<>();
    private static final Set<Integer> batIds = new HashSet<>();
    private static final Set<Integer> felIds = new HashSet<>();

    private StarredMobService() {}

    /**
     * Scans all entities once per tick and caches which entity IDs are starred mobs.
     * Also detects bats and fels (invisible endermen named Dinnerbone).
     */
    public static void tick(Minecraft mc) {
        if (mc == null || mc.level == null) {
            clear();
            return;
        }

        batIds.clear();
        felIds.clear();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ArmorStand as) {
                if (as.getCustomName() == null) continue;
                String name = as.getCustomName().getString();
                if (name.contains("✯") && name.contains("❤")) {
                    checkStarMob(mc, as, name);
                }
            } else if (e instanceof Player p) {
                if (p == mc.player) continue;
                checkSpecialBossMob(mc, p);
            } else if (e instanceof Bat bat) {
                if (!bat.isInvisible() && !bat.isPassenger()) {
                    batIds.add(bat.getId());
                }
            } else if (e instanceof EnderMan em) {
                if (em.getCustomName() != null && em.getCustomName().getString().equals("Dinnerbone")) {
                    felIds.add(em.getId());
                }
            }
        }
    }

    private static void checkStarMob(Minecraft mc, ArmorStand armorStand, String rawName) {
        if (!checkedNameTags.add(armorStand.getId())) return;

        String name = FORMATTING_CODES.matcher(rawName).replaceAll("").toUpperCase();
        // Withermancers: real entity is id-3 (id-1 and id-2 are wither skulls)
        int offset = name.contains("WITHERMANCER") ? 3 : 1;
        int id = armorStand.getId() - offset;

        Entity mob = armorStand.level().getEntity(id);
        if (mob != null && !(mob instanceof ArmorStand) && !starredMobIds.contains(id)) {
            starredMobIds.add(id);
            return;
        }

        // Fallback: search nearby entities under the armor stand
        var nearby = armorStand.level().getEntities(
            armorStand, armorStand.getBoundingBox().move(0.0, -1.0, 0.0),
            e -> !(e instanceof ArmorStand) && !(e instanceof ExperienceOrb)
        );

        for (Entity e : nearby) {
            if (starredMobIds.contains(e.getId())) continue;
            if (e instanceof Player p) {
                if (p.isInvisible() || p.getUUID().version() != 2 || p == mc.player) continue;
            } else if (e instanceof WitherBoss || e instanceof AbstractArrow) {
                continue;
            }
            starredMobIds.add(e.getId());
            break;
        }
    }

    private static void checkSpecialBossMob(Minecraft mc, Player player) {
        if (player.isInvisible()) return;
        if (player.getUUID().version() != 2) return;
        if (mc.getConnection() == null) return;

        PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
        if (info == null || info.getProfile() == null) return;
        String profileName = info.getProfile().name();
        if (profileName == null) return;

        if (profileName.equals("Shadow Assassin")
            || profileName.equals("Lost Adventurer")
            || profileName.equals("Diamond Guy")
            || profileName.equals("King Midas")) {
            starredMobIds.add(player.getId());
        }
    }

    public static void onEntityRemoved(Entity entity) {
        starredMobIds.remove(entity.getId());
        checkedNameTags.remove(entity.getId());
        batIds.remove(entity.getId());
        felIds.remove(entity.getId());
    }

    public static void onWorldChange() {
        clear();
    }

    private static void clear() {
        starredMobIds.clear();
        checkedNameTags.clear();
        batIds.clear();
        felIds.clear();
    }

    /**
     * Returns true if this entity was identified as a starred mob.
     */
    public static boolean isStarredMob(Entity entity) {
        if (entity instanceof ArmorStand) return false;
        return starredMobIds.contains(entity.getId());
    }

    /**
     * Returns true if this entity is a tracked dungeon bat.
     */
    public static boolean isDungeonBat(Entity entity) {
        return batIds.contains(entity.getId());
    }

    /**
     * Returns true if this entity is a tracked invisible fel (Dinnerbone enderman).
     */
    public static boolean isFel(Entity entity) {
        return felIds.contains(entity.getId());
    }

    /**
     * Checks if an ArmorStand entity displays a starred mob nametag.
     */
    public static boolean isStarredNameTag(Entity entity) {
        if (!(entity instanceof ArmorStand as)) return false;
        if (as.getCustomName() == null) return false;
        String name = FORMATTING_CODES.matcher(as.getCustomName().getString()).replaceAll("");
        return name.contains("✯");
    }

    /**
     * Checks if an ArmorStand entity displays a dungeon mob nametag (has HP heart).
     */
    public static boolean isDungeonMobNameTag(Entity entity) {
        if (!(entity instanceof ArmorStand as)) return false;
        if (as.getCustomName() == null) return false;
        String name = as.getCustomName().getString();
        return name.contains("❤");
    }
}
