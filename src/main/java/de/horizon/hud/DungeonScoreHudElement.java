package de.horizon.hud;

import de.horizon.config.HorizonConfig;
import de.horizon.config.HudPosition;
import de.horizon.feature.dungeon.DungeonScoreService;
import de.horizon.feature.dungeon.DungeonStateService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * HUD element showing estimated dungeon score in real-time.
 * Displays: total score, grade (S/S+), and component breakdown.
 */
public final class DungeonScoreHudElement implements HudElement {
    private static final String ID = "dungeon_score";

    private final DungeonScoreService scoreService;
    private final DungeonStateService stateService;

    public DungeonScoreHudElement(DungeonScoreService scoreService, DungeonStateService stateService) {
        this.scoreService = scoreService;
        this.stateService = stateService;
    }

    @Override public String id() { return ID; }
    @Override public boolean isMovable() { return true; }
    @Override public int defaultX() { return 5; }
    @Override public int defaultY() { return 80; }
    @Override public int width(Minecraft mc, HudPosition pos) { return 90; }
    @Override public int height(Minecraft mc, HudPosition pos) { return 24; }

    @Override
    public boolean isEnabled(HorizonConfig config) {
        return config.isDungeonScoreEnabled();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Minecraft mc, HudPosition pos, boolean editMode) {
        // Always through the run incl. the blood room. Whether it also stays during the
        // actual boss fight is configurable (default: hide once the boss starts).
        if (!editMode) {
            if (!scoreService.isActive() || !stateService.isInDungeon()) return;
            de.horizon.HorizonClient client = de.horizon.HorizonClient.getInstance();
            boolean showInBoss = client != null && client.getConfigManager() != null
                && client.getConfigManager().getConfig().isDungeonScoreShowInBoss();
            if (!showInBoss && stateService.isBossFightStarted()) return;
        }

        int x = pos.getX();
        int y = pos.getY();

        int floor = stateService.getCurrentFloor();
        boolean master = stateService.isMasterMode();
        boolean inBoss = stateService.isInBoss();

        int total;
        String grade;

        if (editMode) {
            total = 285;
            grade = "S";
        } else {
            total = scoreService.getTotalScore(floor, master, inBoss);
            grade = scoreService.getGrade(floor, master, inBoss);
        }

        int gradeColor = switch (grade) {
            case "S+" -> 0xFFFFD700;
            case "S"  -> 0xFF55FF55;
            case "A"  -> 0xFF55FFFF;
            case "B"  -> 0xFFFFFF55;
            default   -> 0xFFFF5555;
        };

        ctx.fill(x - 1, y - 1, x + 91, y + 25, 0x80000000);
        if (mc.font != null) {
            ctx.text(mc.font, "Score: " + total, x + 2, y + 2, 0xFFFFFFFF);
            ctx.text(mc.font, grade, x + 70, y + 2, gradeColor);
            if (!editMode) {
                int sk = scoreService.getSkillScore(floor, master, inBoss);
                int ex = scoreService.getExplorationScore(floor, master, inBoss);
                int sp = scoreService.getSpeedScore(floor, master);
                int bn = scoreService.getBonusScore(floor, master);
                String detail = sk + "/" + ex + "/" + sp + "/" + bn;
                ctx.text(mc.font, detail, x + 2, y + 14, 0xFFAAAAAA);
            } else {
                ctx.text(mc.font, "100/85/95/5", x + 2, y + 14, 0xFFAAAAAA);
            }
        }
    }
}
