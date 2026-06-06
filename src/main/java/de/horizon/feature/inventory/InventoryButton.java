package de.horizon.feature.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single configurable button that appears around the player inventory.
 *
 * slotId format: "top_0".."top_8", "bottom_0".."bottom_8",
 *                "left_0".."left_3", "right_0".."right_3"
 */
public class InventoryButton {
    public String id = UUID.randomUUID().toString();
    public String slotId = "";
    public String label = "";
    public InventoryButtonFunction function = InventoryButtonFunction.COMMAND;
    /** Slash-command to execute (without leading '/') when function == COMMAND. */
    public String command = "";
    /** If true the button cycles between an active and inactive state on each click. */
    public boolean toggle = false;
    /** Current toggle state (persisted so it survives restarts). */
    public boolean toggleActive = false;
    /**
     * Item-ID used as the button icon.
     * For a plain Minecraft item: "minecraft:diamond"
     * For a Hypixel-SkyBlock player-head: "HEAD:CONDENSED_FERMENTO"
     */
    public String itemIdActive = "minecraft:lime_stained_glass_pane";
    /**
     * Item-ID used as icon when toggle is off.
     * Only relevant when toggle==true.
     */
    public String itemIdInactive = "minecraft:red_stained_glass_pane";
    /** If true, only show this button on the islands listed in allowedIslands. */
    public boolean islandFilterEnabled = false;
    /** SkyBlockIsland.id() values for which this button should be shown. */
    public List<String> allowedIslands = new ArrayList<>();
    /** If true, FARMING_TOOL_REBIND only activates on the Garden island. */
    public boolean gardenOnly = false;
    /** If true, mouse movement is locked while holding a farming tool on a plot. */
    public boolean squeakyMousemat = false;
}
