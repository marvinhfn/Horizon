package de.horizon.feature.chat;

import de.horizon.config.HorizonConfig;

public enum SpamFilterOption {
    BLOCKS_IN_THE_WAY("Blocks in the way", "Teleport- und Portalblockaden ausblenden."),
    ABILITY_MESSAGES("Ability spam", "Wither-Impact- und aehnliche Ability-Messages ausblenden."),
    MANA_MESSAGES("Mana Messages", "Nicht-genug-Mana-Messages ausblenden."),
    COOLDOWN_MESSAGES("Cooldown Messages", "Allgemeine Cooldown-Chatmeldungen ausblenden."),
    BLESSING_MESSAGES("Dungeon Blessings", "Blessing-Chatmeldungen in Dungeons ausblenden."),
    DUNGEON_PICKUPS("Dungeon Pickups", "Keys, Superboom und Revive Stones ausblenden."),
    AUTOPET_MESSAGES("AutoPet", "AutoPet-Swap-Messages ausblenden."),
    FULL_STATUS_MESSAGES("Full HP / Mana", "Meldungen bei vollem Leben oder Mana ausblenden."),
    EFFECT_MESSAGES("Effect Warnings", "Doppelte Effekt- und Potion-Warnungen ausblenden."),
    HEALING_MESSAGES("Heal Messages", "Heilungs-Chatmeldungen ausblenden."),
    DUNGEON_EVENT_MESSAGES("Dungeon Events", "Blood- und Wither-Door-Ansagen ausblenden."),
    LOCKED_CHEST_MESSAGES("Locked Chest", "This chest is locked-Meldungen ausblenden.");

    private final String title;
    private final String description;

    SpamFilterOption(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public boolean isEnabled(HorizonConfig config) {
        return switch (this) {
            case BLOCKS_IN_THE_WAY -> config.isHideBlocksInTheWayMessages();
            case ABILITY_MESSAGES -> config.isHideAbilityMessages();
            case MANA_MESSAGES -> config.isHideManaMessages();
            case COOLDOWN_MESSAGES -> config.isHideCooldownMessages();
            case BLESSING_MESSAGES -> config.isHideBlessingMessages();
            case DUNGEON_PICKUPS -> config.isHideDungeonPickupMessages();
            case AUTOPET_MESSAGES -> config.isHideAutoPetMessages();
            case FULL_STATUS_MESSAGES -> config.isHideFullStatusMessages();
            case EFFECT_MESSAGES -> config.isHideEffectMessages();
            case HEALING_MESSAGES -> config.isHideHealingMessages();
            case DUNGEON_EVENT_MESSAGES -> config.isHideDungeonEventMessages();
            case LOCKED_CHEST_MESSAGES -> config.isHideLockedChestMessages();
        };
    }

    public void toggle(HorizonConfig config) {
        switch (this) {
            case BLOCKS_IN_THE_WAY -> config.setHideBlocksInTheWayMessages(!config.isHideBlocksInTheWayMessages());
            case ABILITY_MESSAGES -> config.setHideAbilityMessages(!config.isHideAbilityMessages());
            case MANA_MESSAGES -> config.setHideManaMessages(!config.isHideManaMessages());
            case COOLDOWN_MESSAGES -> config.setHideCooldownMessages(!config.isHideCooldownMessages());
            case BLESSING_MESSAGES -> config.setHideBlessingMessages(!config.isHideBlessingMessages());
            case DUNGEON_PICKUPS -> config.setHideDungeonPickupMessages(!config.isHideDungeonPickupMessages());
            case AUTOPET_MESSAGES -> config.setHideAutoPetMessages(!config.isHideAutoPetMessages());
            case FULL_STATUS_MESSAGES -> config.setHideFullStatusMessages(!config.isHideFullStatusMessages());
            case EFFECT_MESSAGES -> config.setHideEffectMessages(!config.isHideEffectMessages());
            case HEALING_MESSAGES -> config.setHideHealingMessages(!config.isHideHealingMessages());
            case DUNGEON_EVENT_MESSAGES -> config.setHideDungeonEventMessages(!config.isHideDungeonEventMessages());
            case LOCKED_CHEST_MESSAGES -> config.setHideLockedChestMessages(!config.isHideLockedChestMessages());
        }
    }
}
