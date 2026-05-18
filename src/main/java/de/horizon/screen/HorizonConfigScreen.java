package de.horizon.screen;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;
import de.horizon.feature.chat.ChatCopyMode;
import de.horizon.feature.chat.SpamFilterOption;
import de.horizon.feature.dungeon.PuzzleSolverOption;
import de.horizon.feature.dungeon.TerminalSolverOption;
import de.horizon.feature.particle.ParticleFilterService;
import de.horizon.feature.revive.ReviveSource;
import de.horizon.hud.HudStyle;
import de.horizon.spotify.SpotifyService;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HorizonConfigScreen extends Screen {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB8B8B8;
    private static final int WARNING = 0xFFFFD27C;
    private static final int CONTENT_ROW_WIDTH = 620;
    private static final int CONTENT_CARD_WIDTH = CONTENT_ROW_WIDTH + 8;
    private static final int CARD_PADDING_TOP = 8;
    private static final int CARD_PADDING_BOTTOM = 8;
    private static final int CARD_GAP = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int DESCRIPTION_INDENT = 14;
    private static final int CONFIG_WINDOW = 0x66F0F1F3;
    private static final int CONFIG_WINDOW_HEADER = 0x73F7F8FA;
    private static final int CONFIG_CARD = 0x60F0F1F3;
    private static final int CONFIG_CARD_FOCUSED = 0x60F7F8FA;
    private static final int CONFIG_BUTTON = 0x60E6E8EC;
    private static final int CONFIG_BUTTON_TEXT = 0xFF1E2A37;
    private static final String[] HUD_COLOR_SWATCHES = {
        "#75E7CA", "#60A5FA", "#FBBF24", "#FB7185", "#F472B6", "#A78BFA",
        "#34D399", "#F97316", "#F87171", "#22D3EE", "#C4B5FD", "#E5E7EB"
    };

    private final Screen parent;
    private final HorizonClient horizonClient;
    private final SpotifyService spotifyService;
    private final ParticleFilterService particleFilterService;

    private Tab activeTab = Tab.HUD;
    private DungeonSection activeDungeonSection = DungeonSection.GENERAL;
    private InputFocus inputFocus = InputFocus.NONE;
    private String catacombsInput;
    private String hudAccentColorInput;
    private String spotifyClientIdInput;
    private String hypixelApiKeyInput;
    private String chatBridgeBotNameInput;
    private String globalSearchInput = "";
    private String particleSearchInput = "";
    private int contentScrollOffset = 0;
    private int particleScrollOffset = 0;

    public HorizonConfigScreen(Screen parent, HorizonClient horizonClient) {
        super(Text.literal("Horizon"));
        this.parent = parent;
        this.horizonClient = horizonClient;
        this.spotifyService = horizonClient.getSpotifyService();
        this.particleFilterService = horizonClient.getParticleFilterService();
        this.catacombsInput = String.valueOf(config().getCatacombsLevel());
        this.hudAccentColorInput = config().getHudAccentColor();
        this.spotifyClientIdInput = config().getSpotifyClientId();
        this.hypixelApiKeyInput = config().getHypixelApiKey();
        this.chatBridgeBotNameInput = config().getChatBridgeBotName();
    }

    @Override
    public void close() {
        commitInputs();
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

        Rect frame = frame();
        if (!frame.contains(click.x(), click.y())) {
            return super.mouseClicked(click, doubled);
        }

        if (closeRect(frame).contains(click.x(), click.y())) {
            close();
            return true;
        }
        if (searchRect(frame).contains(click.x(), click.y())) {
            inputFocus = InputFocus.GLOBAL_SEARCH;
            return true;
        }

        Rect sidebar = sidebarRect(frame);
        for (int index = 0; index < Tab.values().length; index++) {
            if (sidebarTabRect(sidebar, index).contains(click.x(), click.y())) {
                commitInputs();
                activeTab = Tab.values()[index];
                contentScrollOffset = 0;
                particleScrollOffset = 0;
                return true;
            }
        }

        if (activeTab == Tab.DUNGEON) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < DungeonSection.values().length; index++) {
                if (subTabRect(bar, index).contains(click.x(), click.y())) {
                    commitCatacombsInput();
                    activeDungeonSection = DungeonSection.values()[index];
                    contentScrollOffset = 0;
                    return true;
                }
            }
        }

        if (!globalSearchInput.isBlank()) {
            return handleSearchClick(click.x(), click.y(), contentViewportRect(frame)) || super.mouseClicked(click, doubled);
        }

        return switch (activeTab) {
            case HUD -> handleHudClick(click.x(), click.y(), frame);
            case DUNGEON -> handleDungeonClick(click.x(), click.y(), frame);
            case PARTICLE -> handleParticleClick(click.x(), click.y(), frame);
            case MISC -> handleMiscClick(click.x(), click.y(), frame);
            case ANTI_SPAM -> handleAntiSpamClick(click.x(), click.y(), frame);
            case CHAT -> handleChatClick(click.x(), click.y(), frame);
        } || super.mouseClicked(click, doubled);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (inputFocus == InputFocus.CATACOMBS_LEVEL && Character.isDigit(input.codepoint())) {
            if ("0".equals(catacombsInput)) {
                catacombsInput = "";
            }
            if (catacombsInput.length() < 2) {
                catacombsInput += Character.toString(input.codepoint());
            }
            return true;
        }
        if (inputFocus == InputFocus.HUD_ACCENT_COLOR) {
            if (isAllowedHudColorChar(input.codepoint()) && hudAccentColorInput.length() < 7) {
                if (hudAccentColorInput.isEmpty() && input.codepoint() != '#') {
                    hudAccentColorInput = "#";
                }
                if (!(input.codepoint() == '#' && hudAccentColorInput.contains("#"))) {
                    hudAccentColorInput += Character.toString(Character.toUpperCase(input.codepoint()));
                }
            }
            return true;
        }
        if (inputFocus == InputFocus.SPOTIFY_CLIENT_ID) {
            if (isAllowedSpotifyClientChar(input.codepoint()) && spotifyClientIdInput.length() < 64) {
                spotifyClientIdInput += Character.toString(input.codepoint());
            }
            return true;
        }
        if (inputFocus == InputFocus.HYPIXEL_API_KEY) {
            if (isAllowedApiKeyChar(input.codepoint()) && hypixelApiKeyInput.length() < 64) {
                hypixelApiKeyInput += Character.toString(input.codepoint());
            }
            return true;
        }
        if (inputFocus == InputFocus.CHAT_BRIDGE_BOT_NAME) {
            if (!Character.isISOControl(input.codepoint()) && chatBridgeBotNameInput.length() < 48) {
                chatBridgeBotNameInput += Character.toString(input.codepoint());
            }
            return true;
        }
        if (inputFocus == InputFocus.GLOBAL_SEARCH) {
            if (!Character.isISOControl(input.codepoint()) && globalSearchInput.length() < 48) {
                globalSearchInput += Character.toString(input.codepoint());
            }
            return true;
        }
        if (inputFocus == InputFocus.PARTICLE_SEARCH) {
            if (!Character.isISOControl(input.codepoint()) && particleSearchInput.length() < 48) {
                particleSearchInput += Character.toString(input.codepoint());
                particleScrollOffset = 0;
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (inputFocus != InputFocus.NONE) {
            boolean controlDown = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
            if (controlDown) {
                if (input.key() == GLFW.GLFW_KEY_V) {
                    pasteIntoFocusedField();
                    return true;
                }
                if (input.key() == GLFW.GLFW_KEY_C) {
                    copyFocusedField();
                    return true;
                }
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                commitInputs();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                inputFocus = InputFocus.NONE;
                refreshInputs();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
                handleBackspace();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Rect viewport = contentViewportRect(frame());
        if (!viewport.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (activeTab == Tab.PARTICLE) {
            int maxScroll = maxParticleScroll();
            particleScrollOffset = Math.max(0, Math.min(maxScroll, particleScrollOffset - (int) Math.round(verticalAmount * 24.0D)));
            return true;
        }
        int maxScroll = maxContentScroll();
        contentScrollOffset = Math.max(0, Math.min(maxScroll, contentScrollOffset - (int) Math.round(verticalAmount * 24.0D)));
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Rect frame = frame();
        Rect sidebar = sidebarRect(frame);
        Rect viewport = contentViewportRect(frame);
        Rect contentClip = contentClipRect(frame);
        int accent = accentColor();

        drawWindowChrome(context, frame, viewport, accent);

        for (int index = 0; index < Tab.values().length; index++) {
            boolean active = Tab.values()[index] == activeTab;
            Rect rect = sidebarTabRect(sidebar, index);
            drawTextLine(context, rect.x, rect.y, (active ? "> " : "  ") + Tab.values()[index].label, active ? accent : TEXT);
        }

        if (activeTab == Tab.DUNGEON) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < DungeonSection.values().length; index++) {
                boolean active = DungeonSection.values()[index] == activeDungeonSection;
                Rect rect = subTabRect(bar, index);
                drawTextLine(context, rect.x, rect.y, (active ? "[" : "") + DungeonSection.values()[index].label + (active ? "]" : ""), active ? accent : TEXT);
            }
        }

        context.enableScissor(contentClip.x, contentClip.y, contentClip.right(), contentClip.bottom());
        if (!globalSearchInput.isBlank()) {
            renderSearchResults(context, viewport);
        } else {
            switch (activeTab) {
                case HUD -> renderHudText(context, viewport);
                case DUNGEON -> renderDungeonText(context, viewport);
                case PARTICLE -> renderParticleText(context, viewport);
                case MISC -> renderMiscText(context, viewport);
                case ANTI_SPAM -> renderAntiSpamText(context, viewport);
                case CHAT -> renderChatText(context, viewport);
            }
        }
        context.disableScissor();
        drawHeaderMask(context, frame, accent);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHudText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "HUD");
        y = drawToggleRow(context, viewport.x, y, "Revive HUD", config().isReviveHudEnabled(), "Spirit, Bonzo und Phoenix als Status-Panel.");
        y = drawActionRow(context, viewport.x, y, "HUD bearbeiten", "HUD reset", "Layout bearbeiten oder Positionen zuruecksetzen.");
        y = drawHudColorRow(context, viewport.x, y);
        y = drawFieldRow(context, viewport.x, y, "Spotify Client ID", spotifyClientIdInput, inputFocus == InputFocus.SPOTIFY_CLIENT_ID, "Spotify Premium Login.");
        y = drawActionRow(context, viewport.x, y, "Spotify Login", "Spotify Logout", spotifyService.auth().getStatusMessage());
        y = drawToggleRow(context, viewport.x, y, "Spotify Inventarsteuerung", config().isSpotifyInventoryControlsEnabled(), "Steuerung im Inventar ein- oder ausschalten.");
        drawFieldRow(context, viewport.x, y, "Hypixel API Key", hypixelApiKeyInput, inputFocus == InputFocus.HYPIXEL_API_KEY, "Fuer Profil- und Party-Finder-Daten.");
    }

    private void renderDungeonText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeDungeonSection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Dungeons / General");
                y = drawToggleRow(context, viewport.x, y, "Party Finder Overlay", config().isDungeonPartyFinderOverlayEnabled(), "Zeigt beste S+ Zeiten im Party Finder.");
                drawToggleRow(context, viewport.x, y, "Rare Room Alerts", config().isDungeonRareRoomAlertsEnabled(), "Alert fuer Trinity, Tomioka und Duncan.");
            }
            case REVIVAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Dungeons / Revive");
                y = drawNumberRow(context, viewport.x, y, "Catacombs Level", catacombsInput, inputFocus == InputFocus.CATACOMBS_LEVEL, "Nutze [-] und [+] oder tippe direkt.");
                y = drawToggleRow(context, viewport.x, y, "Boss Only", config().isReviveHudOnlyInBoss(), "Nur waehrend Bossphasen.");
                y = drawToggleRow(context, viewport.x, y, "Always Visible", config().isReviveHudAlwaysVisible(), "Auch ausserhalb des Kampfes sichtbar.");
                for (ReviveSource source : ReviveSource.values()) {
                    y = drawToggleRow(context, viewport.x, y, source.displayName(), source.isEnabled(config()), source.cooldownLabel() + ": " + source.configuredCooldown(config()) + "s");
                }
            }
            case TERMINAL_SOLVER -> {
                y = drawSectionTitle(context, viewport.x, y, "Dungeons / Terminal Solver");
                for (TerminalSolverOption option : TerminalSolverOption.values()) {
                    y = drawToggleRow(context, viewport.x, y, option.title(), option.isEnabled(config()), option.description());
                }
            }
            case PUZZLE_SOLVER -> {
                y = drawSectionTitle(context, viewport.x, y, "Dungeons / Puzzle Solver");
                for (PuzzleSolverOption option : PuzzleSolverOption.values()) {
                    y = drawToggleRow(context, viewport.x, y, option.title(), option.isEnabled(config()), option.description());
                }
            }
        }
    }

    private void renderParticleText(DrawContext context, Rect viewport) {
        int y = viewport.y;
        drawFieldRow(context, viewport.x, y, "Particle Suche", particleSearchInput, inputFocus == InputFocus.PARTICLE_SEARCH, "Liste filtern.");
        y += fieldRowHeight("Liste filtern.");
        int baseY = y - particleScrollOffset;
        List<String> particles = filteredParticleIds();
        for (String particleId : particles) {
            String name = particleFilterService.displayName(particleId);
            boolean enabled = particleFilterService.isEnabled(particleId);
            drawTextLine(context, viewport.x, baseY, "[" + (enabled ? "AN" : "AUS") + "] " + name + " - " + particleId, enabled ? TEXT : MUTED);
            baseY += 14;
        }
    }

    private void renderMiscText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Misc");
        y = drawToggleRow(context, viewport.x, y, "Zeit HUD", config().isTimeHudEnabled(), "Lokale Uhrzeit als Overlay.");
        y = drawToggleRow(context, viewport.x, y, "FPS / TPS / Ping", config().isPerformanceHudEnabled(), "Performance-Overlay.");
        y = drawToggleRow(context, viewport.x, y, "System HUD", config().isSystemHudEnabled(), "CPU / GPU / Temperaturen.");
        y = drawToggleRow(context, viewport.x, y, "Solver Debug HUD", config().isSolverDebugHudEnabled(), "Diagnoseanzeige fuer Dungeon Solver.");
        y = drawToggleRow(context, viewport.x, y, "Defense Bar", config().isHideDefenseBar(), "Blendet die Vanilla-Ruestungsanzeige aus.");
        drawToggleRow(context, viewport.x, y, "Kompakte Herzen", config().isCompactHypixelHealthEnabled(), "Fasst Hypixel-Herzen kompakt in einer Reihe zusammen.");
    }

    private void renderChatText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Chat");
        y = drawToggleRow(context, viewport.x, y, "Bridge verstecken", config().isChatBridgeHidden(), "Discord-Bridge-Nachrichten im Guild-Chat ausblenden.");
        y = drawFieldRow(context, viewport.x, y, "Bridge Bot Name", chatBridgeBotNameInput, inputFocus == InputFocus.CHAT_BRIDGE_BOT_NAME, "Ingame-Name des Discord-Bridge-Bots (z.B. catgirlfc).");
        ChatCopyMode copyMode = config().getChatCopyMode();
        y = drawCycleRow(context, viewport.x, y, "Nachrichten kopieren", copyMode.label(), copyMode != ChatCopyMode.OFF, "Modus: Aus, Strg+LK, Rechtsklick oder Beides.");
        drawToggleRow(context, viewport.x + 16, y, "Gesamte Nachricht", config().isChatCopyFullMessage(), "Alle Zeilen des Eintrags oder nur die angeklickte Zeile.");
    }

    private void renderAntiSpamText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Anti Spam");
        y = drawToggleRow(context, viewport.x, y, "Anti Spam Gesamt", config().isAntiSpamEnabled(), "Reduziert Dungeon- und Ability-Noise.");
        for (SpamFilterOption option : SpamFilterOption.values()) {
            y = drawToggleRow(context, viewport.x, y, option.title(), option.isEnabled(config()), option.description());
        }
    }

    private void renderSearchResults(DrawContext context, Rect viewport) {
        int y = viewport.y;
        List<SearchResult> results = searchResults();
        for (int index = 0; index < Math.min(12, results.size()); index++) {
            SearchResult result = results.get(index);
            drawTextLine(context, viewport.x, y, result.title(), accentColor());
            drawTextLine(context, viewport.x + 12, y + 12, result.location(), MUTED);
            y += 28;
        }
        if (results.isEmpty()) {
            drawTextLine(context, viewport.x, y, "Keine Treffer.", MUTED);
        }
    }

    private int drawSectionTitle(DrawContext context, int x, int y, String title) {
        drawTextLine(context, x, y, title, accentColor());
        context.fill(x, y + 14, x + CONTENT_ROW_WIDTH, y + 15, HudStyle.border());
        return y + 24;
    }

    private int drawToggleRow(DrawContext context, int x, int y, String title, boolean enabled, String description) {
        int rowHeight = toggleRowHeight(description);
        drawSettingCard(context, x, y, rowHeight, enabled ? 0xFF2DBA68 : 0xFF8A97A8, false);
        Rect badge = toggleBadgeRect(x, y);
        context.fill(badge.x, badge.y, badge.right(), badge.bottom(), enabled ? 0xFF2DBA68 : 0xFF667487);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(enabled ? "AN" : "AUS"), badge.centerX(), badge.y + 4, 0xFFF7FBFF);
        int contentX = badge.right() + 10;
        int contentWidth = Math.max(80, CONTENT_ROW_WIDTH - (contentX - x) - 10);
        drawTextLine(context, contentX, y + CARD_PADDING_TOP, title, TEXT);
        drawWrappedText(context, contentX, y + CARD_PADDING_TOP + LINE_HEIGHT, description, contentWidth, MUTED);
        return y + rowHeight;
    }

    private int drawCycleRow(DrawContext context, int x, int y, String title, String modeLabel, boolean active, String description) {
        int rowHeight = toggleRowHeight(description);
        drawSettingCard(context, x, y, rowHeight, active ? 0xFF2DBA68 : 0xFF8A97A8, false);
        Rect badge = cycleBadgeRect(x, y);
        context.fill(badge.x, badge.y, badge.right(), badge.bottom(), active ? 0xFF2DBA68 : 0xFF667487);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(modeLabel), badge.centerX(), badge.y + 4, 0xFFF7FBFF);
        int contentX = badge.right() + 10;
        int contentWidth = Math.max(80, CONTENT_ROW_WIDTH - (contentX - x) - 10);
        drawTextLine(context, contentX, y + CARD_PADDING_TOP, title, TEXT);
        drawWrappedText(context, contentX, y + CARD_PADDING_TOP + LINE_HEIGHT, description, contentWidth, MUTED);
        return y + rowHeight;
    }

    private Rect cycleBadgeRect(int x, int y) {
        return new Rect(x, y + CARD_PADDING_TOP - 1, 54, 18);
    }

    private int drawActionRow(DrawContext context, int x, int y, String left, String right, String description) {
        int rowHeight = actionRowHeight(description);
        drawSettingCard(context, x, y, rowHeight, HudStyle.selected(), false);
        Rect leftRect = actionButtonRect(x, y, true);
        Rect rightRect = actionButtonRect(x, y, false);
        drawInlineAction(context, leftRect, left);
        drawInlineAction(context, rightRect, right);
        drawWrappedText(context, x + DESCRIPTION_INDENT, leftRect.bottom() + 6, description, CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10, MUTED);
        return y + rowHeight;
    }

    private int drawFieldRow(DrawContext context, int x, int y, String title, String value, boolean focused, String description) {
        int rowHeight = fieldRowHeight(description);
        drawSettingCard(context, x, y, rowHeight, focused ? HudStyle.accent() : HudStyle.border(), focused);
        drawTextLine(context, x, y + CARD_PADDING_TOP, title + ": " + fieldValue(value, focused), TEXT);
        drawWrappedText(context, x + DESCRIPTION_INDENT, y + CARD_PADDING_TOP + LINE_HEIGHT, description, CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10, MUTED);
        return y + rowHeight;
    }

    private int drawHudColorRow(DrawContext context, int x, int y) {
        int rowHeight = hudColorRowHeight();
        drawSettingCard(context, x, y, rowHeight, inputFocus == InputFocus.HUD_ACCENT_COLOR ? HudStyle.accent() : HudStyle.selected(), inputFocus == InputFocus.HUD_ACCENT_COLOR);
        drawTextLine(context, x, y + CARD_PADDING_TOP, "HUD Farbe: " + fieldValue(hudAccentColorInput, inputFocus == InputFocus.HUD_ACCENT_COLOR), TEXT);
        drawWrappedText(context, x + DESCRIPTION_INDENT, y + CARD_PADDING_TOP + LINE_HEIGHT, "Preview und Palette. Hexwert bleibt weiter editierbar.", CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10, MUTED);

        int previewY = y + CARD_PADDING_TOP + LINE_HEIGHT + wrappedLines("Preview und Palette. Hexwert bleibt weiter editierbar.", CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10).size() * LINE_HEIGHT + 6;
        Rect preview = hudColorPreviewRect(x, previewY);
        context.fill(preview.x, preview.y, preview.right(), preview.bottom(), parsePreviewColor());
        drawTextLine(context, preview.right() + 10, preview.y + 6, HudStyle.isCompleteHex(hudAccentColorInput) ? "Aktive HUD-Farbe" : "Ungueltig -> Default", MUTED);

        for (int index = 0; index < HUD_COLOR_SWATCHES.length; index++) {
            Rect swatch = hudColorSwatchRect(x, previewY, index);
            int color = 0xFF000000 | Integer.parseInt(HUD_COLOR_SWATCHES[index].substring(1), 16);
            context.fill(swatch.x, swatch.y, swatch.right(), swatch.bottom(), color);
        }
        return y + rowHeight;
    }

    private int drawNumberRow(DrawContext context, int x, int y, String title, String value, boolean focused, String description) {
        int rowHeight = numberRowHeight(description);
        drawSettingCard(context, x, y, rowHeight, focused ? HudStyle.accent() : HudStyle.border(), focused);
        Rect minusRect = cataButtonRect(x, y, true);
        Rect plusRect = cataButtonRect(x, y, false);
        drawInlineAction(context, minusRect, "-");
        drawInlineAction(context, plusRect, "+");
        drawTextLine(context, x, y + CARD_PADDING_TOP, title + ": " + fieldValue(value, focused), TEXT);
        drawWrappedText(context, x + DESCRIPTION_INDENT, y + CARD_PADDING_TOP + LINE_HEIGHT, description, CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10, MUTED);
        return y + rowHeight;
    }

    private void drawSettingCard(DrawContext context, int x, int y, int height, int markerColor, boolean focused) {
        int top = y;
        int bottom = y + height - CARD_GAP + 1;
        int left = x - 12;
        int right = x + CONTENT_ROW_WIDTH + 1;
        context.fill(left, top, right, bottom, focused ? CONFIG_CARD_FOCUSED : CONFIG_CARD);
        context.fill(left, top, left + 3, bottom, markerColor);
    }

    private void drawTextLine(DrawContext context, int x, int y, String text, int color) {
        context.drawTextWithShadow(textRenderer, Text.literal(text), x, y, color);
    }

    private String fieldValue(String value, boolean focused) {
        String display = value == null || value.isBlank() ? "<leer>" : value;
        return focused ? display + ((System.currentTimeMillis() / 400L) % 2L == 0L ? "_" : "") : display;
    }

    private boolean handleHudClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setReviveHudEnabled(!config().isReviveHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Spirit, Bonzo und Phoenix als Status-Panel.");
        if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
            client.setScreen(new HudLayoutScreen(this, horizonClient));
            return true;
        }
        if (actionButtonRect(viewport.x, y, false).contains(mouseX, mouseY)) {
            horizonClient.getConfigManager().resetPosition("revive_status", 20, 20);
            return true;
        }
        y += actionRowHeight("Layout bearbeiten oder Positionen zuruecksetzen.");
        Rect colorArea = rowRect(viewport.x, y, hudColorRowHeight());
        if (colorArea.contains(mouseX, mouseY)) {
            int previewY = hudColorPreviewY(y);
            for (int index = 0; index < HUD_COLOR_SWATCHES.length; index++) {
                Rect swatch = hudColorSwatchRect(viewport.x, previewY, index);
                if (swatch.contains(mouseX, mouseY)) {
                    config().setHudAccentColor(HUD_COLOR_SWATCHES[index]);
                    refreshHudAccentColorInput();
                    inputFocus = InputFocus.NONE;
                    horizonClient.getConfigManager().save();
                    return true;
                }
            }
            inputFocus = InputFocus.HUD_ACCENT_COLOR;
            return true;
        }
        y += hudColorRowHeight();
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            inputFocus = InputFocus.SPOTIFY_CLIENT_ID;
            return true;
        }
        y += fieldRowHeight("Spotify Premium Login.");
        if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
            commitSpotifyClientIdInput();
            spotifyService.auth().beginLogin();
            return true;
        }
        if (actionButtonRect(viewport.x, y, false).contains(mouseX, mouseY)) {
            spotifyService.auth().disconnect();
            return true;
        }
        y += actionRowHeight(spotifyService.auth().getStatusMessage());
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setSpotifyInventoryControlsEnabled(!config().isSpotifyInventoryControlsEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Steuerung im Inventar ein- oder ausschalten.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            inputFocus = InputFocus.HYPIXEL_API_KEY;
            return true;
        }
        return false;
    }

    private boolean handleDungeonClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        return switch (activeDungeonSection) {
            case GENERAL -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDungeonPartyFinderOverlayEnabled(!config().isDungeonPartyFinderOverlayEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight("Zeigt beste S+ Zeiten im Party Finder.");
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDungeonRareRoomAlertsEnabled(!config().isDungeonRareRoomAlertsEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                yield false;
            }
            case REVIVAL -> handleReviveClick(mouseX, mouseY, viewport, y);
            case TERMINAL_SOLVER -> handleTerminalRows(mouseX, mouseY, viewport, y);
            case PUZZLE_SOLVER -> handlePuzzleRows(mouseX, mouseY, viewport, y);
        };
    }

    private boolean handleReviveClick(double mouseX, double mouseY, Rect viewport, int y) {
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            inputFocus = InputFocus.CATACOMBS_LEVEL;
            refreshCatacombsInput();
            return true;
        }
        if (cataButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
            adjustCatacombsLevel(-1);
            return true;
        }
        if (cataButtonRect(viewport.x, y, false).contains(mouseX, mouseY)) {
            adjustCatacombsLevel(1);
            return true;
        }
        y += numberRowHeight("Nutze [-] und [+] oder tippe direkt.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setReviveHudOnlyInBoss(!config().isReviveHudOnlyInBoss());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Nur waehrend Bossphasen.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setReviveHudAlwaysVisible(!config().isReviveHudAlwaysVisible());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Auch ausserhalb des Kampfes sichtbar.");
        for (ReviveSource source : ReviveSource.values()) {
            if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                source.toggle(config());
                horizonClient.getConfigManager().save();
                return true;
            }
            y += toggleRowHeight(source.cooldownLabel() + ": " + source.configuredCooldown(config()) + "s");
        }
        return false;
    }

    private boolean handleTerminalRows(double mouseX, double mouseY, Rect viewport, int y) {
        for (TerminalSolverOption option : TerminalSolverOption.values()) {
            if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                option.toggle(config());
                horizonClient.getConfigManager().save();
                return true;
            }
            y += toggleRowHeight(option.description());
        }
        return false;
    }

    private boolean handlePuzzleRows(double mouseX, double mouseY, Rect viewport, int y) {
        for (PuzzleSolverOption option : PuzzleSolverOption.values()) {
            if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                option.toggle(config());
                horizonClient.getConfigManager().save();
                return true;
            }
            y += toggleRowHeight(option.description());
        }
        return false;
    }

    private boolean handleParticleClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        if (rowRect(viewport.x, viewport.y).contains(mouseX, mouseY)) {
            inputFocus = InputFocus.PARTICLE_SEARCH;
            return true;
        }
        int y = viewport.y + fieldRowHeight("Liste filtern.") - particleScrollOffset;
        for (String particleId : filteredParticleIds()) {
            if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                particleFilterService.toggle(particleId);
                return true;
            }
            y += 14;
        }
        return false;
    }

    private boolean handleMiscClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setTimeHudEnabled(!config().isTimeHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Lokale Uhrzeit als Overlay.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setPerformanceHudEnabled(!config().isPerformanceHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Performance-Overlay.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setSystemHudEnabled(!config().isSystemHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("CPU / GPU / Temperaturen.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setSolverDebugHudEnabled(!config().isSolverDebugHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Diagnoseanzeige fuer Dungeon Solver.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setHideDefenseBar(!config().isHideDefenseBar());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Blendet die Vanilla-Ruestungsanzeige aus.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setCompactHypixelHealthEnabled(!config().isCompactHypixelHealthEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        return false;
    }

    private boolean handleChatClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setChatBridgeHidden(!config().isChatBridgeHidden());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Discord-Bridge-Nachrichten im Guild-Chat ausblenden.");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            inputFocus = InputFocus.CHAT_BRIDGE_BOT_NAME;
            return true;
        }
        y += fieldRowHeight("Ingame-Name des Discord-Bridge-Bots (z.B. catgirlfc).");
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            ChatCopyMode[] modes = ChatCopyMode.values();
            int next = (config().getChatCopyMode().ordinal() + 1) % modes.length;
            config().setChatCopyMode(modes[next]);
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Modus: Aus, Strg+LK, Rechtsklick oder Beides.");
        if (rowRect(viewport.x + 16, y).contains(mouseX, mouseY)) {
            config().setChatCopyFullMessage(!config().isChatCopyFullMessage());
            horizonClient.getConfigManager().save();
            return true;
        }
        return false;
    }

    private boolean handleAntiSpamClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setAntiSpamEnabled(!config().isAntiSpamEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight("Reduziert Dungeon- und Ability-Noise.");
        for (SpamFilterOption option : SpamFilterOption.values()) {
            if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                option.toggle(config());
                horizonClient.getConfigManager().save();
                return true;
            }
            y += toggleRowHeight(option.description());
        }
        return false;
    }

    private boolean handleSearchClick(double mouseX, double mouseY, Rect viewport) {
        int y = viewport.y;
        List<SearchResult> results = searchResults();
        for (int index = 0; index < Math.min(12, results.size()); index++) {
            Rect row = new Rect(viewport.x, y, viewport.width, 24);
            if (row.contains(mouseX, mouseY)) {
                SearchResult result = results.get(index);
                activeTab = result.tab();
                if (result.section() != null) {
                    activeDungeonSection = result.section();
                }
                globalSearchInput = "";
                inputFocus = InputFocus.NONE;
                contentScrollOffset = 0;
                return true;
            }
            y += 28;
        }
        return false;
    }

    private Rect rowRect(int x, int y) {
        return rowRect(x, y, 34);
    }

    private Rect rowRect(int x, int y, int height) {
        return new Rect(x - 12, y, CONTENT_CARD_WIDTH + 4, Math.max(24, height - CARD_GAP));
    }

    private Rect leftTokenRect(int x, int y) {
        return new Rect(x, y, 140, 24);
    }

    private Rect rightTokenRect(int x, int y) {
        return new Rect(x + 150, y, 160, 24);
    }

    private Rect rightPlusRect(int x, int y) {
        return new Rect(x + 190, y, 30, 24);
    }

    private HorizonConfig config() {
        return horizonClient.getConfigManager().getConfig();
    }

    private void refreshChatBridgeBotNameInput() {
        chatBridgeBotNameInput = config().getChatBridgeBotName();
    }

    private void commitChatBridgeBotNameInput() {
        if (inputFocus != InputFocus.CHAT_BRIDGE_BOT_NAME) {
            return;
        }
        config().setChatBridgeBotName(chatBridgeBotNameInput);
        refreshChatBridgeBotNameInput();
        inputFocus = InputFocus.NONE;
        horizonClient.getConfigManager().save();
    }

    private void refreshInputs() {
        refreshCatacombsInput();
        refreshHudAccentColorInput();
        refreshSpotifyClientIdInput();
        refreshHypixelApiKeyInput();
        refreshChatBridgeBotNameInput();
        if (inputFocus == InputFocus.GLOBAL_SEARCH) {
            globalSearchInput = "";
        }
        if (inputFocus == InputFocus.PARTICLE_SEARCH) {
            particleSearchInput = "";
            particleScrollOffset = 0;
        }
    }

    private boolean isAllowedSpotifyClientChar(int codepoint) {
        return Character.isLetterOrDigit(codepoint) || codepoint == '-' || codepoint == '_';
    }

    private boolean isAllowedHudColorChar(int codepoint) {
        return codepoint == '#'
            || (codepoint >= '0' && codepoint <= '9')
            || (codepoint >= 'a' && codepoint <= 'f')
            || (codepoint >= 'A' && codepoint <= 'F');
    }

    private boolean isAllowedApiKeyChar(int codepoint) {
        return Character.isLetterOrDigit(codepoint) || codepoint == '-';
    }

    private void refreshCatacombsInput() {
        catacombsInput = String.valueOf(config().getCatacombsLevel());
    }

    private void refreshHudAccentColorInput() {
        hudAccentColorInput = config().getHudAccentColor();
    }

    private void refreshSpotifyClientIdInput() {
        spotifyClientIdInput = config().getSpotifyClientId();
    }

    private void refreshHypixelApiKeyInput() {
        hypixelApiKeyInput = config().getHypixelApiKey();
    }

    private void commitInputs() {
        commitCatacombsInput();
        commitHudAccentColorInput();
        commitSpotifyClientIdInput();
        commitHypixelApiKeyInput();
        commitChatBridgeBotNameInput();
    }

    private void commitCatacombsInput() {
        if (inputFocus != InputFocus.CATACOMBS_LEVEL) {
            return;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(catacombsInput.isBlank() ? "0" : catacombsInput);
        } catch (NumberFormatException ignored) {
            parsed = config().getCatacombsLevel();
        }
        config().setCatacombsLevel(parsed);
        refreshCatacombsInput();
        inputFocus = InputFocus.NONE;
        horizonClient.getConfigManager().save();
    }

    private void commitSpotifyClientIdInput() {
        if (inputFocus != InputFocus.SPOTIFY_CLIENT_ID) {
            return;
        }
        config().setSpotifyClientId(spotifyClientIdInput);
        refreshSpotifyClientIdInput();
        inputFocus = InputFocus.NONE;
        horizonClient.getConfigManager().save();
    }

    private void commitHudAccentColorInput() {
        if (inputFocus != InputFocus.HUD_ACCENT_COLOR) {
            return;
        }
        config().setHudAccentColor(hudAccentColorInput);
        refreshHudAccentColorInput();
        inputFocus = InputFocus.NONE;
        horizonClient.getConfigManager().save();
    }

    private void commitHypixelApiKeyInput() {
        if (inputFocus != InputFocus.HYPIXEL_API_KEY) {
            return;
        }
        config().setHypixelApiKey(hypixelApiKeyInput);
        refreshHypixelApiKeyInput();
        inputFocus = InputFocus.NONE;
        horizonClient.getConfigManager().save();
    }

    private void pasteIntoFocusedField() {
        if (client == null) {
            return;
        }
        String clipboard = client.keyboard.getClipboard();
        if (clipboard == null) {
            return;
        }
        switch (inputFocus) {
            case CATACOMBS_LEVEL -> catacombsInput = sanitizeClipboard(clipboard, true);
            case HUD_ACCENT_COLOR -> hudAccentColorInput = HudStyle.sanitizeHex(clipboard);
            case SPOTIFY_CLIENT_ID -> spotifyClientIdInput = sanitizeClipboard(clipboard, false);
            case HYPIXEL_API_KEY -> hypixelApiKeyInput = sanitizeClipboard(clipboard, false);
            case CHAT_BRIDGE_BOT_NAME -> chatBridgeBotNameInput = sanitizeClipboard(clipboard, false);
            case GLOBAL_SEARCH -> globalSearchInput = sanitizeClipboard(clipboard, false);
            case PARTICLE_SEARCH -> {
                particleSearchInput = sanitizeClipboard(clipboard, false);
                particleScrollOffset = 0;
            }
            case NONE -> {
            }
        }
    }

    private void copyFocusedField() {
        if (client == null) {
            return;
        }
        client.keyboard.setClipboard(switch (inputFocus) {
            case CATACOMBS_LEVEL -> catacombsInput;
            case HUD_ACCENT_COLOR -> hudAccentColorInput;
            case SPOTIFY_CLIENT_ID -> spotifyClientIdInput;
            case HYPIXEL_API_KEY -> hypixelApiKeyInput;
            case CHAT_BRIDGE_BOT_NAME -> chatBridgeBotNameInput;
            case GLOBAL_SEARCH -> globalSearchInput;
            case PARTICLE_SEARCH -> particleSearchInput;
            case NONE -> "";
        });
    }

    private void handleBackspace() {
        switch (inputFocus) {
            case CATACOMBS_LEVEL -> {
                if (!catacombsInput.isEmpty()) {
                    catacombsInput = catacombsInput.substring(0, catacombsInput.length() - 1);
                }
                if (catacombsInput.isEmpty()) {
                    catacombsInput = "0";
                }
            }
            case HUD_ACCENT_COLOR -> {
                if (!hudAccentColorInput.isEmpty()) {
                    hudAccentColorInput = hudAccentColorInput.substring(0, hudAccentColorInput.length() - 1);
                }
            }
            case SPOTIFY_CLIENT_ID -> {
                if (!spotifyClientIdInput.isEmpty()) {
                    spotifyClientIdInput = spotifyClientIdInput.substring(0, spotifyClientIdInput.length() - 1);
                }
            }
            case HYPIXEL_API_KEY -> {
                if (!hypixelApiKeyInput.isEmpty()) {
                    hypixelApiKeyInput = hypixelApiKeyInput.substring(0, hypixelApiKeyInput.length() - 1);
                }
            }
            case CHAT_BRIDGE_BOT_NAME -> {
                if (!chatBridgeBotNameInput.isEmpty()) {
                    chatBridgeBotNameInput = chatBridgeBotNameInput.substring(0, chatBridgeBotNameInput.length() - 1);
                }
            }
            case GLOBAL_SEARCH -> {
                if (!globalSearchInput.isEmpty()) {
                    globalSearchInput = globalSearchInput.substring(0, globalSearchInput.length() - 1);
                }
            }
            case PARTICLE_SEARCH -> {
                if (!particleSearchInput.isEmpty()) {
                    particleSearchInput = particleSearchInput.substring(0, particleSearchInput.length() - 1);
                    particleScrollOffset = 0;
                }
            }
            case NONE -> {
            }
        }
    }

    private String sanitizeClipboard(String value, boolean digitsOnly) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (digitsOnly) {
                if (Character.isDigit(current) && builder.length() < 2) {
                    builder.append(current);
                }
            } else if (!Character.isISOControl(current) && builder.length() < 64) {
                builder.append(current);
            }
        }
        String sanitized = builder.toString().trim();
        return digitsOnly ? (sanitized.isEmpty() ? "0" : sanitized) : sanitized;
    }

    private int maxContentScroll() {
        Rect viewport = contentViewportRect(frame());
        int contentHeight = switch (activeTab) {
            case HUD -> hudContentHeight();
            case DUNGEON -> dungeonContentHeight();
            case PARTICLE -> particleContentHeight();
            case MISC -> miscContentHeight();
            case ANTI_SPAM -> antiSpamContentHeight();
            case CHAT -> chatContentHeight();
        };
        return Math.max(0, contentHeight - viewport.height);
    }

    private int maxParticleScroll() {
        return Math.max(0, filteredParticleIds().size() * 14 - 260);
    }

    private List<String> filteredParticleIds() {
        List<String> particleIds = new ArrayList<>(particleFilterService.particleIds());
        if (particleSearchInput.isBlank()) {
            return particleIds;
        }
        String query = particleSearchInput.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String particleId : particleIds) {
            String displayName = particleFilterService.displayName(particleId);
            if (particleId.toLowerCase(Locale.ROOT).contains(query) || displayName.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(particleId);
            }
        }
        return filtered;
    }

    private List<SearchResult> searchResults() {
        if (globalSearchInput.isBlank()) {
            return List.of();
        }
        String query = globalSearchInput.toLowerCase(Locale.ROOT);
        List<SearchResult> results = new ArrayList<>();
        addSearchResult(results, query, "Revive HUD", "HUD", Tab.HUD, null, "revive hud spirit bonzo phoenix");
        addSearchResult(results, query, "HUD bearbeiten", "HUD", Tab.HUD, null, "hud bearbeiten layout reset");
        addSearchResult(results, query, "HUD Farbe", "HUD", Tab.HUD, null, "hud farbe accent color hex");
        addSearchResult(results, query, "Spotify Client ID", "HUD", Tab.HUD, null, "spotify client id login premium");
        addSearchResult(results, query, "Spotify Inventarsteuerung", "HUD", Tab.HUD, null, "spotify inventarsteuerung inventar controls");
        addSearchResult(results, query, "Hypixel API Key", "HUD", Tab.HUD, null, "hypixel api key profile party finder");
        addSearchResult(results, query, "Party Finder Overlay", "Dungeons / General", Tab.DUNGEON, DungeonSection.GENERAL, "party finder overlay dungeon general");
        addSearchResult(results, query, "Rare Room Alerts", "Dungeons / General", Tab.DUNGEON, DungeonSection.GENERAL, "rare room alerts trinity tomioka duncan");
        addSearchResult(results, query, "Catacombs Level", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL, "catacombs level revive");
        addSearchResult(results, query, "Boss Only", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL, "boss only revive");
        addSearchResult(results, query, "Always Visible", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL, "always visible revive");
        for (ReviveSource source : ReviveSource.values()) {
            addSearchResult(results, query, source.displayName(), "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL, source.displayName() + " revive");
        }
        for (TerminalSolverOption option : TerminalSolverOption.values()) {
            addSearchResult(results, query, option.title(), "Dungeons / Terminal Solver", Tab.DUNGEON, DungeonSection.TERMINAL_SOLVER, option.title() + " " + option.description());
        }
        for (PuzzleSolverOption option : PuzzleSolverOption.values()) {
            addSearchResult(results, query, option.title(), "Dungeons / Puzzle Solver", Tab.DUNGEON, DungeonSection.PUZZLE_SOLVER, option.title() + " " + option.description());
        }
        addSearchResult(results, query, "Particle Suche", "Particle", Tab.PARTICLE, null, "particle suche filter");
        addSearchResult(results, query, "Zeit HUD", "Misc", Tab.MISC, null, "zeit hud clock");
        addSearchResult(results, query, "FPS / TPS / Ping", "Misc", Tab.MISC, null, "fps tps ping performance");
        addSearchResult(results, query, "System HUD", "Misc", Tab.MISC, null, "system hud cpu gpu temperatur");
        addSearchResult(results, query, "Solver Debug HUD", "Misc", Tab.MISC, null, "solver debug hud");
        addSearchResult(results, query, "Defense Bar", "Misc", Tab.MISC, null, "defense bar ruestung armor");
        addSearchResult(results, query, "Kompakte Herzen", "Misc", Tab.MISC, null, "kompakte herzen hypixel health herz absorption");
        addSearchResult(results, query, "Anti Spam Gesamt", "Anti Spam", Tab.ANTI_SPAM, null, "anti spam gesamt");
        for (SpamFilterOption option : SpamFilterOption.values()) {
            addSearchResult(results, query, option.title(), "Anti Spam", Tab.ANTI_SPAM, null, option.title() + " " + option.description());
        }
        addSearchResult(results, query, "Bridge verstecken", "Chat", Tab.CHAT, null, "bridge discord guild bot verstecken ausblenden");
        addSearchResult(results, query, "Bridge Bot Name", "Chat", Tab.CHAT, null, "bridge bot name catgirlfc guild discord");
        addSearchResult(results, query, "Nachrichten kopieren", "Chat", Tab.CHAT, null, "chat nachricht kopieren clipboard copy ctrl rechts klick");
        return results;
    }

    private void addSearchResult(List<SearchResult> results, String query, String title, String location, Tab tab, DungeonSection section, String haystack) {
        String lowerHaystack = (title + " " + location + " " + haystack).toLowerCase(Locale.ROOT);
        if (lowerHaystack.contains(query)) {
            results.add(new SearchResult(title, location, tab, section));
        }
    }

    private void adjustCatacombsLevel(int delta) {
        int next = Math.max(0, Math.min(50, config().getCatacombsLevel() + delta));
        config().setCatacombsLevel(next);
        refreshCatacombsInput();
        horizonClient.getConfigManager().save();
    }

    private Rect frame() {
        int frameWidth = Math.min(960, width - 28);
        int frameHeight = Math.min(640, height - 28);
        return new Rect((width - frameWidth) / 2, (height - frameHeight) / 2, frameWidth, frameHeight);
    }

    private Rect sidebarRect(Rect frame) {
        return new Rect(frame.x + 12, frame.y + 40, 140, frame.height - 52);
    }

    private Rect subTabBarRect(Rect frame) {
        Rect viewport = contentViewportRect(frame);
        return new Rect(viewport.x, frame.y + 40, viewport.width, 18);
    }

    private Rect contentViewportRect(Rect frame) {
        int left = sidebarRect(frame).right() + 18;
        int top = frame.y + (activeTab == Tab.DUNGEON ? 62 : 40);
        return new Rect(left, top, frame.right() - left - 12, frame.bottom() - top - 12);
    }

    private Rect contentClipRect(Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int left = viewport.x - 12;
        int top = viewport.y;
        int right = frame.right() - 1;
        int bottom = viewport.bottom();
        return new Rect(left, top, right - left, bottom - top);
    }

    private Rect searchRect(Rect frame) {
        return new Rect(frame.right() - 220, frame.y + 12, 170, 18);
    }

    private Rect closeRect(Rect frame) {
        return new Rect(frame.right() - 24, frame.y + 12, 18, 18);
    }

    private Rect sidebarTabRect(Rect sidebar, int index) {
        return new Rect(sidebar.x, sidebar.y + index * 16, sidebar.width, 14);
    }

    private Rect subTabRect(Rect bar, int index) {
        int width = 120;
        return new Rect(bar.x + index * (width + 8), bar.y, width, 14);
    }

    private int accentColor() {
        return HudStyle.accent(config());
    }

    private Rect hudColorPreviewRect(int x, int y) {
        return new Rect(x, y, 32, 18);
    }

    private Rect hudColorSwatchRect(int x, int y, int index) {
        int swatchX = x + 148 + (index % 6) * 22;
        int swatchY = y + (index / 6) * 22;
        return new Rect(swatchX, swatchY, 18, 18);
    }

    private int parsePreviewColor() {
        String value = HudStyle.isCompleteHex(hudAccentColorInput) ? hudAccentColorInput : config().getHudAccentColor();
        return 0xFF000000 | Integer.parseInt(value.substring(1), 16);
    }

    private int toggleRowHeight(String description) {
        return toggleCardHeight(description);
    }

    private int actionRowHeight(String description) {
        return cardHeight(description, true);
    }

    private int fieldRowHeight(String description) {
        return cardHeight(description, false);
    }

    private int numberRowHeight(String description) {
        return cardHeight(description, false);
    }

    private int hudColorRowHeight() {
        int descLines = wrappedLines("Preview und Palette. Hexwert bleibt weiter editierbar.", CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10).size();
        int previewY = CARD_PADDING_TOP + LINE_HEIGHT + descLines * LINE_HEIGHT + 6;
        int contentHeight = previewY + 22 + 18;
        return contentHeight + CARD_PADDING_BOTTOM + CARD_GAP;
    }

    private int hudColorPreviewY(int y) {
        int descLines = wrappedLines("Preview und Palette. Hexwert bleibt weiter editierbar.", CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10).size();
        return y + CARD_PADDING_TOP + LINE_HEIGHT + descLines * LINE_HEIGHT + 6;
    }

    private int cardHeight(String description, boolean hasButtonRow) {
        int titleBlock = CARD_PADDING_TOP + LINE_HEIGHT;
        int descBlock = wrappedLines(description, CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10).size() * LINE_HEIGHT;
        int buttonBlock = hasButtonRow ? 28 : 0;
        return titleBlock + descBlock + buttonBlock + CARD_PADDING_BOTTOM + CARD_GAP;
    }

    private int toggleCardHeight(String description) {
        int badgeWidth = toggleBadgeRect(0, 0).width;
        int textStart = badgeWidth + 10;
        int contentWidth = Math.max(80, CONTENT_ROW_WIDTH - textStart - 10);
        int titleBlock = CARD_PADDING_TOP + LINE_HEIGHT;
        int descBlock = wrappedLines(description, contentWidth).size() * LINE_HEIGHT;
        return titleBlock + descBlock + CARD_PADDING_BOTTOM + CARD_GAP;
    }

    private void drawWrappedText(DrawContext context, int x, int y, String text, int maxWidth, int color) {
        int lineY = y;
        for (String line : wrappedLines(text, maxWidth)) {
            drawTextLine(context, x, lineY, line, color);
            lineY += LINE_HEIGHT;
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
        return lines.isEmpty() ? List.of("") : lines;
    }

    private Rect toggleBadgeRect(int x, int y) {
        return new Rect(x, y + CARD_PADDING_TOP - 1, 38, 18);
    }

    private Rect actionButtonRect(int x, int y, boolean left) {
        return new Rect(x + (left ? 0 : 152), y + CARD_PADDING_TOP, 136, 20);
    }

    private Rect cataButtonRect(int x, int y, boolean minus) {
        return new Rect(x + (minus ? 500 : 540), y + CARD_PADDING_TOP - 1, 34, 22);
    }

    private void drawInlineAction(DrawContext context, Rect rect, String label) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), CONFIG_BUTTON);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), rect.centerX(), rect.y + 5, CONFIG_BUTTON_TEXT);
    }

    private int hudContentHeight() {
        return 24
            + toggleRowHeight("Spirit, Bonzo und Phoenix als Status-Panel.")
            + actionRowHeight("Layout bearbeiten oder Positionen zuruecksetzen.")
            + hudColorRowHeight()
            + fieldRowHeight("Spotify Premium Login.")
            + actionRowHeight(spotifyService.auth().getStatusMessage())
            + toggleRowHeight("Steuerung im Inventar ein- oder ausschalten.")
            + fieldRowHeight("Fuer Profil- und Party-Finder-Daten.");
    }

    private int dungeonContentHeight() {
        return 24 + switch (activeDungeonSection) {
            case GENERAL -> toggleRowHeight("Zeigt beste S+ Zeiten im Party Finder.")
                + toggleRowHeight("Alert fuer Trinity, Tomioka und Duncan.");
            case REVIVAL -> {
                int height = numberRowHeight("Nutze [-] und [+] oder tippe direkt.")
                    + toggleRowHeight("Nur waehrend Bossphasen.")
                    + toggleRowHeight("Auch ausserhalb des Kampfes sichtbar.");
                for (ReviveSource source : ReviveSource.values()) {
                    height += toggleRowHeight(source.cooldownLabel() + ": " + source.configuredCooldown(config()) + "s");
                }
                yield height;
            }
            case TERMINAL_SOLVER -> {
                int height = 0;
                for (TerminalSolverOption option : TerminalSolverOption.values()) {
                    height += toggleRowHeight(option.description());
                }
                yield height;
            }
            case PUZZLE_SOLVER -> {
                int height = 0;
                for (PuzzleSolverOption option : PuzzleSolverOption.values()) {
                    height += toggleRowHeight(option.description());
                }
                yield height;
            }
        };
    }

    private int particleContentHeight() {
        return fieldRowHeight("Liste filtern.") + Math.max(0, filteredParticleIds().size() * 14);
    }

    private int miscContentHeight() {
        return 24
            + toggleRowHeight("Lokale Uhrzeit als Overlay.")
            + toggleRowHeight("Performance-Overlay.")
            + toggleRowHeight("CPU / GPU / Temperaturen.")
            + toggleRowHeight("Diagnoseanzeige fuer Dungeon Solver.")
            + toggleRowHeight("Blendet die Vanilla-Ruestungsanzeige aus.")
            + toggleRowHeight("Fasst Hypixel-Herzen kompakt in einer Reihe zusammen.");
    }

    private int antiSpamContentHeight() {
        int height = 24 + toggleRowHeight("Reduziert Dungeon- und Ability-Noise.");
        for (SpamFilterOption option : SpamFilterOption.values()) {
            height += toggleRowHeight(option.description());
        }
        return height;
    }

    private int chatContentHeight() {
        return 24
            + toggleRowHeight("Discord-Bridge-Nachrichten im Guild-Chat ausblenden.")
            + fieldRowHeight("Ingame-Name des Discord-Bridge-Bots (z.B. catgirlfc).")
            + toggleRowHeight("Modus: Aus, Strg+LK, Rechtsklick oder Beides.")
            + toggleRowHeight("Alle Zeilen des Eintrags oder nur die angeklickte Zeile.");
    }

    private void drawWindowChrome(DrawContext context, Rect frame, Rect viewport, int accent) {
        context.fill(frame.x, frame.y, frame.right(), frame.bottom(), CONFIG_WINDOW);
        context.fill(viewport.x - 12, frame.y + 35, frame.right() - 1, frame.bottom() - 1, CONFIG_WINDOW);
        context.drawStrokedRectangle(frame.x, frame.y, frame.width, frame.height, HudStyle.border());
        context.fill(frame.x, frame.y, frame.right(), frame.y + 34, CONFIG_WINDOW_HEADER);
        drawTextLine(context, frame.x + 12, frame.y + 12, "HORIZON", accent);
        drawTextLine(context, searchRect(frame).x, searchRect(frame).y + 2, "Suche: " + fieldValue(globalSearchInput, inputFocus == InputFocus.GLOBAL_SEARCH), inputFocus == InputFocus.GLOBAL_SEARCH ? accent : TEXT);
        drawTextLine(context, closeRect(frame).x, closeRect(frame).y + 2, "[X]", WARNING);
    }

    private void drawHeaderMask(DrawContext context, Rect frame, int accent) {
        context.fill(frame.x + 1, frame.y + 1, frame.right() - 1, frame.y + 34, CONFIG_WINDOW_HEADER);
        context.drawStrokedRectangle(frame.x, frame.y, frame.width, frame.height, HudStyle.border());
        drawTextLine(context, frame.x + 12, frame.y + 12, "HORIZON", accent);
        drawTextLine(context, searchRect(frame).x, searchRect(frame).y + 2, "Suche: " + fieldValue(globalSearchInput, inputFocus == InputFocus.GLOBAL_SEARCH), inputFocus == InputFocus.GLOBAL_SEARCH ? accent : TEXT);
        drawTextLine(context, closeRect(frame).x, closeRect(frame).y + 2, "[X]", WARNING);
    }

    private enum Tab {
        HUD("HUD"),
        DUNGEON("Dungeons"),
        PARTICLE("Particle"),
        MISC("Misc"),
        ANTI_SPAM("Anti Spam"),
        CHAT("Chat");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private enum DungeonSection {
        GENERAL("General"),
        REVIVAL("Revive"),
        TERMINAL_SOLVER("Terminal Solver"),
        PUZZLE_SOLVER("Puzzle Solver");

        private final String label;

        DungeonSection(String label) {
            this.label = label;
        }
    }

    private enum InputFocus {
        NONE,
        CATACOMBS_LEVEL,
        HUD_ACCENT_COLOR,
        SPOTIFY_CLIENT_ID,
        HYPIXEL_API_KEY,
        CHAT_BRIDGE_BOT_NAME,
        GLOBAL_SEARCH,
        PARTICLE_SEARCH
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

    private record SearchResult(String title, String location, Tab tab, DungeonSection section) {
    }
}
