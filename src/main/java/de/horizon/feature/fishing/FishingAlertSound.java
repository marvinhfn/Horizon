package de.horizon.feature.fishing;

public enum FishingAlertSound {
    RARE("Rare Sound"),
    MEOW("Meow"),
    CUSTOM("Boo Womp"),
    MR("Mr.");

    private final String label;

    FishingAlertSound(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
