package de.horizon.feature.inventory;

public enum InventoryButtonFunction {
    COMMAND("Command", "Fuehrt einen Slash-Command aus wenn der Button geklickt wird."),
    FARMING_TOOL_REBIND("Farming Tool Rebind",
            "Wenn ein Farming-Tool gehalten wird: Springen -> LMB, Abbauen -> Leertaste.");

    private final String title;
    private final String description;

    InventoryButtonFunction(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
