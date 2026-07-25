package de.horizon.feature.dungeon.terminal;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Name normalisation tables for the item-name and coloured-items terminals.
 * Hypixel uses legacy item names ("Oak Wood Planks") that differ from the modern
 * display names ("Oak Planks"); the terminal titles reference the legacy names.
 */
public final class TerminalNames {

    private TerminalNames() {}

    // Modern display name -> legacy Hypixel name (used by the "Item Name" terminal).
    private static final Map<String, String> LEGACY_NAMES = Map.ofEntries(
        Map.entry("Grass", "Grass Block"),
        Map.entry("Redstone Dust", "Redstone"),
        Map.entry("Empty Map", "Map"),
        Map.entry("Oak Planks", "Oak Wood Planks"),
        Map.entry("Spruce Planks", "Spruce Wood Planks"),
        Map.entry("Birch Planks", "Birch Wood Planks"),
        Map.entry("Jungle Planks", "Jungle Wood Planks"),
        Map.entry("Acacia Planks", "Acacia Wood Planks"),
        Map.entry("Dark Oak Planks", "Dark Oak Wood Planks"),
        Map.entry("Tall Grass", "Double Tallgrass"),
        Map.entry("Brown Mushroom", "Mushroom"),
        Map.entry("Red Mushroom", "Mushroom"),
        Map.entry("Brick Slab", "Bricks Slab"),
        Map.entry("Stone Brick Slab", "Stone Bricks Slab"),
        Map.entry("Oak Slab", "Oak Wood Slab"),
        Map.entry("Spruce Slab", "Spruce Wood Slab"),
        Map.entry("Birch Slab", "Birch Wood Slab"),
        Map.entry("Jungle Slab", "Jungle Wood Slab"),
        Map.entry("Acacia Slab", "Acacia Wood Slab"),
        Map.entry("Dark Oak Slab", "Dark Oak Wood Slab"),
        Map.entry("Mossy Cobblestone", "Mossy Stone"),
        Map.entry("Oak Stairs", "Oak Wood Stairs"),
        Map.entry("Spruce Stairs", "Spruce Wood Stairs"),
        Map.entry("Birch Stairs", "Birch Wood Stairs"),
        Map.entry("Jungle Stairs", "Jungle Wood Stairs"),
        Map.entry("Acacia Stairs", "Acacia Wood Stairs"),
        Map.entry("Dark Oak Stairs", "Dark Oak Wood Stairs"),
        Map.entry("Oak Pressure Plate", "Wooden Pressure Plate"),
        Map.entry("Light Weighted Pressure Plate", "Weighted Pressure Plate (Light)"),
        Map.entry("Heavy Weighted Pressure Plate", "Weighted Pressure Plate (Heavy)"),
        Map.entry("Oak Button", "Button"),
        Map.entry("Stone Button", "Button"),
        Map.entry("White Carpet", "Carpet"),
        Map.entry("Black Terracotta", "Black Stained Clay"),
        Map.entry("Red Terracotta", "Red Stained Clay"),
        Map.entry("Green Terracotta", "Green Stained Clay"),
        Map.entry("Brown Terracotta", "Brown Stained Clay"),
        Map.entry("Blue Terracotta", "Blue Stained Clay"),
        Map.entry("Purple Terracotta", "Purple Stained Clay"),
        Map.entry("Cyan Terracotta", "Cyan Stained Clay"),
        Map.entry("Light Gray Terracotta", "Light Gray Stained Clay"),
        Map.entry("Gray Terracotta", "Gray Stained Clay"),
        Map.entry("Pink Terracotta", "Pink Stained Clay"),
        Map.entry("Lime Terracotta", "Lime Stained Clay"),
        Map.entry("Yellow Terracotta", "Yellow Stained Clay"),
        Map.entry("Light Blue Terracotta", "Light Blue Stained Clay"),
        Map.entry("Magenta Terracotta", "Magenta Stained Clay"),
        Map.entry("Orange Terracotta", "Orange Stained Clay"),
        Map.entry("White Terracotta", "White Stained Clay"),
        Map.entry("Terracotta", "Hardened Clay"),
        Map.entry("Nether Portal", "Portal"),
        Map.entry("White Wool", "Wool"),
        Map.entry("Block of Lapis Lazuli", "Lapis Lazuli Block"),
        Map.entry("Red Bed", "Bed"),
        Map.entry("White Bed", "Bed"),
        Map.entry("Oak Trapdoor", "Wooden Trapdoor"),
        Map.entry("Infested Stone", "Stone Monster Egg"),
        Map.entry("Infested Cobblestone", "Cobblestone Monster Egg"),
        Map.entry("Infested Stone Bricks", "Stone Brick Monster Egg"),
        Map.entry("Infested Mossy Stone Bricks", "Mossy Stone Brick Monster Egg"),
        Map.entry("Infested Cracked Stone Bricks", "Cracked Stone Brick Monster Egg"),
        Map.entry("Infested Chiseled Stone Bricks", "Chiseled Stone Brick Monster Egg"),
        Map.entry("Enchanting Table", "Enchantment Table"),
        Map.entry("Chipped Anvil", "Slightly Damaged Anvil"),
        Map.entry("Damaged Anvil", "Very Damaged Anvil"),
        Map.entry("Daylight Detector", "Daylight Sensor"),
        Map.entry("Quartz Pillar", "Pillar Quartz Block"),
        Map.entry("Wheat Seeds", "Seeds"),
        Map.entry("Chainmail Helmet", "Chain Helmet"),
        Map.entry("Chainmail Chestplate", "Chain Chestplate"),
        Map.entry("Chainmail Leggings", "Chain Leggings"),
        Map.entry("Chainmail Boots", "Chain Boots"),
        Map.entry("Oak Boat", "Boat"),
        Map.entry("Milk Bucket", "Milk"),
        Map.entry("Sugar Cane", "Sugar Canes"),
        Map.entry("Raw Cod", "Raw Fish"),
        Map.entry("Tropical Fish", "Clownfish"),
        Map.entry("Cooked Cod", "Cooked Fish"),
        Map.entry("Red Dye", "Rose Red"),
        Map.entry("Green Dye", "Cactus Green"),
        Map.entry("Yellow Dye", "Dandelion Yellow"),
        Map.entry("Glistering Melon Slice", "Glistering Melon"),
        Map.entry("Player Head", "Head"),
        Map.entry("Golden Horse Armor", "Gold Horse Armor")
    );

