package de.horizon.feature.inventory;

import de.horizon.HorizonMod;
import de.horizon.config.ConfigManager;
import de.horizon.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages inventory button activations and the FARMING_TOOL_REBIND key-remapping.
 *
 * Remapping behaviour:
 *   When the toggle is ON and the player holds a hoe/farming-tool:
 *     - Jump key  -> what Attack was bound to (usually LMB)
 *     - Attack key -> what Jump was bound to (usually Space)
 *   The remap is reverted as soon as the player stops holding a farming tool or
 *   the toggle is turned off.
 */
public final class InventoryButtonService {

    // Substrings present in the SkyBlock item-ID of every Farming Toolkit tool.
    // Covers all tiers and both old (THEORETICAL_HOE_*) and new named variants.
    private static final Set<String> FARMING_TOOL_PATTERNS = new HashSet<>(Arrays.asList(
        "WHEAT_HOE",      "HOE_WHEAT",   // Euclid's Wheat Hoe / old Theoretical
        "CARROT_HOE",     "HOE_CARROT",  // Gauss Carrot Hoe / old
        "POTATO_HOE",     "HOE_POTATO",  // Pythagorean Potato Hoe / old
        "SUGAR_CANE_HOE", "HOE_CANE",    // Turing Sugar Cane Hoe / old
        "NETHER_WART_HOE","HOE_WART",    // Newton Nether Wart Hoe / old
        "PUMPKIN_DICER",                 // Pumpkin Dicer
        "MELON_DICER",                   // Melon Dicer
        "FUNGI_CUTTER",                  // Fungi Cutter
        "CACTUS_KNIFE",                  // Cactus Knife
        "COCOA_CHOPPER",                 // Cocoa Chopper
        "ECLIPSE_HOE",                   // Eclipse Hoe
        "WILD_ROSE_HOE"                  // Wild Rose Hoe
    ));

    private final ConfigManager configManager;

    private InputUtil.Key savedJumpKey = null;
    private InputUtil.Key savedAttackKey = null;
    private boolean rebindApplied = false;

    public InventoryButtonService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public void tick(MinecraftClient mc) {
        if (mc.player == null) return;

        boolean wantFarmingRebind = false;
        for (InventoryButton button : buttons()) {
            if (button.function == InventoryButtonFunction.FARMING_TOOL_REBIND
                    && button.toggle && button.toggleActive) {
                wantFarmingRebind = true;
                break;
            }
        }

        if (wantFarmingRebind) {
            boolean holdsFarmingTool = isFarmingTool(mc.player.getMainHandStack());
            if (holdsFarmingTool && !rebindApplied) {
                applyRebind(true, mc);
            } else if (!holdsFarmingTool && rebindApplied) {
                applyRebind(false, mc);
            }
        } else if (rebindApplied) {
            applyRebind(false, mc);
        }
    }

    // ── Button activation ────────────────────────────────────────────────────

    /** Called when the player clicks a button in the overlay. */
    public void activateButton(InventoryButton button, MinecraftClient mc) {
        if (mc == null || mc.player == null) return;

        if (button.toggle) {
            button.toggleActive = !button.toggleActive;
            configManager.save();
            // For FARMING_TOOL_REBIND the tick() handles the actual key swap.
            return;
        }

        executeFunction(button, mc);
    }

    private void executeFunction(InventoryButton button, MinecraftClient mc) {
        switch (button.function) {
            case COMMAND -> {
                String cmd = button.command == null ? "" : button.command.trim();
                if (!cmd.isEmpty()) {
                    if (cmd.startsWith("/")) cmd = cmd.substring(1);
                    try {
                        mc.player.networkHandler.sendChatCommand(cmd);
                    } catch (Exception e) {
                        HorizonMod.LOGGER.warn("InventoryButton command '{}' failed: {}", cmd, e.getMessage());
                    }
                }
            }
            case FARMING_TOOL_REBIND -> {
                // Direct (non-toggle) usage toggles the rebind once.
                applyRebind(!rebindApplied, mc);
            }
        }
    }

    // ── Key remapping ────────────────────────────────────────────────────────

    private void applyRebind(boolean active, MinecraftClient mc) {
        if (active && !rebindApplied) {
            savedJumpKey = ((KeyBindingAccessor) mc.options.jumpKey).getBoundKey();
            savedAttackKey = ((KeyBindingAccessor) mc.options.attackKey).getBoundKey();
            // Swap: jump gets the attack binding (typically LMB),
            //       attack gets the jump binding (typically Space).
            mc.options.jumpKey.setBoundKey(savedAttackKey);
            mc.options.attackKey.setBoundKey(savedJumpKey);
            KeyBinding.updateKeysByCode();
            rebindApplied = true;
        } else if (!active && rebindApplied) {
            if (savedJumpKey != null)   mc.options.jumpKey.setBoundKey(savedJumpKey);
            if (savedAttackKey != null) mc.options.attackKey.setBoundKey(savedAttackKey);
            KeyBinding.updateKeysByCode();
            rebindApplied = false;
            savedJumpKey   = null;
            savedAttackKey = null;
        }
    }

    /** Called when the player disconnects so bindings are always restored. */
    public void onDisconnect() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && rebindApplied) {
            applyRebind(false, mc);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean isFarmingTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Vanilla hoes as fallback (shouldn't appear in SkyBlock but safe to keep)
        if (stack.getItem() instanceof HoeItem) return true;
        String sbId = getSkyBlockItemId(stack);
        if (sbId == null) return false;
        for (String pattern : FARMING_TOOL_PATTERNS) {
            if (sbId.contains(pattern)) return true;
        }
        return false;
    }

    /** Reads the Hypixel SkyBlock item ID from ExtraAttributes NBT, or null. */
    private static String getSkyBlockItemId(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;
        NbtCompound nbt = customData.copyNbt();
        NbtCompound extra = nbt.getCompoundOrEmpty("ExtraAttributes");
        String id = extra.getString("id", "");
        return id.isEmpty() ? null : id;
    }

    private List<InventoryButton> buttons() {
        return configManager.getConfig().getInventoryButtons();
    }

    public boolean isRebindActive() {
        return rebindApplied;
    }
}
