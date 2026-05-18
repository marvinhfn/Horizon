package de.horizon.feature.chat;

public enum ChatCopyMode {
    OFF("AUS"),
    CTRL_LEFT("STRG+LK"),
    RIGHT("RECHTS"),
    BOTH("BEIDE");

    private final String label;

    ChatCopyMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
