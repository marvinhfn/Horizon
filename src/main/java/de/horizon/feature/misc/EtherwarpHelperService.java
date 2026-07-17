package de.horizon.feature.misc;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static java.lang.Math.*;

/**
 * Shows a colored box at the predicted Etherwarp teleport destination.
 *
 * Precomputed blockFlags array with PASSABLE and BLOCKS_FEET flags per block state.
 * Collision shape height for clearance checking.
 * Proper voxel traversal (DDA algorithm).
 */
public final class EtherwarpHelperService {
    private static final int MAX_RANGE = 60;
    private static final int COLOR_VALID   = 0x80D9AA00; // gold-ish
    private static final int COLOR_INVALID = 0x88FF4444;

    private static final int PASSABLE    = 1; // ray passes through
    private static final int BLOCKS_FEET = 2; // cannot stand inside even if passable

    private static int[] blockFlags;

    private BlockPos cachedDest = null;
    private boolean cachedValid = false;
    private boolean lastUseKeyDown = false;

    private static void initBlockFlags() {
        if (blockFlags != null) return;
        blockFlags = new int[Block.BLOCK_STATE_REGISTRY.size()];
        Block.BLOCK_STATE_REGISTRY.forEach(state -> {
            Block block = state.getBlock();
            int id = Block.getId(state);
            if (id < 0 || id >= blockFlags.length) return;

            boolean passable = isBlockPassable(block);
            boolean feet = isBlocksFeet(block);

            int flags = 0;
            if (passable) flags |= PASSABLE;
            if (feet) flags |= BLOCKS_FEET;
            blockFlags[id] = flags;
        });
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean isBlockPassable(Block block) {
        if (block instanceof AirBlock) return true;
        if (block instanceof FlowerBlock || block instanceof TallGrassBlock || block instanceof BushBlock
            || block instanceof TallFlowerBlock) return true;
        if (block instanceof TorchBlock || block instanceof RedstoneTorchBlock) return true;
        if (block instanceof TripWireBlock || block instanceof TripWireHookBlock) return true;
        if (block instanceof RailBlock) return true;
        if (block instanceof FireBlock) return true;
        if (block instanceof VineBlock) return true;
        if (block instanceof LiquidBlock) return true;
        if (block instanceof SaplingBlock) return true;
        if (block instanceof CropBlock || block instanceof StemBlock) return true;
        if (block instanceof SeagrassBlock || block instanceof TallSeagrassBlock) return true;
        if (block instanceof SugarCaneBlock) return true;
        if (block instanceof MushroomBlock) return true;
        if (block instanceof NetherWartBlock) return true;
        if (block instanceof RedStoneWireBlock || block instanceof ComparatorBlock || block instanceof RepeaterBlock) return true;
        if (block instanceof DoublePlantBlock) return true;
        if (block instanceof LeverBlock) return true;
        if (block instanceof SnowLayerBlock) return true;
        if (block instanceof BubbleColumnBlock) return true;
        if (block instanceof GrowingPlantBlock) return true;
        if (block instanceof PistonHeadBlock) return true;
        if (block instanceof ButtonBlock) return true;
        if (block instanceof LanternBlock) return true;
        if (block instanceof SkullBlock || block instanceof WallSkullBlock) return true;
        if (block instanceof LadderBlock) return true;
        if (block instanceof FlowerPotBlock) return true;
        if (block instanceof WebBlock) return true;
        if (block instanceof NetherPortalBlock) return true;
        return false;
    }

    private static boolean isBlocksFeet(Block block) {
        if (block instanceof SkullBlock || block instanceof WallSkullBlock) return true;
        if (block instanceof FlowerPotBlock) return true;
        if (block instanceof LadderBlock) return true;
        if (block instanceof VineBlock) return true;
        return false;
    }

    public void tick(HorizonConfig config) {
        if (!config.isEtherwarpEnabled()) {
            cachedDest = null;
            lastUseKeyDown = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            cachedDest = null;
            lastUseKeyDown = false;
            return;
        }
        if (!isHoldingEtherwarpItem(mc)) {
            cachedDest = null;
            lastUseKeyDown = false;
            return;
        }

        initBlockFlags();
        EtherResult result = calculateDest(mc);
        cachedDest = result.pos;
        cachedValid = result.succeeded;

        // Play sound when player presses use-key with a valid destination
        boolean useKeyNow = mc.options.keyUse.isDown();
        if (useKeyNow && !lastUseKeyDown && cachedValid && config.isEtherwarpSoundEnabled()) {
            SoundEvent sound = soundForIndex(config.getEtherwarpSoundIndex());
            mc.player.playSound(sound, config.getEtherwarpSoundVolume(), config.getEtherwarpSoundPitch());
        }
        lastUseKeyDown = useKeyNow;
    }

    public void renderWorld(LevelRenderContext context, HorizonConfig config) {
        if (!config.isEtherwarpEnabled() || cachedDest == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (config.isEtherwarpSneakOnly() && !mc.player.isCrouching()) return;

        int color = cachedValid ? COLOR_VALID : COLOR_INVALID;
        AABB box = new AABB(cachedDest).inflate(0.002);
        int style = config.getEtherwarpRenderStyle();
        DungeonRenderUtil.drawBox(context, box, color, style, true);
    }

    private static SoundEvent soundForIndex(int index) {
        return index == 1 ? SoundEvents.CHORUS_FRUIT_TELEPORT : SoundEvents.ENDER_DRAGON_HURT;
    }

    private static boolean isHoldingEtherwarpItem(Minecraft mc) {
        var stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) return false;
        String name = stack.getHoverName().getString().toLowerCase();
        return name.contains("aspect of the void") || name.contains("aspect of the dragons");
    }

    private record EtherResult(boolean succeeded, BlockPos pos) {}

    private static EtherResult calculateDest(Minecraft mc) {
        Vec3 pos = mc.player.position();
        double eyeHeight = mc.player.isCrouching() ? 1.27 : 1.62;
        Vec3 start = pos.add(0, eyeHeight, 0);
        Vec3 look = mc.player.getViewVector(1.0f);
        Vec3 end = start.add(look.scale(MAX_RANGE));

        return traverseVoxels(mc.level, start, end);
    }

    /**
     * Voxel traversal (DDA) from start to end. Returns the first solid block hit
     * with clearance validation.
     */
    private static EtherResult traverseVoxels(Level level, Vec3 start, Vec3 end) {
        double x0 = start.x, y0 = start.y, z0 = start.z;
        double x1 = end.x, y1 = end.y, z1 = end.z;

        int x = (int) floor(x0), y = (int) floor(y0), z = (int) floor(z0);
        int endX = (int) floor(x1), endY = (int) floor(y1), endZ = (int) floor(z1);

        double dirX = x1 - x0, dirY = y1 - y0, dirZ = z1 - z0;

        int stepX = dirX > 0 ? 1 : (dirX < 0 ? -1 : 0);
        int stepY = dirY > 0 ? 1 : (dirY < 0 ? -1 : 0);
        int stepZ = dirZ > 0 ? 1 : (dirZ < 0 ? -1 : 0);

        double invDirX = dirX != 0 ? 1.0 / dirX : Double.MAX_VALUE;
        double invDirY = dirY != 0 ? 1.0 / dirY : Double.MAX_VALUE;
        double invDirZ = dirZ != 0 ? 1.0 / dirZ : Double.MAX_VALUE;

        double tDeltaX = abs(invDirX * stepX);
        double tDeltaY = abs(invDirY * stepY);
        double tDeltaZ = abs(invDirZ * stepZ);

        double tMaxX = abs((x + max(stepX, 0) - x0) * invDirX);
        double tMaxY = abs((y + max(stepY, 0) - y0) * invDirY);
        double tMaxZ = abs((z + max(stepZ, 0) - z0) * invDirZ);

        for (int i = 0; i < MAX_RANGE * 3; i++) {
            BlockPos blockPos = new BlockPos(x, y, z);
            LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
            BlockState state = chunk.getBlockState(blockPos);
            int id = Block.getId(state);

            if (id >= 0 && id < blockFlags.length && (blockFlags[id] & PASSABLE) == 0) {
                // Hit a solid block — check clearance for etherwarp landing
                double collisionTop = state.getCollisionShape(level, blockPos).max(Direction.Axis.Y);
                int clearanceBaseY = blockPos.getY() + max(1, (int) ceil(collisionTop));

                BlockState feetState = chunk.getBlockState(new BlockPos(x, clearanceBaseY, z));
                int feetId = Block.getId(feetState);
                if (feetId >= 0 && feetId < blockFlags.length) {
                    int feetFlags = blockFlags[feetId];
                    if ((feetFlags & PASSABLE) == 0 || (feetFlags & BLOCKS_FEET) != 0) {
                        return new EtherResult(false, blockPos);
                    }
                }

                BlockState headState = chunk.getBlockState(new BlockPos(x, clearanceBaseY + 1, z));
                int headId = Block.getId(headState);
                if (headId >= 0 && headId < blockFlags.length) {
                    int headFlags = blockFlags[headId];
                    if ((headFlags & PASSABLE) == 0 || (headFlags & BLOCKS_FEET) != 0) {
                        return new EtherResult(false, blockPos);
                    }
                }

                return new EtherResult(true, blockPos);
            }

            if (x == endX && y == endY && z == endZ) break;

            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                tMaxX += tDeltaX;
                x += stepX;
            } else if (tMaxY <= tMaxZ) {
                tMaxY += tDeltaY;
                y += stepY;
            } else {
                tMaxZ += tDeltaZ;
                z += stepZ;
            }
        }

        return new EtherResult(false, null);
    }
}
