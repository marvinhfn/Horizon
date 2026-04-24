package de.horizon.screen;

import de.horizon.hud.HudStyle;
import de.horizon.hypixel.HypixelDungeonStats;
import de.horizon.hypixel.HypixelProfileService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class PlayerProfileScreen extends Screen {
    private static final String[] FLOOR_KEYS = {"0", "1", "2", "3", "4", "5", "6", "7"};
    private static final String[] FLOOR_LABELS = {"Entrance", "Floor I", "Floor II", "Floor III", "Floor IV", "Floor V", "Floor VI", "Floor VII"};
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB8B8B8;
    private static final int WARNING = 0xFFFFD27C;
    private static final int WINDOW = 0x66F0F1F3;
    private static final int WINDOW_HEADER = 0x73F7F8FA;
    private static final int CARD = 0x60F0F1F3;
    private static final int CARD_FOCUSED = 0x60F7F8FA;
    private static final int BUTTON = 0x60E6E8EC;
    private static final int BUTTON_TEXT = 0xFF1E2A37;

    private final Screen parent;
    private final String requestedPlayer;
    private final HypixelProfileService profileService;

    private CompletableFuture<Void> loadFuture;
    private HypixelDungeonStats stats;
    private String error;
    private ViewerTab activeTab = ViewerTab.OVERVIEW;
    private int contentScrollOffset = 0;

    public PlayerProfileScreen(Screen parent, String requestedPlayer, HypixelProfileService profileService) {
        super(Text.literal("Horizon Viewer"));
        this.parent = parent;
        this.requestedPlayer = requestedPlayer;
        this.profileService = profileService;
    }

    @Override
    protected void init() {
        super.init();
        if (loadFuture == null) {
            loadFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return profileService.load(requestedPlayer);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }).handle((loaded, throwable) -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        if (throwable != null) {
                            Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
                            error = cause.getMessage() == null ? "Profil konnte nicht geladen werden." : cause.getMessage();
                        } else {
                            stats = loaded;
                        }
                    });
                }
                return null;
            });
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Rect frame = frame();
        if (!frame.contains(click.x(), click.y())) {
            return super.mouseClicked(click, doubled);
        }
        if (click.button() == 0) {
            if (closeRect(frame).contains(click.x(), click.y())) {
                close();
                return true;
            }
            Rect sidebar = sidebarRect(frame);
            for (int index = 0; index < ViewerTab.values().length; index++) {
                if (sidebarTabRect(sidebar, index).contains(click.x(), click.y())) {
                    activeTab = ViewerTab.values()[index];
                    contentScrollOffset = 0;
                    return true;
                }
            }
            if (stats != null) {
                Rect viewport = contentViewportRect(frame);
                int actionY = linksActionY(viewport);
                if (activeTab == ViewerTab.STORAGE && actionButtonRect(viewport.x, actionY, 0).contains(click.x(), click.y())) {
                    openUrl(skyCryptUrl(stats.username()));
                    return true;
                }
                if (activeTab == ViewerTab.STORAGE && actionButtonRect(viewport.x, actionY, 1).contains(click.x(), click.y())) {
                    openUrl(nameMcUrl(stats.username()));
                    return true;
                }
                if (activeTab == ViewerTab.STORAGE && actionButtonRect(viewport.x, actionY, 2).contains(click.x(), click.y())) {
                    copyCommand(stats.username());
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Rect viewport = contentViewportRect(frame());
        if (!viewport.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int maxScroll = Math.max(0, contentHeight() - viewport.height);
        contentScrollOffset = Math.max(0, Math.min(maxScroll, contentScrollOffset - (int) Math.round(verticalAmount * 28.0D)));
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Rect frame = frame();
        Rect sidebar = sidebarRect(frame);
        Rect viewport = contentViewportRect(frame);
        Rect clip = contentClipRect(frame);

        drawWindowChrome(context, frame);

        for (int index = 0; index < ViewerTab.values().length; index++) {
            boolean active = ViewerTab.values()[index] == activeTab;
            Rect rect = sidebarTabRect(sidebar, index);
            drawText(context, rect.x, rect.y, (active ? "> " : "  ") + ViewerTab.values()[index].label, active ? HudStyle.accent() : TEXT);
        }

        context.enableScissor(clip.x, clip.y, clip.right(), clip.bottom());
        if (stats == null && error == null) {
            drawLoading(context, viewport);
        } else if (error != null) {
            drawError(context, viewport);
        } else {
            switch (activeTab) {
                case OVERVIEW -> drawOverview(context, viewport);
                case DUNGEONS -> drawDungeons(context, viewport);
                case SKILLS -> drawSkills(context, viewport);
                case SLAYERS -> drawSlayers(context, viewport);
                case WEALTH -> drawWealth(context, viewport);
                case STORAGE -> drawStorage(context, viewport);
            }
        }
        context.disableScissor();

        drawScrollBar(context, viewport);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawOverview(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Profile Overview", "Grafische Profilansicht angelehnt an SkyCrypt/NEU.");
        y = drawOverviewGrid(context, viewport.x, y);
        y = drawProfileListSection(context, viewport.x, y, "Profiles");
        drawVisualFooter(context, viewport.x, y, "Viewer Data", "Die Darstellung bleibt innerhalb des Viewports geclippt und scrollt sauber.");
    }

    private void drawDungeons(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Dungeon Stats", "Catacombs, Secrets und bekannte S+ Zeiten.");
        y = drawDungeonSummary(context, viewport.x, y);
        for (int index = 0; index < FLOOR_KEYS.length; index++) {
            y = drawDungeonRow(context, viewport.x, y, index);
        }
    }

    private void drawSkills(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Skills", "Skill-Level, Average Skill und Fortschritt.");
        y = drawSkillSummary(context, viewport.x, y);
        drawSkillGrid(context, viewport.x, y);
    }

    private void drawSlayers(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Slayers", "Slayer-Level und gesamte Slayer XP.");
        y = drawLinkCard(context, viewport.x, y, "Total Slayer XP", formatCoins(stats.totalSlayerXp()), "Zusammengefasste Slayer Erfahrung.");
        drawSlayerGrid(context, viewport.x, y);
    }

    private void drawWealth(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Networth", "Purse, Bank und Networth nach Kategorien.");
        y = drawWealthSummary(context, viewport.x, y);
        drawNetworthGrid(context, viewport.x, y);
    }

    private void drawStorage(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Inventory / Storage", "Inventory, Ender Chest, Vault, Wardrobe, Pets und Accessories als Kategorien.");
        y = drawStorageIntro(context, viewport.x, y);
        y = drawStorageCategoryCards(context, viewport.x, y);
        y = drawLinkCard(context, viewport.x, y, "SkyCrypt", skyCryptUrl(stats.username()), "Fuer itemgenaue Inhalte, wenn der oeffentliche Item-Endpoint verfuegbar ist.");
        y = drawLinkCard(context, viewport.x, y, "NameMC", nameMcUrl(stats.username()), "Name-Historie und UUID anschauen.");
        y = drawLinkCard(context, viewport.x, y, "Command", "/hv " + stats.username(), "Viewer fuer diesen Spieler erneut oeffnen.");
        drawActionCard(context, viewport.x, y, "Aktionen", "SkyCrypt, NameMC oder Copy /hv.");
    }

    private void drawLoading(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Profile Viewer", "Lade Daten fuer " + displayRequestedPlayer() + "...");
        y = drawLinkCard(context, viewport.x, y, "Status", "Lade Profil...", "Es werden oeffentliche und optionale API-Daten zusammengefuehrt.");
        drawVisualFooter(context, viewport.x, y, "Hinweis", "Ohne Hypixel API Key werden nur verfuegbare oeffentliche Daten angezeigt.");
    }

    private void drawError(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Profile Viewer", "Der Abruf konnte nicht abgeschlossen werden.");
        y = drawLinkCard(context, viewport.x, y, "Abruf fehlgeschlagen", error, "Pruefe Spielername, Verbindung oder externe APIs.");
        drawVisualFooter(context, viewport.x, y, "Hinweis", "Mit Hypixel API Key sind zusaetzliche Dungeon-Daten verfuegbar.");
    }

    private int drawHeroCard(DrawContext context, int x, int y, String title, String subtitle) {
        int height = 140;
        drawSettingCard(context, x, y, height, HudStyle.accent());
        Rect avatar = new Rect(x + 8, y + 10, 84, 84);
        context.fill(avatar.x, avatar.y, avatar.right(), avatar.bottom(), CARD_FOCUSED);
        context.fill(avatar.x + 10, avatar.y + 10, avatar.right() - 10, avatar.bottom() - 10, BUTTON);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(initials()), avatar.centerX(), avatar.y + 34, BUTTON_TEXT);
        drawText(context, x + 108, y + 12, title, TEXT);
        drawWrappedText(context, x + 108, y + 28, subtitle, 510, MUTED);
        drawText(context, x + 108, y + 60, stats == null ? displayRequestedPlayer() : stats.username(), HudStyle.accent());
        drawText(context, x + 108, y + 76, "Profil " + (stats == null ? "--" : displayProfileName()), TEXT);
        drawText(context, x + 108, y + 92, "UUID " + compactUuid(), MUTED);
        drawProgressBar(context, x + 108, y + 110, 232, 10, statProgress(stats == null ? 0 : stats.catacombsLevel(), 50), HudStyle.accent(), "Catacombs");
        drawProgressBar(context, x + 356, y + 110, 232, 10, statProgress(stats == null ? 0 : stats.totalCompletions(), 500), HudStyle.selected(), "Completions");
        return y + height;
    }

    private int drawOverviewGrid(DrawContext context, int x, int y) {
        Rect left = new Rect(x - 12, y, 302, 176);
        Rect right = new Rect(x + 300, y, 309, 176);
        context.fill(left.x, left.y, left.right(), left.bottom(), CARD);
        context.fill(right.x, right.y, right.right(), right.bottom(), CARD);
        drawText(context, x, y + 10, "Core Stats", TEXT);
        drawText(context, x + 312, y + 10, "Profile Summary", TEXT);
        drawKeyValue(context, x, y + 34, "SkyBlock Level", formatNumber(stats.skyblockLevel()));
        drawKeyValue(context, x, y + 56, "Avg Skill", String.format(Locale.ROOT, "%.2f", stats.averageSkillLevel()));
        drawKeyValue(context, x, y + 78, "Slayer XP", formatCoins(stats.totalSlayerXp()));
        drawKeyValue(context, x, y + 100, "Player", stats.username());
        drawKeyValue(context, x, y + 122, "Profile", displayProfileName());
        drawWrappedText(context, x + 312, y + 34, "Die grafische Ansicht orientiert sich an einem Profile-Viewer statt an einer reinen Setting-Liste.", 280, MUTED);
        drawWrappedText(context, x + 312, y + 74, "Karten, Balken und Networth-Kategorien bleiben im Content-Viewport und werden beim Scrollen sauber abgeschnitten.", 280, MUTED);
        drawProgressBar(context, x + 312, y + 122, 250, 10, statProgress(stats.skyblockLevel(), 500), HudStyle.accent(), "Level Progress");
        drawProgressBar(context, x + 312, y + 146, 250, 10, statProgress((int) Math.round(stats.averageSkillLevel()), 60), HudStyle.selected(), "Skill Progress");
        return y + 188;
    }

    private int drawProfileListSection(DrawContext context, int x, int y, String title) {
        int height = 34 + Math.max(1, stats.profileNames().size()) * 22;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, title, TEXT);
        int rowY = y + 34;
        if (stats.profileNames().isEmpty()) {
            drawText(context, x + 14, rowY, displayProfileName(), HudStyle.accent());
        } else {
            for (String profile : stats.profileNames()) {
                drawText(context, x + 14, rowY, profile, profile.startsWith(displayProfileName()) ? HudStyle.accent() : TEXT);
                rowY += 22;
            }
        }
        return y + height;
    }

    private int drawFloorChipSection(DrawContext context, int x, int y, String title) {
        int height = 146;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, title, TEXT);
        int chipY = y + 34;
        for (int index = 0; index < FLOOR_KEYS.length; index++) {
            Rect chip = floorChipRect(x, chipY, index);
            context.fill(chip.x, chip.y, chip.right(), chip.bottom(), index % 2 == 0 ? CARD_FOCUSED : BUTTON);
            drawText(context, chip.x + 10, chip.y + 8, FLOOR_LABELS[index], index % 2 == 0 ? TEXT : BUTTON_TEXT);
            drawText(context, chip.x + 10, chip.y + 22, formatTime(stats.fastestSPlus(FLOOR_KEYS[index])), HudStyle.accent());
        }
        return y + height;
    }

    private int drawDungeonSummary(DrawContext context, int x, int y) {
        int height = 112;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, "Dungeon Snapshot", TEXT);
        drawMetric(context, new Rect(x + 8, y + 34, 188, 48), "Cata", String.valueOf(stats.catacombsLevel()));
        drawMetric(context, new Rect(x + 206, y + 34, 188, 48), "Secrets", stats.secretsFound() <= 0 ? "--" : formatNumber(stats.secretsFound()));
        drawMetric(context, new Rect(x + 404, y + 34, 188, 48), "Runs", stats.totalCompletions() <= 0 ? "--" : formatNumber(stats.totalCompletions()));
        return y + height;
    }

    private int drawDungeonRow(DrawContext context, int x, int y, int index) {
        int height = 56;
        drawSettingCard(context, x, y, height, index % 2 == 0 ? HudStyle.selected() : HudStyle.border());
        drawText(context, x, y + 10, FLOOR_LABELS[index], TEXT);
        String time = formatTime(stats.fastestSPlus(FLOOR_KEYS[index]));
        drawText(context, x + 464, y + 10, time, HudStyle.accent());
        drawProgressBar(context, x, y + 30, 588, 8, timeProgress(stats.fastestSPlus(FLOOR_KEYS[index])), index % 2 == 0 ? HudStyle.accent() : HudStyle.selected(), "Run");
        return y + height;
    }

    private int drawLinkCard(DrawContext context, int x, int y, String title, String value, String description) {
        int rowHeight = cardHeight(description) + wrappedLines(value, 596).size() * 12;
        drawSettingCard(context, x, y, rowHeight, HudStyle.selected());
        drawText(context, x, y + 8, title, TEXT);
        drawWrappedText(context, x + 14, y + 22, value, 596, HudStyle.accent());
        drawWrappedText(context, x + 14, y + 22 + wrappedLines(value, 596).size() * 12 + 4, description, 596, MUTED);
        return y + rowHeight;
    }

    private int drawSkillSummary(DrawContext context, int x, int y) {
        int height = 92;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawMetric(context, new Rect(x + 8, y + 26, 188, 48), "SkyBlock Level", formatNumber(stats.skyblockLevel()));
        drawMetric(context, new Rect(x + 206, y + 26, 188, 48), "Avg Skill", String.format(Locale.ROOT, "%.2f", stats.averageSkillLevel()));
        drawMetric(context, new Rect(x + 404, y + 26, 188, 48), "Skills", formatNumber(stats.skillLevels().size()));
        return y + height;
    }

    private void drawSkillGrid(DrawContext context, int x, int y) {
        int index = 0;
        int baseY = y;
        for (Map.Entry<String, Integer> entry : stats.skillLevels().entrySet()) {
            Rect card = statGridRect(x, baseY, index);
            context.fill(card.x, card.y, card.right(), card.bottom(), index % 2 == 0 ? CARD : CARD_FOCUSED);
            drawText(context, card.x + 10, card.y + 8, pretty(entry.getKey()), TEXT);
            drawText(context, card.x + 10, card.y + 24, "Level " + entry.getValue(), HudStyle.accent());
            float progress = stats.skillProgress().getOrDefault(entry.getKey(), 0.0F);
            drawProgressBar(context, card.x + 10, card.y + 42, card.width - 20, 8, progress, HudStyle.selected(), "Progress");
            index++;
        }
    }

    private void drawSlayerGrid(DrawContext context, int x, int y) {
        int height = 34 + Math.max(1, stats.slayerLevels().size()) * 24;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, "Slayer Levels", TEXT);
        int rowY = y + 34;
        if (stats.slayerLevels().isEmpty()) {
            drawText(context, x + 14, rowY, "Keine Slayer Daten verfuegbar.", MUTED);
            return;
        }
        for (Map.Entry<String, Integer> entry : stats.slayerLevels().entrySet()) {
            drawText(context, x + 14, rowY, pretty(entry.getKey()), TEXT);
            drawText(context, x + 220, rowY, "Level " + entry.getValue(), HudStyle.accent());
            rowY += 24;
        }
    }

    private int drawWealthSummary(DrawContext context, int x, int y) {
        int height = 112;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawMetric(context, new Rect(x + 8, y + 34, 188, 48), "Networth", formatCoins(Math.round(stats.networth())));
        drawMetric(context, new Rect(x + 206, y + 34, 188, 48), "Purse", formatCoins(Math.round(stats.purse())));
        drawMetric(context, new Rect(x + 404, y + 34, 188, 48), "Bank", formatCoins(Math.round(stats.bank())));
        return y + height;
    }

    private void drawNetworthGrid(DrawContext context, int x, int y) {
        int index = 0;
        for (Map.Entry<String, Double> entry : stats.networthByType().entrySet()) {
            Rect card = statGridRect(x, y, index);
            context.fill(card.x, card.y, card.right(), card.bottom(), index % 2 == 0 ? CARD : CARD_FOCUSED);
            drawText(context, card.x + 10, card.y + 8, pretty(entry.getKey()), TEXT);
            drawText(context, card.x + 10, card.y + 24, formatCoins(Math.round(entry.getValue())), HudStyle.accent());
            index++;
        }
    }

    private int drawStorageIntro(DrawContext context, int x, int y) {
        int height = 78;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawWrappedText(context, x, y + 10, "Die oeffentlich verfuegbaren Daten liefern derzeit vor allem Storage- und Inventory-Kategorien ueber Networth-Typen. Itemgenaue Inhalte der einzelnen Container sind nicht in jedem Fall oeffentlich abrufbar.", 600, MUTED);
        return y + height;
    }

    private int drawStorageCategoryCards(DrawContext context, int x, int y) {
        int[] order = {0};
        String[] categories = {"inventory", "enderchest", "personal_vault", "wardrobe", "storage", "accessories", "pets"};
        for (String category : categories) {
            if (!stats.networthByType().containsKey(category)) {
                continue;
            }
            Rect card = statGridRect(x, y, order[0]++);
            context.fill(card.x, card.y, card.right(), card.bottom(), order[0] % 2 == 0 ? CARD : CARD_FOCUSED);
            drawText(context, card.x + 10, card.y + 8, pretty(category), TEXT);
            drawText(context, card.x + 10, card.y + 24, formatCoins(Math.round(stats.networthByType().get(category))), HudStyle.accent());
        }
        int rows = Math.max(1, (order[0] + 2) / 3);
        return y + rows * 96;
    }

    private void drawVisualFooter(DrawContext context, int x, int y, String title, String description) {
        int height = 64;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, title, TEXT);
        drawWrappedText(context, x + 14, y + 24, description, 596, MUTED);
    }

    private void drawMetric(DrawContext context, Rect rect, String label, String value) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), CARD_FOCUSED);
        drawText(context, rect.x + 10, rect.y + 8, label, MUTED);
        drawText(context, rect.x + 10, rect.y + 24, value, TEXT);
    }

    private void drawActionCard(DrawContext context, int x, int y, String title, String description) {
        int rowHeight = 76;
        drawSettingCard(context, x, y, rowHeight, HudStyle.selected());
        drawText(context, x, y + 8, title, TEXT);
        drawWrappedText(context, x + 14, y + 20, description, 596, MUTED);
        int buttonY = y + 42;
        drawAction(context, actionButtonRect(x, buttonY, 0), "SkyCrypt");
        drawAction(context, actionButtonRect(x, buttonY, 1), "NameMC");
        drawAction(context, actionButtonRect(x, buttonY, 2), "Copy /hv");
    }

    private void drawSettingCard(DrawContext context, int x, int y, int height, int markerColor) {
        int top = y;
        int bottom = y + height - 9;
        int left = x - 12;
        int right = x + 621;
        context.fill(left, top, right, bottom, CARD);
        context.fill(left, top, left + 3, bottom, markerColor);
    }

    private void drawAction(DrawContext context, Rect rect, String label) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), BUTTON);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), rect.centerX(), rect.y + 5, BUTTON_TEXT);
    }

    private void drawKeyValue(DrawContext context, int x, int y, String key, String value) {
        drawText(context, x, y, key, MUTED);
        drawText(context, x + 120, y, value, TEXT);
    }

    private void drawProgressBar(DrawContext context, int x, int y, int width, int height, float progress, int color, String label) {
        context.fill(x, y, x + width, y + height, BUTTON);
        int fillWidth = Math.max(0, Math.min(width, Math.round(width * progress)));
        context.fill(x, y, x + fillWidth, y + height, color);
        drawText(context, x, y - 10, label, MUTED);
    }

    private void drawScrollBar(DrawContext context, Rect viewport) {
        int totalHeight = contentHeight();
        if (totalHeight <= viewport.height) {
            return;
        }
        int barX = viewport.right() - 4;
        int trackTop = viewport.y;
        int trackBottom = viewport.bottom();
        context.fill(barX, trackTop, barX + 2, trackBottom, BUTTON);
        int thumbHeight = Math.max(22, Math.round((viewport.height / (float) totalHeight) * viewport.height));
        int maxThumbTravel = viewport.height - thumbHeight;
        int thumbY = trackTop + Math.round((contentScrollOffset / (float) (totalHeight - viewport.height)) * maxThumbTravel);
        context.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, HudStyle.accent());
    }

    private void drawWrappedText(DrawContext context, int x, int y, String text, int maxWidth, int color) {
        int lineY = y;
        for (String line : wrappedLines(text, maxWidth)) {
            drawText(context, x, lineY, line, color);
            lineY += 12;
        }
    }

    private List<String> wrappedLines(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textRenderer.getWidth(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private void drawText(DrawContext context, int x, int y, String text, int color) {
        context.drawTextWithShadow(textRenderer, Text.literal(text), x, y, color);
    }

    private int cardHeight(String description) {
        int titleBlock = 20;
        int descBlock = wrappedLines(description, 596).size() * 12;
        return titleBlock + descBlock + 18;
    }

    private int contentHeight() {
        if (stats == null) {
            return 140 + linkCardHeight("Lade Profil...", "Es werden oeffentliche und optionale API-Daten zusammengefuehrt.") + 64;
        }
        return switch (activeTab) {
            case OVERVIEW -> 140 + 188 + Math.max(60, 34 + Math.max(1, stats.profileNames().size()) * 22) + 64;
            case DUNGEONS -> 140 + 112 + FLOOR_KEYS.length * 56;
            case SKILLS -> 140 + 92 + Math.max(1, (stats.skillLevels().size() + 2) / 3) * 96;
            case SLAYERS -> 140 + linkCardHeight(formatCoins(stats.totalSlayerXp()), "Zusammengefasste Slayer Erfahrung.")
                + 34 + Math.max(1, stats.slayerLevels().size()) * 24;
            case WEALTH -> 140 + 112 + Math.max(1, (stats.networthByType().size() + 2) / 3) * 96;
            case STORAGE -> 140 + 78 + Math.max(1, 3) * 96
                + linkCardHeight(skyCryptUrl(stats.username()), "Fuer itemgenaue Inhalte, wenn der oeffentliche Item-Endpoint verfuegbar ist.")
                + linkCardHeight(nameMcUrl(stats.username()), "Name-Historie und UUID anschauen.")
                + linkCardHeight("/hv " + stats.username(), "Viewer fuer diesen Spieler erneut oeffnen.")
                + 76;
        };
    }

    private int linkCardHeight(String value, String description) {
        return cardHeight(description) + wrappedLines(value, 596).size() * 12;
    }

    private String displayRequestedPlayer() {
        return requestedPlayer == null || requestedPlayer.isBlank() ? "Spieler" : requestedPlayer;
    }

    private void openUrl(String url) {
        Util.getOperatingSystem().open(url);
    }

    private void copyCommand(String username) {
        if (client != null) {
            client.keyboard.setClipboard("/hv " + username);
        }
    }

    private String formatTime(double seconds) {
        if (seconds < 0.0D) {
            return "--:--";
        }
        int total = (int) Math.round(seconds);
        int minutes = total / 60;
        int secs = total % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, secs);
    }

    private String formatNumber(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private String displayProfileName() {
        return stats.selectedProfile() == null || stats.selectedProfile().isBlank() ? "Unbekannt" : stats.selectedProfile();
    }

    private String skyCryptUrl(String username) {
        return "https://sky.shiiyu.moe/stats/" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    private String nameMcUrl(String username) {
        return "https://namemc.com/profile/" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    private void drawWindowChrome(DrawContext context, Rect frame) {
        context.fill(frame.x, frame.y, frame.right(), frame.bottom(), WINDOW);
        context.fill(frame.x, frame.y, frame.right(), frame.y + 34, WINDOW_HEADER);
        context.fill(frame.x + 170, frame.y + 35, frame.right() - 1, frame.bottom() - 1, WINDOW);
        drawText(context, frame.x + 12, frame.y + 12, "HORIZON", HudStyle.accent());
        drawText(context, frame.x + 110, frame.y + 12, "Viewer: " + displayRequestedPlayer(), TEXT);
        drawText(context, closeRect(frame).x, closeRect(frame).y + 2, "[X]", WARNING);
    }

    private Rect frame() {
        int frameWidth = Math.min(960, width - 28);
        int frameHeight = Math.min(640, height - 28);
        return new Rect((width - frameWidth) / 2, (height - frameHeight) / 2, frameWidth, frameHeight);
    }

    private Rect sidebarRect(Rect frame) {
        return new Rect(frame.x + 12, frame.y + 40, 140, frame.height - 52);
    }

    private Rect contentViewportRect(Rect frame) {
        int left = sidebarRect(frame).right() + 18;
        int top = frame.y + 40;
        return new Rect(left, top, frame.right() - left - 12, frame.bottom() - top - 12);
    }

    private Rect contentClipRect(Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int left = viewport.x - 12;
        int right = frame.right() - 1;
        return new Rect(left, viewport.y, right - left, viewport.bottom() - viewport.y);
    }

    private Rect closeRect(Rect frame) {
        return new Rect(frame.right() - 24, frame.y + 12, 18, 18);
    }

    private Rect sidebarTabRect(Rect sidebar, int index) {
        return new Rect(sidebar.x, sidebar.y + index * 16, sidebar.width, 14);
    }

    private Rect metricRect(int x, int y, int index) {
        return new Rect(x + 14 + index * 198, y + 8, 184, 44);
    }

    private Rect actionButtonRect(int x, int y, int index) {
        return new Rect(x + index * 152, y, 136, 20);
    }

    private Rect floorChipRect(int x, int y, int index) {
        return new Rect(x + (index % 4) * 150, y + (index / 4) * 42, 138, 34);
    }

    private int linksActionY(Rect viewport) {
        return viewport.y - contentScrollOffset + 140
            + 78
            + Math.max(1, 3) * 96
            + linkCardHeight(skyCryptUrl(stats.username()), "Fuer itemgenaue Inhalte, wenn der oeffentliche Item-Endpoint verfuegbar ist.")
            + linkCardHeight(nameMcUrl(stats.username()), "Name-Historie und UUID anschauen.")
            + linkCardHeight("/hv " + stats.username(), "Viewer fuer diesen Spieler erneut oeffnen.")
            + 32;
    }

    private float statProgress(int value, int max) {
        if (max <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value / (float) max));
    }

    private float timeProgress(double seconds) {
        if (seconds <= 0.0D) {
            return 0.08F;
        }
        double normalized = 1.0D - Math.min(1.0D, seconds / 600.0D);
        return (float) Math.max(0.12D, normalized);
    }

    private String initials() {
        String name = stats == null ? displayRequestedPlayer() : stats.username();
        if (name == null || name.isBlank()) {
            return "HV";
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ROOT);
    }

    private String compactUuid() {
        String uuid = stats == null ? "" : stats.uuid();
        if (uuid == null || uuid.length() < 13) {
            return uuid;
        }
        return uuid.substring(0, 8) + "..." + uuid.substring(uuid.length() - 4);
    }

    private Rect statGridRect(int x, int y, int index) {
        return new Rect(x - 12 + (index % 3) * 206, y + (index / 3) * 96, 194, 84);
    }

    private String pretty(String key) {
        return key.replace('_', ' ').replace('-', ' ');
    }

    private String formatCoins(long value) {
        double abs = Math.abs((double) value);
        if (abs >= 1_000_000_000D) {
            return String.format(Locale.ROOT, "%.2fB", value / 1_000_000_000D);
        }
        if (abs >= 1_000_000D) {
            return String.format(Locale.ROOT, "%.2fM", value / 1_000_000D);
        }
        if (abs >= 1_000D) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000D);
        }
        return String.valueOf(value);
    }

    private enum ViewerTab {
        OVERVIEW("Overview"),
        DUNGEONS("Dungeons"),
        SKILLS("Skills"),
        SLAYERS("Slayers"),
        WEALTH("Networth"),
        STORAGE("Storage");

        private final String label;

        ViewerTab(String label) {
            this.label = label;
        }
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
