package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.mining.CommissionService;
import de.horizon.screen.render.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Movable HUD listing the active commissions read from the tab list by {@link CommissionService}. */
public final class CommissionHudElement implements HudElement {
    private static final int DONE = 0xFF55FF55;
    private static final int LINE_GAP = 2;

    private final CommissionService service;

    public CommissionHudElement(CommissionService service) {
        this.service = service;
    }

    @Override public String id() { return "commission_hud"; }
    @Override public boolean isEnabled(HorizonConfig config) { return config.isCommissionHudEnabled(); }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return 5; }
    @Override public int defaultY() { return 60; }

    private List<String> lines(boolean editorMode) {
        List<String> out = new ArrayList<>();
        out.add("Commissions");
        if (editorMode || service.getCommissions().isEmpty()) {
            out.add("Titanium Miner: 45%");
            out.add("Goblin Raid: DONE");
            return out;
        }
        for (CommissionService.Commission c : service.getCommissions()) {
            out.add(c.name() + ": " + c.value());
        }
        return out;
    }

    @Override
    public int width(Minecraft client, HudPosition position) {
        int max = 0;
        for (String l : lines(false)) max = Math.max(max, client.font.width(l));
        return (int) Math.ceil(max * position.getScale());
    }

    @Override
    public int height(Minecraft client, HudPosition position) {
        int n = lines(false).size();
        return (int) Math.ceil((n * (client.font.lineHeight + LINE_GAP)) * position.getScale());
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, Minecraft client, HudPosition position, boolean editorMode) {
        // Outside a commission zone there's nothing to show — stay hidden unless we're laying it out.
        if (!editorMode && !service.hasCommissions()) return;

        List<String> lines = lines(editorMode);
        int guiW = client.getWindow().getGuiScaledWidth();
        int guiH = client.getWindow().getGuiScaledHeight();
        position.migrateIfNeeded(guiW, guiH);

        drawContext.pose().pushMatrix();
        drawContext.pose().translate(position.resolveX(guiW), position.resolveY(guiH));
        drawContext.pose().scale((float) position.getScale(), (float) position.getScale());

        int y = 0;
        int lineH = client.font.lineHeight + LINE_GAP;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int color = i == 0 ? HudStyle.accent()
                    : line.toUpperCase(java.util.Locale.ROOT).endsWith("DONE") ? DONE
                    : HudStyle.text();
            drawContext.text(client.font, Fonts.hud(line), 0, y, color, true);
            y += lineH;
        }
        drawContext.pose().popMatrix();
    }
}
