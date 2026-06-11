package de.horizon.screen;

import de.horizon.HorizonClient;
import de.horizon.feature.inventory.InventoryButton;
import de.horizon.feature.inventory.InventoryButtonFunction;
import de.horizon.feature.inventory.InventoryButtonItems;
import de.horizon.hud.HudStyle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout editor that shows a simplified inventory representation with
 * grey "+" placeholder boxes for each of the 26 configurable button slots.
 *
 * Clicking a slot opens an inline popup to configure or delete the button.
 * Changes are saved immediately to the ConfigManager.
 */
public final class InventoryButtonLayoutScreen extends Screen {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int BG            = 0xD0101820;
    private static final int INV_BG        = 0xFF2D3748;
    private static final int INV_SLOT      = 0xFF3D4A5C;
    private static final int BTN_EMPTY     = 0x88444F60;
    private static final int BTN_BORDER    = 0xFFAAAACC;
    private static final int BTN_HOVER     = 0xA055AAFF;
    private static final int POPUP_BG      = 0xF0151C26;
    private static final int POPUP_BORDER  = 0xFF4A6080;
    private static final int BUTTON_FILL   = 0xFF2E4060;
    private static final int BUTTON_ACTIVE = 0xFF2DBA68;
    private static final int BUTTON_DEL    = 0xFF8A3A3A;
    private static final int TEXT_COLOR    = 0xFFFFFFFF;
    private static final int MUTED         = 0xFFB8B8B8;
    private static final int ACCENT_TEXT   = 0xFF75E7CA;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int BTN_SIZE   = 18; // slot size in px
    private static final int INV_W      = 176;
    private static final int INV_H      = 166;
    private static final int GAP        = 4;  // gap between inv edge and buttons

    private final Screen parent;
    private final HorizonClient horizonClient;

    // All 26 slot definitions (built once in init)
    private final List<SlotDef> slots = new ArrayList<>();

    // ── Editor popup state ───────────────────────────────────────────────────
    private boolean popupOpen = false;
    private String  editingSlotId = null;

    // Fields for the popup form
    private String  editLabel       = "";
    private InventoryButtonFunction editFunction = InventoryButtonFunction.COMMAND;
    private String  editCommand     = "";
    private boolean editToggle      = false;
    private String  editItemActive  = "minecraft:lime_stained_glass_pane";
    private String  editItemInactive = "minecraft:red_stained_glass_pane";
    private boolean existingButton  = false;

    // Island filter state
    private boolean editIslandFilterEnabled = false;
    private final java.util.LinkedHashSet<String> editAllowedIslands = new java.util.LinkedHashSet<>();
    private String islandSearch = "";
    // Garden-only state (only for FARMING_TOOL_REBIND)
    private boolean editGardenOnly = false;
    private boolean editSqueakyMousemat = false;

    // Which text field in the popup has focus
    private enum PopupFocus { NONE, LABEL, COMMAND, ISLAND_SEARCH }
    private PopupFocus popupFocus = PopupFocus.NONE;

    // ── Constructor ───────────────────────────────────────────────────────────

