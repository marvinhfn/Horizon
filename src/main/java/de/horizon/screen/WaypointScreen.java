package de.horizon.screen;

import de.horizon.feature.waypoint.Waypoint;
import de.horizon.feature.waypoint.WaypointService;
import de.horizon.hud.HudStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;

/**
 * The {@code /horizon waypoints} menu: a centered frosted window (matching the config menu). Pick an
 * island via a dropdown, toggle edit mode, import/export, group waypoints and set a group's ordered
 * route, and configure each waypoint (name, type, through-walls, beacon, group, colour via an HSB
 * picker). Opening with a specific waypoint jumps straight to that waypoint's config.
 */
public final class WaypointScreen extends Screen {
    // Config-menu palette (frosted light panel + dark text) so it looks like the rest of the config.
    private static final int WINDOW = 0x66F0F1F3;
    private static final int HEADER = 0x73F7F8FA;
    private static final int CARD = 0x60E6E8EC;
    private static final int CARD_HOVER = 0x80CFE0FF;
    private static final int DARK = 0xFF1E2A37;
    private static final int MUTED = 0xFF5A6472;

    private static final String[] TYPE_LABELS = { "Outlined", "Box", "Outline+Box" };

    private final Screen parent;
    private final WaypointService service;

    private String islandId;
    private Waypoint editing;
    private boolean nameFocused = false;
    private boolean islandDropdownOpen = false;

    // Window geometry (centered).
    private int fx, fy, fw, fh;

    public WaypointScreen(Screen parent, WaypointService service, Waypoint jumpTo) {
        super(Component.literal("Waypoints"));
        this.parent = parent;
        this.service = service;
        this.islandId = service.currentIslandId();
        if (islandDropdownIndex() < 0) this.islandId = "unknown";
        this.editing = jumpTo;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        fw = 360;
        fh = Math.min(height - 40, 320);
        fx = (width - fw) / 2;
        fy = (height - fh) / 2;
    }

    @Override
    public void onClose() {
        service.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private int islandDropdownIndex() {
        List<String> ids = service.knownIslandIds();
        return ids.indexOf(islandId);
    }

    // ── Input ────────────────────────────────────────────────────────────────────

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (editing != null && nameFocused && !Character.isISOControl(input.codepoint()) && editing.name.length() < 40) {
            editing.name += Character.toString(input.codepoint());
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (editing != null && nameFocused) {
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!editing.name.isEmpty()) editing.name = editing.name.substring(0, editing.name.length() - 1);
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_ESCAPE) { nameFocused = false; return true; }
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (islandDropdownOpen) { islandDropdownOpen = false; return true; }
            if (editing != null) { editing = null; return true; }
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (in(mx, my, fx + fw - 18, fy + 8, 14, 14)) { onClose(); return true; }
        if (editing != null) return clickEdit(mx, my);
        return clickList(mx, my);
    }

    private boolean clickList(int mx, int my) {
        int x = fx + 12;
        int y = fy + 32;
        // Island dropdown
        if (in(mx, my, x, y, fw - 24, 16)) { islandDropdownOpen = !islandDropdownOpen; return true; }
        if (islandDropdownOpen) {
            List<String> ids = service.knownIslandIds();
            for (int i = 0; i < ids.size(); i++) {
                if (in(mx, my, x, y + 18 + i * 14, fw - 24, 14)) {
                    islandId = ids.get(i); islandDropdownOpen = false; editing = null; return true;
                }
            }
            return true;
        }
        y += 22;
        if (in(mx, my, x, y, 110, 16)) { service.setEditMode(!service.isEditMode()); return true; }
        if (in(mx, my, x + 120, y, 100, 16)) { // export
            if (minecraft != null) minecraft.keyboardHandler.setClipboard(service.exportIsland(islandId));
            return true;
        }
        if (in(mx, my, x + 230, y, 100, 16)) { // import
            if (minecraft != null) service.importIsland(islandId, minecraft.keyboardHandler.getClipboard());
            return true;
        }
        y += 24;
        // grouped list
        List<Waypoint> list = service.waypoints(islandId);
        for (String group : service.groups(islandId)) {
            List<Waypoint> inGroup = list.stream().filter(w -> group.equals(safeGroup(w))).toList();
            if (inGroup.isEmpty() && !group.equals("Default")) continue;
            // group header row: name + sorted toggle
            if (in(mx, my, fx + fw - 100, y, 90, 12)) {
                service.setGroupSorted(islandId, group, !service.isGroupSorted(islandId, group));
                return true;
            }
            y += 14;
            for (Waypoint w : inGroup) {
                if (in(mx, my, fx + fw - 30, y, 18, 12)) { service.remove(islandId, w); return true; }
                if (in(mx, my, x, y, fw - 50, 12)) { editing = w; nameFocused = false; return true; }
                y += 14;
            }
        }
        return true;
    }

