package de.horizon.feature.dungeon.room;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Maps modern block registry names to legacy numeric block IDs.
 * Used for dungeon room hash computation to match the community room database.
 */
public final class LegacyBlockRegistry {
    private LegacyBlockRegistry() {}

    private static final Map<String, Integer> BLOCKS = Map.ofEntries(
        Map.entry("minecraft:air", 0),
        Map.entry("minecraft:stone", 1),
        Map.entry("minecraft:polished_andesite", 1),
        Map.entry("minecraft:polished_diorite", 1),
        Map.entry("minecraft:polished_granite", 1),
        Map.entry("minecraft:andesite", 1),
        Map.entry("minecraft:diorite", 1),
        Map.entry("minecraft:granite", 1),
        Map.entry("minecraft:grass_block", 2),
        Map.entry("minecraft:coarse_dirt", 3),
        Map.entry("minecraft:dirt", 3),
        Map.entry("minecraft:cobblestone", 4),
        Map.entry("minecraft:dark_oak_planks", 5),
        Map.entry("minecraft:spruce_planks", 5),
        Map.entry("minecraft:jungle_planks", 5),
        Map.entry("minecraft:birch_planks", 5),
        Map.entry("minecraft:bedrock", 7),
        Map.entry("minecraft:lava", 11),
        Map.entry("minecraft:gravel", 13),
        Map.entry("minecraft:gold_ore", 14),
        Map.entry("minecraft:oak_wood", 17),
        Map.entry("minecraft:oak_log", 17),
        Map.entry("minecraft:oak_leaves", 18),
        Map.entry("minecraft:lapis_block", 22),
        Map.entry("minecraft:sandstone", 24),
        Map.entry("minecraft:sticky_piston", 29),
        Map.entry("minecraft:cobweb", 30),
        Map.entry("minecraft:piston", 33),
        Map.entry("minecraft:piston_head", 34),
        Map.entry("minecraft:gray_wool", 35),
        Map.entry("minecraft:red_wool", 35),
        Map.entry("minecraft:black_wool", 35),
        Map.entry("minecraft:light_gray_wool", 35),
        Map.entry("minecraft:green_wool", 35),
        Map.entry("minecraft:orange_wool", 35),
        Map.entry("minecraft:brown_mushroom", 39),
        Map.entry("minecraft:gold_block", 41),
        Map.entry("minecraft:iron_block", 42),
        Map.entry("minecraft:smooth_stone", 43),
        Map.entry("minecraft:smooth_sandstone", 43),
        Map.entry("minecraft:bookshelf", 47),
        Map.entry("minecraft:mossy_cobblestone", 48),
        Map.entry("minecraft:obsidian", 49),
        Map.entry("minecraft:wall_torch", 50),
        Map.entry("minecraft:torch", 50),
        Map.entry("minecraft:fire", 51),
        Map.entry("minecraft:chest", 54),
        Map.entry("minecraft:diamond_block", 57),
        Map.entry("minecraft:ladder", 65),
        Map.entry("minecraft:rail", 66),
        Map.entry("minecraft:cobblestone_stairs", 67),
        Map.entry("minecraft:lever", 69),
        Map.entry("minecraft:stone_button", 77),
        Map.entry("minecraft:ice", 79),
        Map.entry("minecraft:oak_fence", 85),
        Map.entry("minecraft:glowstone", 89),
        Map.entry("minecraft:black_stained_glass", 95),
        Map.entry("minecraft:light_gray_stained_glass", 95),
        Map.entry("minecraft:infested_cobblestone", 97),
        Map.entry("minecraft:stone_bricks", 98),
        Map.entry("minecraft:mossy_stone_bricks", 98),
        Map.entry("minecraft:cracked_stone_bricks", 98),
        Map.entry("minecraft:chiseled_stone_bricks", 98),
        Map.entry("minecraft:brown_mushroom_block", 99),
        Map.entry("minecraft:iron_bars", 101),
        Map.entry("minecraft:vine", 106),
        Map.entry("minecraft:stone_brick_stairs", 109),
        Map.entry("minecraft:nether_bricks", 112),
        Map.entry("minecraft:cauldron", 118),
        Map.entry("minecraft:end_portal_frame", 120),
        Map.entry("minecraft:end_stone", 121),
        Map.entry("minecraft:emerald_block", 133),
        Map.entry("minecraft:spruce_stairs", 134),
        Map.entry("minecraft:cobblestone_wall", 139),
        Map.entry("minecraft:redstone_block", 152),
        Map.entry("minecraft:hopper", 154),
        Map.entry("minecraft:quartz_block", 155),
        Map.entry("minecraft:quartz_stairs", 156),
        Map.entry("minecraft:cyan_terracotta", 159),
        Map.entry("minecraft:blue_terracotta", 159),
        Map.entry("minecraft:light_blue_terracotta", 159),
        Map.entry("minecraft:gray_terracotta", 159),
        Map.entry("minecraft:light_gray_terracotta", 159),
        Map.entry("minecraft:lime_terracotta", 159),
        Map.entry("minecraft:green_terracotta", 159),
        Map.entry("minecraft:black_terracotta", 159),
        Map.entry("minecraft:magenta_terracotta", 159),
        Map.entry("minecraft:purple_terracotta", 159),
        Map.entry("minecraft:red_terracotta", 159),
        Map.entry("minecraft:white_terracotta", 159),
        Map.entry("minecraft:orange_terracotta", 159),
        Map.entry("minecraft:yellow_terracotta", 159),
        Map.entry("minecraft:pink_terracotta", 159),
        Map.entry("minecraft:brown_terracotta", 159),
        Map.entry("minecraft:magenta_stained_glass_pane", 160),
        Map.entry("minecraft:dark_oak_stairs", 164),
        Map.entry("minecraft:slime_block", 165),
        Map.entry("minecraft:barrier", 166),
        Map.entry("minecraft:dark_prismarine", 168),
        Map.entry("minecraft:prismarine", 168),
        Map.entry("minecraft:prismarine_bricks", 168),
        Map.entry("minecraft:sea_lantern", 169),
        Map.entry("minecraft:green_carpet", 171),
        Map.entry("minecraft:gray_carpet", 171),
        Map.entry("minecraft:light_gray_carpet", 171),
        Map.entry("minecraft:red_carpet", 171),
        Map.entry("minecraft:brown_carpet", 171),
        Map.entry("minecraft:magenta_carpet", 171),
        Map.entry("minecraft:blue_carpet", 171),
        Map.entry("minecraft:light_blue_carpet", 171),
        Map.entry("minecraft:white_carpet", 171),
        Map.entry("minecraft:orange_carpet", 171),
        Map.entry("minecraft:yellow_carpet", 171),
        Map.entry("minecraft:lime_carpet", 171),
        Map.entry("minecraft:pink_carpet", 171),
        Map.entry("minecraft:cyan_carpet", 171),
        Map.entry("minecraft:purple_carpet", 171),
        Map.entry("minecraft:black_carpet", 171),
        Map.entry("minecraft:coal_block", 173),
        Map.entry("minecraft:packed_ice", 174),
        Map.entry("minecraft:sunflower", 175),
        Map.entry("minecraft:spruce_fence", 188)
    );