    public InventoryButtonLayoutScreen(Screen parent, HorizonClient horizonClient) {
        super(Component.literal("Inventory Buttons - Layout"));
        this.parent        = parent;
        this.horizonClient = horizonClient;
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        slots.clear();
        int cx = width  / 2;
        int cy = height / 2;
        int invX = cx - INV_W / 2;
        int invY = cy - INV_H / 2;

        for (int i = 0; i < 9; i++) {
            slots.add(new SlotDef("top_"    + i,
                    invX + 8 + i * BTN_SIZE,
                    invY - GAP - BTN_SIZE));
            slots.add(new SlotDef("bottom_" + i,
                    invX + 8 + i * BTN_SIZE,
                    invY + INV_H + GAP));
        }
        for (int i = 0; i < 4; i++) {
            slots.add(new SlotDef("left_"  + i,
                    invX - GAP - BTN_SIZE,
                    invY + 8 + i * BTN_SIZE));
            slots.add(new SlotDef("right_" + i,
                    invX + INV_W + GAP,
                    invY + 8 + i * BTN_SIZE));
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (!popupOpen) return super.charTyped(input);
        char c = (char) input.codepoint();
        if (Character.isISOControl(c)) return true;
        if (popupFocus == PopupFocus.LABEL && editLabel.length() < 32) {
            editLabel += c;
        } else if (popupFocus == PopupFocus.COMMAND && editCommand.length() < 128) {
            editCommand += c;
        } else if (popupFocus == PopupFocus.ISLAND_SEARCH && islandSearch.length() < 32) {
            islandSearch += c;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (!popupOpen) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                onClose();
                return true;
            }
            return super.keyPressed(input);
        }
        // Popup open
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            closePopup();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (popupFocus == PopupFocus.LABEL && !editLabel.isEmpty()) {
                editLabel = editLabel.substring(0, editLabel.length() - 1);
            } else if (popupFocus == PopupFocus.COMMAND && !editCommand.isEmpty()) {
                editCommand = editCommand.substring(0, editCommand.length() - 1);
            } else if (popupFocus == PopupFocus.ISLAND_SEARCH && !islandSearch.isEmpty()) {
                islandSearch = islandSearch.substring(0, islandSearch.length() - 1);
            }
            return true;
        }
        // Ctrl+V paste
        if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0
                && input.key() == GLFW.GLFW_KEY_V && minecraft != null) {
            String clip = minecraft.keyboardHandler.getClipboard();
            if (popupFocus == PopupFocus.LABEL) {
                for (int i = 0; i < clip.length() && editLabel.length() < 32; i++) {
                    if (!Character.isISOControl(clip.charAt(i))) editLabel += clip.charAt(i);
                }
            } else if (popupFocus == PopupFocus.COMMAND) {
                for (int i = 0; i < clip.length() && editCommand.length() < 128; i++) {
                    if (!Character.isISOControl(clip.charAt(i))) editCommand += clip.charAt(i);
                }
            } else if (popupFocus == PopupFocus.ISLAND_SEARCH) {
                for (int i = 0; i < clip.length() && islandSearch.length() < 32; i++) {
                    if (!Character.isISOControl(clip.charAt(i))) islandSearch += clip.charAt(i);
                }
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        if (popupOpen) {
            return handlePopupClick(click.x(), click.y());
        }

        // Slot click
        for (SlotDef slot : slots) {
            if (click.x() >= slot.x && click.x() < slot.x + BTN_SIZE
             && click.y() >= slot.y && click.y() < slot.y + BTN_SIZE) {
                openPopup(slot.id);
                return true;
            }
        }

        // Done button
        if (doneRect().contains(click.x(), click.y())) {
            onClose();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BG);

        int cx = width  / 2;
        int cy = height / 2;
        int invX = cx - INV_W / 2;
        int invY = cy - INV_H / 2;

        drawInventory(context, invX, invY);
        drawSlots(context, mouseX, mouseY);

        // Title
        int accent = HudStyle.accent();
        context.centeredText(font,
                Component.literal("Inventory Buttons – Layout"), cx, invY - 36, accent);
        context.centeredText(font,
                Component.literal("Klicke auf einen Slot um einen Button zu konfigurieren."),
                cx, invY - 24, MUTED);

        // Done button
        Rect done = doneRect();
        context.fill(done.x, done.y, done.x + done.w, done.y + done.h, BUTTON_ACTIVE);
        context.centeredText(font, Component.literal("Fertig"),
                done.x + done.w / 2, done.y + 5, TEXT_COLOR);

        if (popupOpen) {
            drawPopup(context, mouseX, mouseY);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawInventory(GuiGraphicsExtractor context, int invX, int invY) {
        // Background
        context.fill(invX, invY, invX + INV_W, invY + INV_H, INV_BG);
        drawBorder(context, invX, invY, INV_W, INV_H, 0xFF556070);

        // Armor slots (4)
        for (int i = 0; i < 4; i++) {
            drawInvSlot(context, invX + 8, invY + 8 + i * 18);
        }
        // Offhand slot
        drawInvSlot(context, invX + 77, invY + 89);
        // Main inventory (9 x 3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawInvSlot(context, invX + 8 + col * 18, invY + 84 + row * 18);
            }
        }
        // Hotbar (9)
        for (int col = 0; col < 9; col++) {
            drawInvSlot(context, invX + 8 + col * 18, invY + 142);
        }

        // Label
        context.centeredText(font,
                Component.literal("Inventar"), invX + INV_W / 2, invY + 4, MUTED);
    }

    private void drawInvSlot(GuiGraphicsExtractor context, int x, int y) {
        context.fill(x, y, x + 16, y + 16, INV_SLOT);
        drawBorder(context, x, y, 16, 16, 0xFF444C5C);
    }

    private void drawSlots(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        List<InventoryButton> buttons = horizonClient.getConfigManager()
                .getConfig().getInventoryButtons();
        for (SlotDef slot : slots) {
            InventoryButton btn = findButton(buttons, slot.id);
            boolean hovered = mouseX >= slot.x && mouseX < slot.x + BTN_SIZE
                           && mouseY >= slot.y && mouseY < slot.y + BTN_SIZE;

            if (btn == null) {
                // Empty slot: grey "+" box
                context.fill(slot.x, slot.y,
                        slot.x + BTN_SIZE, slot.y + BTN_SIZE, BTN_EMPTY);
                if (hovered) {
                    context.fill(slot.x, slot.y,
                            slot.x + BTN_SIZE, slot.y + BTN_SIZE, 0x33FFFFFF);
                }
                drawBorder(context, slot.x, slot.y, BTN_SIZE, BTN_SIZE, BTN_BORDER);
                context.centeredText(font, Component.literal("+"),
                        slot.x + BTN_SIZE / 2, slot.y + 5, hovered ? 0xFFFFFFFF : MUTED);
            } else {
                // Configured button: show item
                context.fill(slot.x, slot.y,
                        slot.x + BTN_SIZE, slot.y + BTN_SIZE, 0x88224455);
                if (hovered) {
                    context.fill(slot.x, slot.y,
                            slot.x + BTN_SIZE, slot.y + BTN_SIZE, BTN_HOVER);
                }
                drawBorder(context, slot.x, slot.y, BTN_SIZE, BTN_SIZE,
                        HudStyle.accent());
                String itemId = btn.itemIdActive;
                ItemStack stack = InventoryButtonItems.resolve(itemId);
                context.item(stack, slot.x + 1, slot.y + 1);
                if (hovered && !btn.label.isBlank()) {
                    context.setTooltipForNextFrame(font,
                            Component.literal(btn.label), mouseX, mouseY);
                }
            }
        }
    }

    private void drawPopup(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int pw = 320, ph = popupHeight();
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;

        context.fill(px, py, px + pw, py + ph, POPUP_BG);
        drawBorder(context, px, py, pw, ph, POPUP_BORDER);

        int accent = HudStyle.accent();
        int y = py + 10;

        // Title
        String title = existingButton
                ? "Button bearbeiten – " + editingSlotId
                : "Neuer Button – " + editingSlotId;
        context.text(font, Component.literal(title), px + 12, y, accent);
        y += 18;

        // Label field
        context.text(font, Component.literal("Label:"), px + 12, y, TEXT_COLOR);
        y += 12;
        boolean labelFocused = popupFocus == PopupFocus.LABEL;
        context.fill(px + 12, y, px + pw - 12, y + 14,
                labelFocused ? 0xFF1A2C40 : 0xFF151E2A);
        drawBorder(context, px + 12, y, pw - 24, 14,
                labelFocused ? accent : POPUP_BORDER);
        String labelDisplay = editLabel.isEmpty() ? "<leer>" : editLabel;
        if (labelFocused) labelDisplay += ((System.currentTimeMillis() / 400L) % 2 == 0 ? "_" : "");
        context.text(font, Component.literal(labelDisplay),
                px + 14, y + 3, labelFocused ? TEXT_COLOR : MUTED);
        y += 20;

        // Function cycle
        context.text(font,
                Component.literal("Funktion: " + editFunction.title()), px + 12, y, TEXT_COLOR);
        Rect funcBtn = new Rect(px + 12, y + 12, 120, 16);
        context.fill(funcBtn.x, funcBtn.y, funcBtn.x + funcBtn.w, funcBtn.y + funcBtn.h, BUTTON_FILL);
        drawBorder(context, funcBtn.x, funcBtn.y, funcBtn.w, funcBtn.h, POPUP_BORDER);
        context.centeredText(font,
                Component.literal("Wechseln"), funcBtn.x + funcBtn.w / 2, funcBtn.y + 4, MUTED);
        y += 32;

        // Command field (only if COMMAND)
        if (editFunction == InventoryButtonFunction.COMMAND) {
            context.text(font,
                    Component.literal("Command (ohne /):"), px + 12, y, TEXT_COLOR);
            y += 12;
            boolean cmdFocused = popupFocus == PopupFocus.COMMAND;
            context.fill(px + 12, y, px + pw - 12, y + 14,
                    cmdFocused ? 0xFF1A2C40 : 0xFF151E2A);
            drawBorder(context, px + 12, y, pw - 24, 14,
                    cmdFocused ? accent : POPUP_BORDER);
            String cmdDisplay = editCommand.isEmpty() ? "<leer>" : editCommand;
            if (cmdFocused) cmdDisplay += ((System.currentTimeMillis() / 400L) % 2 == 0 ? "_" : "");
            context.text(font, Component.literal(cmdDisplay),
                    px + 14, y + 3, cmdFocused ? TEXT_COLOR : MUTED);
            y += 20;
        } else {
            // Description of function
            context.text(font,
                    Component.literal(editFunction.description()), px + 12, y, MUTED);
            y += 14;
        }

        // Toggle flag
        String toggleLabel = editToggle ? "[AN]  Toggle" : "[AUS] Toggle";
        Rect toggleBtn = new Rect(px + 12, y, 130, 16);
        context.fill(toggleBtn.x, toggleBtn.y,
                toggleBtn.x + toggleBtn.w, toggleBtn.y + toggleBtn.h,
                editToggle ? BUTTON_ACTIVE : BUTTON_FILL);
        drawBorder(context, toggleBtn.x, toggleBtn.y, toggleBtn.w, toggleBtn.h, POPUP_BORDER);
        context.centeredText(font, Component.literal(toggleLabel),
                toggleBtn.x + toggleBtn.w / 2, toggleBtn.y + 4, TEXT_COLOR);
        y += 22;

        // Item selectors
        context.text(font,
                Component.literal(editToggle ? "Icon (aktiv):" : "Icon:"), px + 12, y, TEXT_COLOR);
        ItemStack activeStack = InventoryButtonItems.resolve(editItemActive);
        context.item(activeStack, px + 12, y + 10);
        Rect itemActiveBtn = new Rect(px + 32, y + 10, 80, 14);
        context.fill(itemActiveBtn.x, itemActiveBtn.y,
                itemActiveBtn.x + itemActiveBtn.w, itemActiveBtn.y + itemActiveBtn.h, BUTTON_FILL);
        drawBorder(context, itemActiveBtn.x, itemActiveBtn.y, itemActiveBtn.w, itemActiveBtn.h, POPUP_BORDER);
        context.centeredText(font, Component.literal("Waehlen"),
                itemActiveBtn.x + itemActiveBtn.w / 2, itemActiveBtn.y + 3, MUTED);
        y += 28;

        if (editToggle) {
            context.text(font,
                    Component.literal("Icon (inaktiv):"), px + 12, y, TEXT_COLOR);
            ItemStack inactiveStack = InventoryButtonItems.resolve(editItemInactive);
            context.item(inactiveStack, px + 12, y + 10);
            Rect itemInactiveBtn = new Rect(px + 32, y + 10, 80, 14);
            context.fill(itemInactiveBtn.x, itemInactiveBtn.y,
                    itemInactiveBtn.x + itemInactiveBtn.w, itemInactiveBtn.y + itemInactiveBtn.h, BUTTON_FILL);
            drawBorder(context, itemInactiveBtn.x, itemInactiveBtn.y,
                    itemInactiveBtn.w, itemInactiveBtn.h, POPUP_BORDER);
            context.centeredText(font, Component.literal("Waehlen"),
                    itemInactiveBtn.x + itemInactiveBtn.w / 2, itemInactiveBtn.y + 3, MUTED);
            y += 28;
        }

        // Island filter
        String islandFilterLabel = editIslandFilterEnabled ? "Wird angezeigt auf: Ausgewaehlten" : "Wird angezeigt auf: Allen Islands";
        Rect islandToggleBtn = new Rect(px + 12, y, 200, 16);
        context.fill(islandToggleBtn.x, islandToggleBtn.y,
                islandToggleBtn.x + islandToggleBtn.w, islandToggleBtn.y + islandToggleBtn.h,
                editIslandFilterEnabled ? BUTTON_ACTIVE : BUTTON_FILL);
        drawBorder(context, islandToggleBtn.x, islandToggleBtn.y, islandToggleBtn.w, islandToggleBtn.h, POPUP_BORDER);
        context.centeredText(font, Component.literal(islandFilterLabel),
                islandToggleBtn.x + islandToggleBtn.w / 2, islandToggleBtn.y + 4, TEXT_COLOR);
        y += 22;

        if (editIslandFilterEnabled) {
            // Island search field
            boolean islandSearchFocused = popupFocus == PopupFocus.ISLAND_SEARCH;
            context.fill(px + 12, y, px + pw - 12, y + 14,
                    islandSearchFocused ? 0xFF1A2C40 : 0xFF151E2A);
            drawBorder(context, px + 12, y, pw - 24, 14, islandSearchFocused ? accent : POPUP_BORDER);
            String islandSearchDisplay = islandSearch.isEmpty() ? "Islands suchen..." : islandSearch;
            if (islandSearchFocused) islandSearchDisplay += ((System.currentTimeMillis() / 400L) % 2 == 0 ? "_" : "");
            context.text(font, Component.literal(islandSearchDisplay),
                    px + 14, y + 3, islandSearchFocused ? TEXT_COLOR : MUTED);
            y += 20;

            // Island grid – 3 per row
            de.horizon.hypixel.SkyBlockIsland[] islands = filteredIslands();
            int colW = (pw - 24) / 3;
            for (int i = 0; i < islands.length; i++) {
                int col = i % 3;
                int row = i / 3;
                int ix = px + 12 + col * colW;
                int iy = y + row * 18;
                boolean selected = editAllowedIslands.contains(islands[i].id());
                context.fill(ix, iy, ix + colW - 2, iy + 16, selected ? 0xCC1A4A2A : 0xCC1A2030);
                drawBorder(context, ix, iy, colW - 2, 16, selected ? BUTTON_ACTIVE : POPUP_BORDER);
                String mark = selected ? "\u2713 " : "  ";
                context.text(font,
                        Component.literal(mark + islands[i].label()),
                        ix + 4, iy + 4, selected ? TEXT_COLOR : MUTED);
            }
            y += islandRowCount() * 18;
        }

        // Garden-only toggle (only for FARMING_TOOL_REBIND)
        if (editFunction == InventoryButtonFunction.FARMING_TOOL_REBIND) {
            String gardenLabel = editGardenOnly ? "[AN]  Nur auf Garden aktiv" : "[AUS] Nur auf Garden aktiv";
            Rect gardenBtn = new Rect(px + 12, y, 200, 16);
            context.fill(gardenBtn.x, gardenBtn.y,
                    gardenBtn.x + gardenBtn.w, gardenBtn.y + gardenBtn.h,
                    editGardenOnly ? BUTTON_ACTIVE : BUTTON_FILL);
            drawBorder(context, gardenBtn.x, gardenBtn.y, gardenBtn.w, gardenBtn.h, POPUP_BORDER);
            context.centeredText(font, Component.literal(gardenLabel),
                    gardenBtn.x + gardenBtn.w / 2, gardenBtn.y + 4, TEXT_COLOR);
            y += 22;

            String mouseLabel = editSqueakyMousemat ? "[AN]  Squeaky Mousemat" : "[AUS] Squeaky Mousemat";
            Rect mouseBtn = new Rect(px + 12, y, 200, 16);
            context.fill(mouseBtn.x, mouseBtn.y,
                    mouseBtn.x + mouseBtn.w, mouseBtn.y + mouseBtn.h,
                    editSqueakyMousemat ? BUTTON_ACTIVE : BUTTON_FILL);
            drawBorder(context, mouseBtn.x, mouseBtn.y, mouseBtn.w, mouseBtn.h, POPUP_BORDER);
            context.centeredText(font, Component.literal(mouseLabel),
                    mouseBtn.x + mouseBtn.w / 2, mouseBtn.y + 4, TEXT_COLOR);
            y += 22;
        }

        // Action buttons: Save / Delete / Cancel
        y = py + ph - 26;
        Rect saveBtn   = new Rect(px + 12,           y, 80, 18);
        Rect cancelBtn = new Rect(px + pw - 92,      y, 80, 18);
        Rect deleteBtn = existingButton
                ? new Rect(px + 12 + 88 + 8, y, 80, 18)
                : null;

        context.fill(saveBtn.x, saveBtn.y,
                saveBtn.x + saveBtn.w, saveBtn.y + saveBtn.h, BUTTON_ACTIVE);
        context.centeredText(font, Component.literal("Speichern"),
                saveBtn.x + saveBtn.w / 2, saveBtn.y + 5, TEXT_COLOR);

        if (deleteBtn != null) {
            context.fill(deleteBtn.x, deleteBtn.y,
                    deleteBtn.x + deleteBtn.w, deleteBtn.y + deleteBtn.h, BUTTON_DEL);
            context.centeredText(font, Component.literal("Loeschen"),
                    deleteBtn.x + deleteBtn.w / 2, deleteBtn.y + 5, TEXT_COLOR);
        }

        context.fill(cancelBtn.x, cancelBtn.y,
                cancelBtn.x + cancelBtn.w, cancelBtn.y + cancelBtn.h, BUTTON_FILL);
        context.centeredText(font, Component.literal("Abbrechen"),
                cancelBtn.x + cancelBtn.w / 2, cancelBtn.y + 5, MUTED);
    }

    private int popupHeight() {
        int base = 10 + 18 + 12 + 20 + 32; // title + label + function
        if (editFunction == InventoryButtonFunction.COMMAND) base += 12 + 20;
        else base += 14;
        base += 22; // toggle flag
        base += 28; // active icon
        if (editToggle) base += 28; // inactive icon
        base += 22; // island filter toggle
        if (editIslandFilterEnabled) {
            base += 20; // island search field
            base += islandRowCount() * 18; // island grid rows
        }
        if (editFunction == InventoryButtonFunction.FARMING_TOOL_REBIND) {
            base += 22; // garden-only toggle
            base += 22; // squeaky mousemat toggle
        }
        base += 26 + 10; // action buttons + padding
        return base;
    }

    private de.horizon.hypixel.SkyBlockIsland[] filteredIslands() {
        de.horizon.hypixel.SkyBlockIsland[] all = de.horizon.hypixel.SkyBlockIsland.knownIslands();
        if (islandSearch.isBlank()) return all;
        String q = islandSearch.toLowerCase(java.util.Locale.ROOT);
        java.util.List<de.horizon.hypixel.SkyBlockIsland> result = new java.util.ArrayList<>();
        for (de.horizon.hypixel.SkyBlockIsland island : all) {
            if (island.label().toLowerCase(java.util.Locale.ROOT).contains(q)) result.add(island);
        }
        return result.toArray(new de.horizon.hypixel.SkyBlockIsland[0]);
    }

    private int islandRowCount() {
        return Math.max(1, (filteredIslands().length + 2) / 3);
    }

    // ── Popup click handling ──────────────────────────────────────────────────

    private boolean handlePopupClick(double mx, double my) {
        int pw = 320, ph = popupHeight();
        int px = (width  - pw) / 2;
        int py = (height - ph) / 2;
        int y  = py + 10 + 18;

        // Label field area
        int labelY = y + 12;
        if (inRect(mx, my, px + 12, labelY, pw - 24, 14)) {
            popupFocus = PopupFocus.LABEL;
            return true;
        }
        y += 12 + 20;

        // Function cycle button
        int funcBtnY = y + 12;
        if (inRect(mx, my, px + 12, funcBtnY, 120, 16)) {
            InventoryButtonFunction[] vals = InventoryButtonFunction.values();
            editFunction = vals[(editFunction.ordinal() + 1) % vals.length];
            popupFocus = PopupFocus.NONE;
            return true;
        }
        y += 32;

        // Command field (only if COMMAND)
        if (editFunction == InventoryButtonFunction.COMMAND) {
            int cmdY = y + 12;
            if (inRect(mx, my, px + 12, cmdY, pw - 24, 14)) {
                popupFocus = PopupFocus.COMMAND;
                return true;
            }
            y += 12 + 20;
        } else {
            y += 14;
        }

        // Toggle button
        if (inRect(mx, my, px + 12, y, 130, 16)) {
            editToggle = !editToggle;
            popupFocus = PopupFocus.NONE;
            return true;
        }
        y += 22;

        // Active item picker button
        int itemActiveY = y + 10;
        if (inRect(mx, my, px + 32, itemActiveY, 80, 14)) {
            popupFocus = PopupFocus.NONE;
            openItemPicker(chosen -> editItemActive = chosen);
            return true;
        }
        y += 28;

        // Inactive item picker button
        if (editToggle) {
            int itemInactiveY = y + 10;
            if (inRect(mx, my, px + 32, itemInactiveY, 80, 14)) {
                popupFocus = PopupFocus.NONE;
                openItemPicker(chosen -> editItemInactive = chosen);
                return true;
            }
            y += 28;
        }

        // Island filter toggle
        if (inRect(mx, my, px + 12, y, 200, 16)) {
            editIslandFilterEnabled = !editIslandFilterEnabled;
            if (!editIslandFilterEnabled) islandSearch = "";
            popupFocus = PopupFocus.NONE;
            return true;
        }
        y += 22;

        if (editIslandFilterEnabled) {
            // Island search field
            if (inRect(mx, my, px + 12, y, pw - 24, 14)) {
                popupFocus = PopupFocus.ISLAND_SEARCH;
                return true;
            }
            y += 20;

            // Island chips
            de.horizon.hypixel.SkyBlockIsland[] islands = filteredIslands();
            int colW = (pw - 24) / 3;
            for (int i = 0; i < islands.length; i++) {
                int col = i % 3;
                int row = i / 3;
                int ix = px + 12 + col * colW;
                int iy = y + row * 18;
                if (inRect(mx, my, ix, iy, colW - 2, 16)) {
                    String id = islands[i].id();
                    if (!editAllowedIslands.remove(id)) editAllowedIslands.add(id);
                    popupFocus = PopupFocus.NONE;
                    return true;
                }
            }
            y += islandRowCount() * 18;
        }

        // Garden-only toggle (only for FARMING_TOOL_REBIND)
        if (editFunction == InventoryButtonFunction.FARMING_TOOL_REBIND) {
            if (inRect(mx, my, px + 12, y, 200, 16)) {
                editGardenOnly = !editGardenOnly;
                popupFocus = PopupFocus.NONE;
                return true;
            }
            y += 22;

            if (inRect(mx, my, px + 12, y, 200, 16)) {
                editSqueakyMousemat = !editSqueakyMousemat;
                popupFocus = PopupFocus.NONE;
                return true;
            }
            y += 22;
        }

        // Action buttons
        int actionY = py + ph - 26;
        // Save
        if (inRect(mx, my, px + 12, actionY, 80, 18)) {
            savePopup();
            return true;
        }
        // Delete
        if (existingButton && inRect(mx, my, px + 12 + 88, actionY, 80, 18)) {
            deleteButton();
            return true;
        }
        // Cancel
        if (inRect(mx, my, px + pw - 92, actionY, 80, 18)) {
            closePopup();
            return true;
        }

        // MouseButtonEvent outside popup → close
        if (mx < px || mx > px + pw || my < py || my > py + ph) {
            closePopup();
        }
        return true;
    }

    private void openItemPicker(java.util.function.Consumer<String> callback) {
        if (minecraft == null) return;
        // Open the picker and return here afterwards (this screen is the parent).
        minecraft.setScreen(new ItemPickerScreen(this, chosen -> {
            callback.accept(chosen);
            // Re-open this layout screen
            minecraft.setScreen(InventoryButtonLayoutScreen.this);
        }));
    }

    private void savePopup() {
        List<InventoryButton> buttons = horizonClient.getConfigManager()
                .getConfig().getInventoryButtons();
        InventoryButton btn = findButton(buttons, editingSlotId);
        if (btn == null) {
            btn = new InventoryButton();
            btn.slotId = editingSlotId;
            buttons.add(btn);
        }
        btn.label              = editLabel.trim();
        btn.function            = editFunction;
        btn.command             = editCommand.trim();
        btn.toggle              = editToggle;
        btn.itemIdActive        = editItemActive;
        btn.itemIdInactive      = editItemInactive;
        btn.islandFilterEnabled = editIslandFilterEnabled;
        btn.allowedIslands      = new java.util.ArrayList<>(editAllowedIslands);
        btn.gardenOnly          = editGardenOnly;
        btn.squeakyMousemat     = editSqueakyMousemat;
        horizonClient.getConfigManager().save();
        closePopup();
    }

    private void deleteButton() {
        List<InventoryButton> buttons = horizonClient.getConfigManager()
                .getConfig().getInventoryButtons();
        buttons.removeIf(b -> editingSlotId.equals(b.slotId));
        horizonClient.getConfigManager().save();
        closePopup();
    }

    private void openPopup(String slotId) {
        editingSlotId = slotId;
        popupFocus    = PopupFocus.NONE;
        List<InventoryButton> buttons = horizonClient.getConfigManager()
                .getConfig().getInventoryButtons();
        InventoryButton existing = findButton(buttons, slotId);
        if (existing != null) {
            existingButton        = true;
            editLabel             = existing.label;
            editFunction          = existing.function;
            editCommand           = existing.command;
            editToggle            = existing.toggle;
            editItemActive        = existing.itemIdActive;
            editItemInactive      = existing.itemIdInactive;
            editIslandFilterEnabled = existing.islandFilterEnabled;
            editAllowedIslands.clear();
            editAllowedIslands.addAll(existing.allowedIslands);
            editGardenOnly          = existing.gardenOnly;
            editSqueakyMousemat     = existing.squeakyMousemat;
        } else {
            existingButton        = false;
            editLabel             = "";
            editFunction          = InventoryButtonFunction.COMMAND;
            editCommand           = "";
            editToggle            = false;
            editItemActive        = "minecraft:lime_stained_glass_pane";
            editItemInactive      = "minecraft:red_stained_glass_pane";
            editIslandFilterEnabled = false;
            editAllowedIslands.clear();
            editGardenOnly          = false;
            editSqueakyMousemat     = false;
        }
        islandSearch = "";
        popupOpen = true;
    }

    private void closePopup() {
        popupOpen     = false;
        editingSlotId = null;
        popupFocus    = PopupFocus.NONE;
        islandSearch  = "";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Rect doneRect() {
        return new Rect((width - 80) / 2, height - 30, 80, 20);
    }

    private static InventoryButton findButton(List<InventoryButton> buttons, String slotId) {
        for (InventoryButton b : buttons) {
            if (slotId.equals(b.slotId)) return b;
        }
        return null;
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void drawBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record SlotDef(String id, int x, int y) {}
}
