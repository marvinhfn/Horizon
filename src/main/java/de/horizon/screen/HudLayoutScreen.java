package de.horizon.screen;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.hud.HudElement;
import de.horizon.hud.HudStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Locale;

public final class HudLayoutScreen extends Screen {
    private final Screen parent;
    private final HorizonClient horizonClient;

    private HudElement selectedElement;
    private HudElement draggedElement;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudLayoutScreen(Screen parent, HorizonClient horizonClient) {
        super(Text.literal("HUD Layout"));
        this.parent = parent;
        this.horizonClient = horizonClient;
    }

    @Override
    public void close() {
        horizonClient.getConfigManager().save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        Rect panel = sidePanel();
        if (doneRect(panel).contains(click.x(), click.y())) {
            close();
            return true;
        }

        if (selectedElement != null && resetRect(panel).contains(click.x(), click.y())) {
            horizonClient.getConfigManager().resetPosition(selectedElement.id(), selectedElement.defaultX(), selectedElement.defaultY());
            return true;
        }

        if (selectedElement != null && scaleRect(panel, true).contains(click.x(), click.y())) {
            adjustScale(selectedElement, -0.1D);
            return true;
        }

        if (selectedElement != null && scaleRect(panel, false).contains(click.x(), click.y())) {
            adjustScale(selectedElement, 0.1D);
            return true;
        }

        HudElement hit = findHitElement(click.x(), click.y());
        if (hit != null) {
            selectedElement = hit;
            draggedElement = hit;
            HudPosition position = positionOf(hit);
            dragOffsetX = (int) click.x() - position.getX();
            dragOffsetY = (int) click.y() - position.getY();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() == 0 && draggedElement != null && client != null) {
            HudPosition position = positionOf(draggedElement);
            int maxX = Math.max(0, width - draggedElement.width(client, position));
            int maxY = Math.max(0, height - draggedElement.height(client, position));
            position.setX(clamp((int) click.x() - dragOffsetX, 0, maxX));
            position.setY(clamp((int) click.y() - dragOffsetY, 0, maxY));
            horizonClient.getConfigManager().save();
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            draggedElement = null;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudElement hovered = findHitElement(mouseX, mouseY);
        if (hovered != null) {
            selectedElement = hovered;
            adjustScale(hovered, verticalAmount > 0 ? 0.05D : -0.05D);
            return true;
        }

        if (selectedElement != null) {
            adjustScale(selectedElement, verticalAmount > 0 ? 0.05D : -0.05D);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, HudStyle.backdrop());

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        HorizonConfig config = horizonClient.getConfigManager().getConfig();
        for (HudElement element : horizonClient.getHudRegistry().getElements()) {
            if (!element.isMovable() || !element.isEnabled(config)) {
                continue;
            }

            HudPosition position = positionOf(element);
            element.render(context, minecraftClient, position, true);
            if (element == selectedElement) {
                context.drawStrokedRectangle(position.getX() - 2, position.getY() - 2, element.width(minecraftClient, position) + 4, element.height(minecraftClient, position) + 4, HudStyle.selected());
            }
        }

        Rect panel = sidePanel();
        context.fill(panel.x, panel.y, panel.right(), panel.bottom(), HudStyle.panel());
        context.drawStrokedRectangle(panel.x, panel.y, panel.width, panel.height, HudStyle.border());
        context.drawTextWithShadow(textRenderer, title, panel.x + 16, panel.y + 16, HudStyle.accent());
        context.drawTextWithShadow(textRenderer, Text.literal("Ziehen verschiebt. Mausrad oder +/- aendert die Groesse."), panel.x + 16, panel.y + 34, HudStyle.muted());

        String selection = selectedElement == null ? "Kein HUD ausgewaehlt" : "Auswahl: " + selectedElement.id();
        context.drawTextWithShadow(textRenderer, Text.literal(selection), panel.x + 16, panel.y + 62, HudStyle.text());

        String scaleText = selectedElement == null ? "--" : String.format(Locale.ROOT, "%.2fx", positionOf(selectedElement).getScale());
        context.fill(panel.x + 12, panel.y + 82, panel.right() - 12, panel.y + 138, HudStyle.panelAlt());
        context.drawStrokedRectangle(panel.x + 12, panel.y + 82, panel.width - 24, 56, HudStyle.border());
        context.drawTextWithShadow(textRenderer, Text.literal("Groesse"), panel.x + 24, panel.y + 92, HudStyle.muted());
        drawAction(context, scaleRect(panel, true), "-");
        drawAction(context, scaleRect(panel, false), "+");

        Rect valueRect = scaleValueRect(panel);
        context.fill(valueRect.x, valueRect.y, valueRect.right(), valueRect.bottom(), HudStyle.action());
        context.drawStrokedRectangle(valueRect.x, valueRect.y, valueRect.width, valueRect.height, HudStyle.border());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(scaleText), valueRect.centerX(), valueRect.y + 8, HudStyle.text());

        drawAction(context, resetRect(panel), "Reset HUD");
        drawDone(context, doneRect(panel), "Schliessen");

        super.render(context, mouseX, mouseY, delta);
    }

    private void adjustScale(HudElement element, double delta) {
        HudPosition position = positionOf(element);
        position.setScale(clampScale(position.getScale() + delta));
        horizonClient.getConfigManager().save();
    }

    private HudPosition positionOf(HudElement element) {
        return horizonClient.getConfigManager().getOrCreatePosition(element.id(), element.defaultX(), element.defaultY());
    }

    private HudElement findHitElement(double mouseX, double mouseY) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        HorizonConfig config = horizonClient.getConfigManager().getConfig();
        for (int index = horizonClient.getHudRegistry().getElements().size() - 1; index >= 0; index--) {
            HudElement element = horizonClient.getHudRegistry().getElements().get(index);
            if (!element.isMovable() || !element.isEnabled(config)) {
                continue;
            }

            HudPosition position = positionOf(element);
            int x = position.getX();
            int y = position.getY();
            int width = element.width(minecraftClient, position);
            int height = element.height(minecraftClient, position);
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                return element;
            }
        }

        return null;
    }

    private Rect sidePanel() {
        int panelWidth = 224;
        return new Rect(Math.max(12, width - panelWidth - 16), 18, panelWidth, 212);
    }

    private Rect scaleRect(Rect panel, boolean minus) {
        return new Rect(panel.x + (minus ? 24 : 174), panel.y + 106, 28, 24);
    }

    private Rect scaleValueRect(Rect panel) {
        return new Rect(panel.x + 62, panel.y + 106, 104, 24);
    }

    private Rect resetRect(Rect panel) {
        return new Rect(panel.x + 24, panel.y + 148, panel.width - 48, 24);
    }

    private Rect doneRect(Rect panel) {
        return new Rect(panel.x + 24, panel.y + 178, panel.width - 48, 24);
    }

    private void drawAction(DrawContext context, Rect rect, String label) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), HudStyle.action());
        context.drawStrokedRectangle(rect.x, rect.y, rect.width, rect.height, HudStyle.border());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), rect.centerX(), rect.y + 8, HudStyle.text());
    }

    private void drawDone(DrawContext context, Rect rect, String label) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), HudStyle.accent());
        context.drawStrokedRectangle(rect.x, rect.y, rect.width, rect.height, HudStyle.border());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), rect.centerX(), rect.y + 8, 0xFF0A1016);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampScale(double scale) {
        return Math.max(0.60D, Math.min(3.00D, scale));
    }

    private record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        int centerX() {
            return x + (width / 2);
        }

        boolean contains(double px, double py) {
            return px >= x && px <= right() && py >= y && py <= bottom();
        }
    }
}