    // Slab variants with type suffix
    private static final Map<String, Integer> SLAB_DOUBLES = Map.ofEntries(
        Map.entry("minecraft:cobblestone_slab", 43),
        Map.entry("minecraft:stone_brick_slab", 43),
        Map.entry("minecraft:smooth_stone_slab", 43),
        Map.entry("minecraft:sandstone_slab", 43)
    );
    private static final Map<String, Integer> SLAB_HALVES = Map.ofEntries(
        Map.entry("minecraft:stone_brick_slab", 44),
        Map.entry("minecraft:cobblestone_slab", 44),
        Map.entry("minecraft:smooth_stone_slab", 44),
        Map.entry("minecraft:sandstone_slab", 44)
    );
    private static final Map<String, Integer> WOOD_SLAB_DOUBLES = Map.ofEntries(
        Map.entry("minecraft:spruce_slab", 125),
        Map.entry("minecraft:oak_slab", 125),
        Map.entry("minecraft:dark_oak_slab", 125)
    );
    private static final Map<String, Integer> WOOD_SLAB_HALVES = Map.ofEntries(
        Map.entry("minecraft:spruce_slab", 126),
        Map.entry("minecraft:oak_slab", 126),
        Map.entry("minecraft:dark_oak_slab", 126)
    );
    private static final Map<String, Integer> RED_SANDSTONE_SLAB = Map.of(
        "minecraft:red_sandstone_slab", 182
    );

    /**
     * Gets the legacy block ID for the given block state.
     * Returns null if the block is not in the registry (unknown block).
     */
    public static Integer getLegacyId(BlockState state) {
        var block = state.getBlock();

        // Handle fluids first
        var fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            if (fluidState.is(FluidTags.WATER))
                return fluidState.isSource() ? 9 : 8;
            if (fluidState.is(FluidTags.LAVA))
                return fluidState.isSource() ? 11 : 10;
        }

        String registryName = BuiltInRegistries.BLOCK.getKey(block).toString();

        // Handle slabs with type property
        if (block instanceof SlabBlock) {
            String typeStr = state.getValue(SlabBlock.TYPE).name().toLowerCase();
            String keyWithType = registryName + "[type=" + typeStr + "]";

            if (typeStr.equals("double")) {
                Integer id = SLAB_DOUBLES.get(registryName);
                if (id != null) return id;
                id = WOOD_SLAB_DOUBLES.get(registryName);
                if (id != null) return id;
                if (registryName.equals("minecraft:red_sandstone_slab")) return 181;
            } else {
                Integer id = SLAB_HALVES.get(registryName);
                if (id != null) return id;
                id = WOOD_SLAB_HALVES.get(registryName);
                if (id != null) return id;
                if (registryName.equals("minecraft:red_sandstone_slab")) return 182;
            }
        }

        return BLOCKS.get(registryName);
    }
}
