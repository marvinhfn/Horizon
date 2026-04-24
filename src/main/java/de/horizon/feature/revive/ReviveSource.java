package de.horizon.feature.revive;

import de.horizon.config.HorizonConfig;

import java.util.List;

public enum ReviveSource {
    SPIRIT_MASK("spirit_mask", "Spirit Mask", List.of(
        "second wind activated! your spirit mask saved your life!"
    )),
    BONZO_MASK("bonzo_mask", "Bonzo Mask", List.of(
        "your bonzo's mask saved your life"
    )),
    PHOENIX_PET("phoenix_pet", "Phoenix Pet", List.of(
        "your phoenix pet saved your life",
        "your phoenix pet saved you from certain death"
    ));

    private final String id;
    private final String displayName;
    private final List<String> chatTriggers;

    ReviveSource(String id, String displayName, List<String> chatTriggers) {
        this.id = id;
        this.displayName = displayName;
        this.chatTriggers = chatTriggers;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> chatTriggers() {
        return chatTriggers;
    }

    public int cooldownSeconds(HorizonConfig config) {
        return switch (this) {
            case SPIRIT_MASK -> 30;
            case BONZO_MASK -> Math.max(180, (int) Math.round(360.0D - (config.getCatacombsLevel() * 3.6D)));
            case PHOENIX_PET -> 60;
        };
    }

    public boolean isEnabled(HorizonConfig config) {
        return switch (this) {
            case SPIRIT_MASK -> config.isSpiritMaskEnabled();
            case BONZO_MASK -> config.isBonzoMaskEnabled();
            case PHOENIX_PET -> config.isPhoenixPetEnabled();
        };
    }

    public String cooldownLabel() {
        return switch (this) {
            case SPIRIT_MASK -> "Spirit Cooldown";
            case BONZO_MASK -> "Bonzo Cooldown";
            case PHOENIX_PET -> "Phoenix Cooldown";
        };
    }

    public String enabledLabel() {
        return switch (this) {
            case SPIRIT_MASK -> "Spirit Tracker";
            case BONZO_MASK -> "Bonzo Tracker";
            case PHOENIX_PET -> "Phoenix Tracker";
        };
    }

    public void toggle(HorizonConfig config) {
        switch (this) {
            case SPIRIT_MASK -> config.setSpiritMaskEnabled(!config.isSpiritMaskEnabled());
            case BONZO_MASK -> config.setBonzoMaskEnabled(!config.isBonzoMaskEnabled());
            case PHOENIX_PET -> config.setPhoenixPetEnabled(!config.isPhoenixPetEnabled());
        }
    }

    public int configuredCooldown(HorizonConfig config) {
        return cooldownSeconds(config);
    }
}