    private boolean clickEdit(int mx, int my) {
        int x = fx + 12, y = fy + 32;
        if (in(mx, my, x, y, fw - 24, 16)) { nameFocused = true; return true; }
        y += 22;
        if (in(mx, my, x, y, 170, 16)) { editing.type = (editing.type + 1) % 3; service.save(); return true; }
        y += 20;
        if (in(mx, my, x, y, 170, 16)) { editing.throughWalls = !editing.throughWalls; service.save(); return true; }
        y += 20;
        if (in(mx, my, x, y, 170, 16)) { editing.beacon = !editing.beacon; service.save(); return true; }
        y += 20;
        if (in(mx, my, x, y, 170, 16)) { editing.group = cycleGroup(editing.group); service.save(); return true; }
        y += 22;
        // HSB colour picker (hue bar + SV box)
        int svX = x, svY = y, svW = 120, svH = 60;
        int hueX = x + svW + 10, hueW = 14;
        if (in(mx, my, svX, svY, svW, svH)) { pickSV(mx - svX, my - svY, svW, svH); service.save(); return true; }
        if (in(mx, my, hueX, svY, hueW, svH)) { pickHue(my - svY, svH); service.save(); return true; }
        y += svH + 8;
        if (in(mx, my, x, y, 90, 16)) { service.remove(islandId, editing); editing = null; return true; }
        if (in(mx, my, x + 100, y, 90, 16)) { editing = null; return true; }
        return true;
    }

    private static String safeGroup(Waypoint w) {
        return w.group == null || w.group.isBlank() ? "Default" : w.group;
    }

    private String cycleGroup(String cur) {
        List<String> gs = service.groups(islandId);
        int idx = Math.max(0, gs.indexOf(cur == null ? "Default" : cur));
        // Offer existing groups plus a "Group N" fresh option.
        if (idx + 1 < gs.size()) return gs.get(idx + 1);
        return "Group " + (gs.size() + 1);
    }

    private void pickSV(int lx, int ly, int w, int h) {
        float[] hsb = Color.RGBtoHSB((editing.color >> 16) & 0xFF, (editing.color >> 8) & 0xFF, editing.color & 0xFF, null);
        float s = clamp01(lx / (float) w);
        float v = 1f - clamp01(ly / (float) h);
        editing.color = 0xFF000000 | (Color.HSBtoRGB(hsb[0], s, v) & 0xFFFFFF);
    }

