package de.horizon.feature.chat;

import de.horizon.config.HorizonConfig;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SpamHider {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");

    public boolean shouldHide(String rawMessage, HorizonConfig config) {
        if (!config.isAntiSpamEnabled()) {
            return false;
        }

        String message = FORMATTING_CODES.matcher(rawMessage == null ? "" : rawMessage)
            .replaceAll("")
            .strip()
            .toLowerCase(Locale.ROOT);
        return isBlocksInTheWay(message, config)
            || isAbilitySpam(message, config)
            || isManaSpam(message, config)
            || isCooldownSpam(message, config)
            || isBlessingSpam(message, config)
            || isDungeonPickupSpam(message, config)
            || isAutopetSpam(message, config)
            || isFullStatusSpam(message, config)
            || isEffectSpam(message, config)
            || isHealingSpam(message, config)
            || isDungeonEventSpam(message, config)
            || isLockedChestSpam(message, config);
    }

    private boolean isBlocksInTheWay(String message, HorizonConfig config) {
        return config.isHideBlocksInTheWayMessages()
            && message.contains("there are blocks in the way");
    }

    private boolean isAbilitySpam(String message, HorizonConfig config) {
        if (!config.isHideAbilityMessages()) {
            return false;
        }

        return message.contains("your player was warped into a wall")
            || message.contains("you cannot teleport while in combat")
            || message.contains("you cannot use this while in combat")
            || message.contains("you used your ")
            || message.contains("wither impact")
            || message.contains("implosion")
            || message.contains("shadow warp")
            || message.contains("wither shield")
            || message.contains("this ability is currently disabled")
            || message.contains("you cannot use abilities in this room")
            || message.contains("there are blocks in the way");
    }

    private boolean isManaSpam(String message, HorizonConfig config) {
        if (!config.isHideManaMessages()) {
            return false;
        }

        return message.contains("not enough mana")
            || message.contains("you need more mana")
            || message.contains("you do not have enough mana");
    }

    private boolean isCooldownSpam(String message, HorizonConfig config) {
        if (!config.isHideCooldownMessages()) {
            return false;
        }

        return message.contains("is on cooldown")
            || message.contains("ability is on cooldown")
            || message.contains("cooldown for")
            || message.contains("is now ready to use again");
    }

    private boolean isBlessingSpam(String message, HorizonConfig config) {
        if (!config.isHideBlessingMessages()) {
            return false;
        }

        return message.contains("blessing of ")
            || message.contains("blessing found")
            || message.contains("has obtained blessing");
    }

    private boolean isDungeonPickupSpam(String message, HorizonConfig config) {
        if (!config.isHideDungeonPickupMessages()) {
            return false;
        }

        return message.contains("picked up a wither key")
            || message.contains("picked up a blood key")
            || message.contains("picked up a superboom tnt")
            || message.contains("picked up superboom tnt")
            || message.contains("picked up a revive stone");
    }

    private boolean isAutopetSpam(String message, HorizonConfig config) {
        return config.isHideAutoPetMessages()
            && message.contains("autopet");
    }

    private boolean isFullStatusSpam(String message, HorizonConfig config) {
        if (!config.isHideFullStatusMessages()) {
            return false;
        }

        return message.contains("you are already at full health")
            || message.contains("you are already at full mana")
            || message.contains("you already have full health")
            || message.contains("you already have full mana");
    }

    private boolean isEffectSpam(String message, HorizonConfig config) {
        if (!config.isHideEffectMessages()) {
            return false;
        }

        return message.contains("you already have this effect active")
            || message.contains("you already have this potion effect active")
            || message.contains("you are already under the effect")
            || message.contains("this potion effect is already active");
    }

    private boolean isHealingSpam(String message, HorizonConfig config) {
        if (!config.isHideHealingMessages()) {
            return false;
        }

        return message.contains("you were healed for")
            || message.contains("healed you for")
            || message.contains("you healed yourself for")
            || message.contains("you have been healed for");
    }

    private boolean isDungeonEventSpam(String message, HorizonConfig config) {
        if (!config.isHideDungeonEventMessages()) {
            return false;
        }

        return message.contains("the blood door has been opened")
            || message.contains("a wither door has been opened")
            || message.contains("the gate has been opened")
            || message.contains("the watcher has finished summoning mobs");
    }

    private boolean isLockedChestSpam(String message, HorizonConfig config) {
        return config.isHideLockedChestMessages()
            && (message.contains("this chest is locked")
            || message.contains("chest is locked"));
    }
}
