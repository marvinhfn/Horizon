package de.horizon.feature.chat;

public enum ChatTab {
    ALL("A"),
    PARTY("P"),
    GUILD("G"),
    DM("D");

    private final String key;

    ChatTab(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
