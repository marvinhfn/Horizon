package de.horizon.feature.chat;

import de.horizon.Lang;
import de.horizon.config.HorizonConfig;

public enum SpamFilterOption {
    // ── Dungeon ───────────────────────────────────────────────────────────────
    BLESSING_MESSAGES(Category.DUNGEON, "Dungeon Blessings",
        "Blessing-Chatmeldungen in Dungeons ausblenden.",
        "Hide blessing chat messages in dungeons."),
    DUNGEON_PICKUPS(Category.DUNGEON, "Dungeon Pickups",
        "Keys, Superboom und Revive Stones ausblenden.",
        "Hide keys, superboom and revive stones."),
    DUNGEON_EVENT_MESSAGES(Category.DUNGEON, "Dungeon Events",
        "Blood- und Wither-Door-Ansagen ausblenden.",
        "Hide Blood and Wither Door announcements."),
    LOCKED_CHEST_MESSAGES(Category.DUNGEON, "Locked Chest",
        "This chest is locked-Meldungen ausblenden.",
        "Hide locked chest messages."),
    BOSS_MESSAGES(Category.DUNGEON, "Boss Messages",
        "Boss-Nachrichten in Dungeons ausblenden.",
        "Hide boss chat messages in dungeons."),

    // ── General ───────────────────────────────────────────────────────────────
    BLOCKS_IN_THE_WAY(Category.GENERAL, "Blocks in the way",
        "Teleport- und Portalblockaden ausblenden.",
        "Hide teleport and portal blockage messages."),
    ABILITY_MESSAGES(Category.GENERAL, "Ability spam",
        "Wither-Impact- und aehnliche Ability-Messages ausblenden.",
        "Hide Wither Impact and similar ability messages."),
    MANA_MESSAGES(Category.GENERAL, "Mana Messages",
        "Nicht-genug-Mana-Messages ausblenden.",
        "Hide not-enough-mana messages."),
    COOLDOWN_MESSAGES(Category.GENERAL, "Cooldown Messages",
        "Allgemeine Cooldown-Chatmeldungen ausblenden.",
        "Hide general cooldown chat messages."),
    AUTOPET_MESSAGES(Category.GENERAL, "AutoPet",
        "AutoPet-Swap-Messages ausblenden.",
        "Hide AutoPet swap messages."),
    FULL_STATUS_MESSAGES(Category.GENERAL, "Full HP / Mana",
        "Meldungen bei vollem Leben oder Mana ausblenden.",
        "Hide messages at full HP or mana."),
    EFFECT_MESSAGES(Category.GENERAL, "Effect Warnings",
        "Doppelte Effekt- und Potion-Warnungen ausblenden.",
        "Hide duplicate effect and potion warnings."),
    HEALING_MESSAGES(Category.GENERAL, "Heal Messages",
        "Heilungs-Chatmeldungen ausblenden.",
        "Hide healing chat messages."),
    WARPING_MESSAGES(Category.GENERAL, "Warping",
        "Warping...-Meldungen ausblenden.",
        "Hide warping messages."),
    SENDING_TO_SERVER_MESSAGES(Category.GENERAL, "Sending to Server",
        "Sending to Server-Meldungen ausblenden.",
        "Hide sending to server messages."),
    PROFILE_MESSAGES(Category.GENERAL, "Profile / Profile ID",
        "Profil-ID und Profil-Nachrichten ausblenden.",
        "Hide profile ID and profile messages."),
    GUILD_JOIN_LEAVE_MESSAGES(Category.GENERAL, "Guild Join / Leave",
        "Beitritts- und Austritts-Nachrichten der Gilde ausblenden.",
        "Hide guild member join and leave messages."),
    FIRESALE_MESSAGES(Category.GENERAL, "Firesales",
        "Firesale-Ankuendigungen ausblenden.",
        "Hide fire sale announcements."),
    RADIO_SIGNAL_MESSAGES(Category.GENERAL, "Radio Signal",
        "Radio-Signal-Nachrichten ausblenden.",
        "Hide radio signal messages."),
    SACKS_MESSAGES(Category.GENERAL, "Sacks",
        "[Sacks]-Chatmeldungen ausblenden.",
        "Hide [Sacks] chat messages.");

    public enum Category {
        DUNGEON, GENERAL;

        public String label() {
            return switch (this) {
                case DUNGEON -> "Dungeon";
                case GENERAL -> Lang.t("Allgemein", "General");
            };
        }
    }

    private final Category category;
    private final String title;
    private final String descriptionDe;
    private final String descriptionEn;

    SpamFilterOption(Category category, String title, String descriptionDe, String descriptionEn) {
        this.category = category;
        this.title = title;
        this.descriptionDe = descriptionDe;
        this.descriptionEn = descriptionEn;
    }

    public Category category() {
        return category;
    }

    public String title() {
        return title;
    }

    public String description() {
        return Lang.t(descriptionDe, descriptionEn);
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
            case BOSS_MESSAGES -> config.isHideBossMessages();
            case WARPING_MESSAGES -> config.isHideWarpingMessages();
            case SENDING_TO_SERVER_MESSAGES -> config.isHideSendingToServerMessages();
            case PROFILE_MESSAGES -> config.isHideProfileMessages();
            case GUILD_JOIN_LEAVE_MESSAGES -> config.isHideGuildJoinLeaveMessages();
            case FIRESALE_MESSAGES -> config.isHideFiresaleMessages();
            case RADIO_SIGNAL_MESSAGES -> config.isHideRadioSignalMessages();
            case SACKS_MESSAGES -> config.isHideSacksMessages();
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
            case BOSS_MESSAGES -> config.setHideBossMessages(!config.isHideBossMessages());
            case WARPING_MESSAGES -> config.setHideWarpingMessages(!config.isHideWarpingMessages());
            case SENDING_TO_SERVER_MESSAGES -> config.setHideSendingToServerMessages(!config.isHideSendingToServerMessages());
            case PROFILE_MESSAGES -> config.setHideProfileMessages(!config.isHideProfileMessages());
            case GUILD_JOIN_LEAVE_MESSAGES -> config.setHideGuildJoinLeaveMessages(!config.isHideGuildJoinLeaveMessages());
            case FIRESALE_MESSAGES -> config.setHideFiresaleMessages(!config.isHideFiresaleMessages());
            case RADIO_SIGNAL_MESSAGES -> config.setHideRadioSignalMessages(!config.isHideRadioSignalMessages());
            case SACKS_MESSAGES -> config.setHideSacksMessages(!config.isHideSacksMessages());
        }
    }
}