    // Colour-word normalisation for the "Coloured Items" terminal.
    private static final Map<String, String> COLOR_FIXES = Map.ofEntries(
        Map.entry("light gray", "silver"),
        Map.entry("wool", "white"),
        Map.entry("bone", "white"),
        Map.entry("ink", "black"),
        Map.entry("lapis", "blue"),
        Map.entry("cocoa", "brown"),
        Map.entry("dandelion", "yellow"),
        Map.entry("rose", "red"),
        Map.entry("cactus", "green")
    );

    /** Reads the display name of a stack, preferring an explicit custom name. */
    public static String displayName(ItemStack stack) {
        var customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) return customName.getString();
        return stack.getItemName().getString();
    }

    /** Maps a modern display name to its legacy Hypixel equivalent (or returns it unchanged). */
    public static String legacyName(String modern) {
        return LEGACY_NAMES.getOrDefault(modern, modern);
    }

    /** Rewrites a colour prefix to its Hypixel-canonical colour word if applicable. */
    public static String fixColorName(String name) {
        for (Map.Entry<String, String> fix : COLOR_FIXES.entrySet()) {
            if (name.regionMatches(true, 0, fix.getKey(), 0, fix.getKey().length())) {
                return fix.getValue();
            }
        }
        return name;
    }

    /** True when a stack already carries a completion glint (already clicked). */
    public static boolean hasGlint(ItemStack stack) {
        Boolean glint = stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        return glint != null && glint;
    }
}