    private void pickHue(int ly, int h) {
        float[] hsb = Color.RGBtoHSB((editing.color >> 16) & 0xFF, (editing.color >> 8) & 0xFF, editing.color & 0xFF, null);
        float hue = clamp01(ly / (float) h);
        editing.color = 0xFF000000 | (Color.HSBtoRGB(hue, hsb[1] <= 0 ? 1 : hsb[1], hsb[2] <= 0 ? 1 : hsb[2]) & 0xFFFFFF);
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    // ── Render ───────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        // Centered frosted window over the vanilla dim (matches the config menu look).
        ctx.fill(fx, fy, fx + fw, fy + fh, WINDOW);
        ctx.fill(fx, fy, fx + fw, fy + 26, HEADER);
        ctx.outline(fx, fy, fw, fh, HudStyle.border());
        ctx.text(font, Component.literal("Waypoints"), fx + 12, fy + 9, HudStyle.accent());
        ctx.text(font, Component.literal("§c[X]"), fx + fw - 18, fy + 8, 0xFFCC3333);

        if (editing != null) { renderEdit(ctx, mouseX, mouseY); super.extractRenderState(ctx, mouseX, mouseY, delta); return; }

        int x = fx + 12, y = fy + 32;
        // Island dropdown
        ctx.fill(x, y, fx + fw - 12, y + 16, CARD);
        ctx.text(font, Component.literal("Insel: " + WaypointService.islandLabel(islandId) + "  ▾"), x + 4, y + 4, DARK);
        if (islandDropdownOpen) {
            List<String> ids = service.knownIslandIds();
            int dy = y + 18;
            ctx.fill(x, dy, fx + fw - 12, dy + ids.size() * 14, 0xF0202428);
            for (int i = 0; i < ids.size(); i++) {
                boolean hov = in(mouseX, mouseY, x, dy + i * 14, fw - 24, 14);
                ctx.text(font, Component.literal(WaypointService.islandLabel(ids.get(i))), x + 4, dy + i * 14 + 3, hov ? 0xFF55FFFF : 0xFFDDDDDD);
            }
            super.extractRenderState(ctx, mouseX, mouseY, delta);
            return;
        }
        y += 22;
        drawBtn(ctx, x, y, 110, "Edit: " + (service.isEditMode() ? "AN" : "AUS"), service.isEditMode());
        drawBtn(ctx, x + 120, y, 100, "Export", false);
        drawBtn(ctx, x + 230, y, 100, "Import", false);
        y += 24;

        List<Waypoint> list = service.waypoints(islandId);
        if (list.isEmpty()) {
            ctx.text(font, Component.literal("Keine Waypoints. Edit Mode an + Block rechtsklicken."), x, y, MUTED);
        }
        for (String group : service.groups(islandId)) {
            List<Waypoint> inGroup = list.stream().filter(w -> group.equals(safeGroup(w))).toList();
            if (inGroup.isEmpty() && !group.equals("Default")) continue;
            boolean sorted = service.isGroupSorted(islandId, group);
            ctx.text(font, Component.literal("§8§l" + group), x, y + 2, DARK);
            drawBtn(ctx, fx + fw - 100, y, 90, sorted ? "Sorted: AN" : "Sorted: AUS", sorted);
            y += 14;
            for (Waypoint w : inGroup) {
                boolean hov = in(mouseX, mouseY, x, y, fw - 50, 12);
                if (hov) ctx.fill(x, y, fx + fw - 30, y + 12, CARD_HOVER);
                ctx.text(font, Component.literal(w.name + " §8[" + w.x + "," + w.y + "," + w.z + "]"), x + 2, y + 2, w.color);
                ctx.fill(fx + fw - 30, y, fx + fw - 12, y + 12, 0xFF7A2A2A);
                ctx.text(font, Component.literal("§f✕"), fx + fw - 26, y + 2, 0xFFFFFFFF);
                y += 14;
            }
        }
        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    private void renderEdit(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int x = fx + 12, y = fy + 32;
        String cur = nameFocused && (System.currentTimeMillis() / 400 % 2 == 0) ? "_" : "";
        ctx.fill(x, y, fx + fw - 12, y + 16, CARD);
        ctx.text(font, Component.literal("Name: " + editing.name + cur), x + 4, y + 4, DARK);
        y += 22;
        drawBtn(ctx, x, y, 170, "Typ: " + TYPE_LABELS[editing.type], true);
        y += 20;
        drawBtn(ctx, x, y, 170, "Durch Waende: " + (editing.throughWalls ? "AN" : "AUS"), editing.throughWalls);
        y += 20;
        drawBtn(ctx, x, y, 170, "Beacon: " + (editing.beacon ? "AN" : "AUS"), editing.beacon);
        y += 20;
        drawBtn(ctx, x, y, 170, "Gruppe: " + safeGroup(editing), false);
        y += 22;
        // HSB picker
        float[] hsb = Color.RGBtoHSB((editing.color >> 16) & 0xFF, (editing.color >> 8) & 0xFF, editing.color & 0xFF, null);
        int svX = x, svY = y, svW = 120, svH = 60, hueX = x + svW + 10, hueW = 14;
        for (int px = 0; px < svW; px += 2) for (int py = 0; py < svH; py += 2) {
            int c = 0xFF000000 | (Color.HSBtoRGB(hsb[0], px / (float) svW, 1f - py / (float) svH) & 0xFFFFFF);
            ctx.fill(svX + px, svY + py, svX + px + 2, svY + py + 2, c);
        }
        for (int py = 0; py < svH; py += 2) {
            int c = 0xFF000000 | (Color.HSBtoRGB(py / (float) svH, 1f, 1f) & 0xFFFFFF);
            ctx.fill(hueX, svY + py, hueX + hueW, svY + py + 2, c);
        }
        // preview swatch
        ctx.fill(hueX + hueW + 8, svY, hueX + hueW + 32, svY + 24, editing.color);
        y += svH + 8;
        drawBtn(ctx, x, y, 90, "Loeschen", false);
        drawBtn(ctx, x + 100, y, 90, "Zurueck", false);
    }

    private void drawBtn(GuiGraphicsExtractor ctx, int x, int y, int w, String label, boolean on) {
        ctx.fill(x, y, x + w, y + 16, on ? 0xFF2DBA68 : CARD);
        ctx.text(font, Component.literal(label), x + 4, y + 4, on ? 0xFFFFFFFF : DARK);
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
