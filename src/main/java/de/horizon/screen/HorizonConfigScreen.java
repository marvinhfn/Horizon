package de.horizon.screen;

import de.horizon.HorizonClient;
import de.horizon.Lang;
import de.horizon.config.HorizonConfig;
import de.horizon.feature.fishing.ElusiveSeaCreature;
import de.horizon.feature.fishing.FishingAlertSound;
import de.horizon.screen.InventoryButtonLayoutScreen;
import de.horizon.feature.chat.ChatCopyMode;
import de.horizon.hypixel.SkyBlockIsland;
import de.horizon.feature.chat.SpamFilterOption;
import de.horizon.feature.dungeon.PuzzleSolverOption;
import de.horizon.feature.dungeon.TerminalSolverOption;
import de.horizon.feature.particle.ParticleFilterService;
import de.horizon.feature.revive.ReviveSource;
import de.horizon.hud.HudStyle;
import de.horizon.spotify.SpotifyService;
import de.horizon.youtube.YoutubeService;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private static final String[][] GLOBAL_SCOREBOARD_LINES = {
        {"location", "Standort (⏣-Zeile)"},
        {"season", "Season"},
        {"time", "Uhrzeit"},
        {"server_code", "Datum"},
        {"profile", "Profil"},
        {"www.hypixel.net", "www.hypixel.net"},
    };

    private static final String[] HUD_COLOR_SWATCHES = {
        "#75E7CA", "#60A5FA", "#FBBF24", "#FB7185", "#F472B6", "#A78BFA",
        "#34D399", "#F97316", "#F87171", "#22D3EE", "#C4B5FD", "#E5E7EB"
    };

    private final Screen parent;
    private final HorizonClient horizonClient;
    private final SpotifyService spotifyService;
    private final YoutubeService youtubeService;
    private final ParticleFilterService particleFilterService;

    private Tab activeTab = Tab.GENERAL;
    private DungeonSection activeDungeonSection = DungeonSection.GENERAL;
    private MusicSection activeMusicSection = MusicSection.GENERAL;
    private ChatSection activeChatSection = ChatSection.GENERAL;
    private InventorySection activeInventorySection = InventorySection.GENERAL;
    private boolean scoreboardGeneralActive = true;
    private SkyBlockIsland activeScoreboardIsland = SkyBlockIsland.HUB;
    private String pendingGlobalToggleKey = null;
    private String pendingGlobalToggleLabel = null;
    private boolean showReloadPopup = false;
    private InputFocus inputFocus = InputFocus.NONE;
    private String catacombsInput;
    private String hudAccentColorInput;
    private String chatBridgeBotNameInput;
    private String globalSearchInput = "";
    private String particleSearchInput = "";
    private int contentScrollOffset = 0;
    private int particleScrollOffset = 0;
    private String dragKey = null;
    private boolean isDragging = false;
    private int dragMouseOffsetY = 0;
    private int dragCurrentMouseY = 0;
    private boolean fishingCreatureListExpanded = false;

    public HorizonConfigScreen(Screen parent, HorizonClient horizonClient) {
        super(Text.literal("Horizon"));
        this.parent = parent;
        this.horizonClient = horizonClient;
        this.spotifyService = horizonClient.getSpotifyService();
        this.youtubeService = horizonClient.getYoutubeService();
        this.particleFilterService = horizonClient.getParticleFilterService();
        this.catacombsInput = String.valueOf(config().getCatacombsLevel());
        this.hudAccentColorInput = config().getHudAccentColor();
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

        if (showReloadPopup) {
            showReloadPopup = false;
            return true;
        }

        if (pendingGlobalToggleKey != null) {
            if (confirmYesRect(frame).contains(click.x(), click.y())) {
                config().toggleScoreboardGlobalLine(pendingGlobalToggleKey);
                horizonClient.getConfigManager().save();
                pendingGlobalToggleKey = null;
                pendingGlobalToggleLabel = null;
            } else {
                pendingGlobalToggleKey = null;
                pendingGlobalToggleLabel = null;
            }
            return true;
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
                if (subTabRect(bar, index, DungeonSection.values().length).contains(click.x(), click.y())) {
                    commitCatacombsInput();
                    activeDungeonSection = DungeonSection.values()[index];
                    contentScrollOffset = 0;
                    return true;
                }
            }
        }

        if (activeTab == Tab.MUSIC_CONTROL) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < MusicSection.values().length; index++) {
                if (subTabRect(bar, index, MusicSection.values().length).contains(click.x(), click.y())) {
                    activeMusicSection = MusicSection.values()[index];
                    contentScrollOffset = 0;
                    return true;
                }
            }
        }

        if (activeTab == Tab.CHAT) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < ChatSection.values().length; index++) {
                if (subTabRect(bar, index, ChatSection.values().length).contains(click.x(), click.y())) {
                    activeChatSection = ChatSection.values()[index];
                    contentScrollOffset = 0;
                    return true;
                }
            }
        }

        if (activeTab == Tab.INVENTORY) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < InventorySection.values().length; index++) {
                if (subTabRect(bar, index, InventorySection.values().length).contains(click.x(), click.y())) {
                    activeInventorySection = InventorySection.values()[index];
                    contentScrollOffset = 0;
                    return true;
                }
            }
        }

        if (activeTab == Tab.SCOREBOARD) {
            Rect bar = scoreboardSubTabBarRect(frame);
            if (scoreboardSubTabRect(bar, 0).contains(click.x(), click.y())) {
                scoreboardGeneralActive = true;
                contentScrollOffset = 0;
                dragKey = null;
                isDragging = false;
                return true;
            }
            SkyBlockIsland[] islands = SkyBlockIsland.knownIslands();
            for (int index = 0; index < islands.length; index++) {
                if (scoreboardSubTabRect(bar, index + 1).contains(click.x(), click.y())) {
                    scoreboardGeneralActive = false;
                    activeScoreboardIsland = islands[index];
                    contentScrollOffset = 0;
                    dragKey = null;
                    isDragging = false;
                    return true;
                }
            }
        }

        if (!globalSearchInput.isBlank()) {
            return handleSearchClick(click.x(), click.y(), contentViewportRect(frame)) || super.mouseClicked(click, doubled);
        }

        return switch (activeTab) {
            case GENERAL -> handleGeneralClick(click.x(), click.y(), frame);
            case HUD -> handleHudClick(click.x(), click.y(), frame);
            case DUNGEON -> handleDungeonClick(click.x(), click.y(), frame);
            case PARTICLE -> handleParticleClick(click.x(), click.y(), frame);
            case MISC -> handleMiscClick(click.x(), click.y(), frame);
            case CHAT -> handleChatClick(click.x(), click.y(), frame);
            case MUSIC_CONTROL -> handleMusicClick(click.x(), click.y(), frame);
            case SCOREBOARD -> handleScoreboardClick(click.x(), click.y(), frame);
            case INVENTORY -> handleInventoryClick(click.x(), click.y(), frame);
            case FISHING -> handleFishingClick(click.x(), click.y(), frame);
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
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() == 0 && dragKey != null) {
            isDragging = true;
            dragCurrentMouseY = (int) click.y();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && dragKey != null) {
            String key = dragKey;
            boolean wasDragging = isDragging;
            dragKey = null;
            isDragging = false;
            if (!wasDragging) {
                boolean wasVisible = !config().isScoreboardLineEffectivelyHidden(activeScoreboardIsland.id(), key);
                config().toggleScoreboardLine(activeScoreboardIsland.id(), key);
                if (wasVisible) {
                    // Line was turned OFF: sort it to just after the last still-ON line
                    autoSortDisabledLine(key);
                }
                horizonClient.getConfigManager().save();
            } else {
                Rect frame = frame();
                Rect viewport = contentViewportRect(frame);
                Map<String, String> known = islandDisplayLines();
                List<String> keysWithoutDragged = new ArrayList<>();
                for (String k : known.keySet()) {
                    if (!k.equals(key)) keysWithoutDragged.add(k);
                }
                int dropIdx = computeDropIndex(viewport, keysWithoutDragged);
                config().reorderScoreboardLine(activeScoreboardIsland.id(), key, dropIdx);
                horizonClient.getConfigManager().save();
            }
            return true;
        }
        return super.mouseReleased(click);
    }

    private void autoSortDisabledLine(String key) {
        Map<String, String> known = islandDisplayLines();
        List<String> keys = new ArrayList<>(known.keySet());
        int currentIndex = keys.indexOf(key);
        if (currentIndex < 0) return;
        int lastOnIndex = -1;
        for (int i = 0; i < keys.size(); i++) {
            if (!keys.get(i).equals(key)
                    && !config().isScoreboardLineEffectivelyHidden(activeScoreboardIsland.id(), keys.get(i))) {
                lastOnIndex = i;
            }
        }
        if (lastOnIndex < 0) return; // all lines off, no need to reorder
        // targetNewIndex is relative to the list after removing key
        int targetNewIndex = currentIndex > lastOnIndex ? lastOnIndex + 1 : lastOnIndex;
        if (targetNewIndex != currentIndex) {
            config().reorderScoreboardLine(activeScoreboardIsland.id(), key, targetNewIndex);
        }
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
                Rect rect = subTabRect(bar, index, DungeonSection.values().length);
                drawTextLine(context, rect.x, rect.y, (active ? "[" : "") + DungeonSection.values()[index].label + (active ? "]" : ""), active ? accent : TEXT);
            }
        }

        if (activeTab == Tab.MUSIC_CONTROL) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < MusicSection.values().length; index++) {
                boolean active = MusicSection.values()[index] == activeMusicSection;
                Rect rect = subTabRect(bar, index, MusicSection.values().length);
                drawTextLine(context, rect.x, rect.y, (active ? "[" : "") + MusicSection.values()[index].label + (active ? "]" : ""), active ? accent : TEXT);
            }
        }

        if (activeTab == Tab.CHAT) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < ChatSection.values().length; index++) {
                boolean active = ChatSection.values()[index] == activeChatSection;
                Rect rect = subTabRect(bar, index, ChatSection.values().length);
                drawTextLine(context, rect.x, rect.y, (active ? "[" : "") + ChatSection.values()[index].label + (active ? "]" : ""), active ? accent : TEXT);
            }
        }

        if (activeTab == Tab.INVENTORY) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < InventorySection.values().length; index++) {
                boolean active = InventorySection.values()[index] == activeInventorySection;
                Rect rect = subTabRect(bar, index, InventorySection.values().length);
                drawTextLine(context, rect.x, rect.y, (active ? "[" : "") + InventorySection.values()[index].label + (active ? "]" : ""), active ? accent : TEXT);
            }
        }

        if (activeTab == Tab.SCOREBOARD) {
            Rect bar = scoreboardSubTabBarRect(frame);
            drawTextLine(context, scoreboardSubTabRect(bar, 0).x, scoreboardSubTabRect(bar, 0).y,
                (scoreboardGeneralActive ? "[" : "") + "General" + (scoreboardGeneralActive ? "]" : ""),
                scoreboardGeneralActive ? accent : TEXT);
            SkyBlockIsland[] islands = SkyBlockIsland.knownIslands();
            for (int index = 0; index < islands.length; index++) {
                boolean active = !scoreboardGeneralActive && islands[index] == activeScoreboardIsland;
                Rect rect = scoreboardSubTabRect(bar, index + 1);
                drawTextLine(context, rect.x, rect.y, (active ? "[" : "") + islands[index].label() + (active ? "]" : ""), active ? accent : TEXT);
            }
        }

        context.enableScissor(contentClip.x, contentClip.y, contentClip.right(), contentClip.bottom());
        if (!globalSearchInput.isBlank()) {
            renderSearchResults(context, viewport);
        } else {
            switch (activeTab) {
                case GENERAL -> renderGeneralText(context, viewport);
                case HUD -> renderHudText(context, viewport);
                case DUNGEON -> renderDungeonText(context, viewport);
                case PARTICLE -> renderParticleText(context, viewport);
                case MISC -> renderMiscText(context, viewport);
                case CHAT -> renderChatText(context, viewport);
                case MUSIC_CONTROL -> renderMusicText(context, viewport);
                case SCOREBOARD -> renderScoreboardText(context, viewport);
                case INVENTORY -> renderInventoryText(context, viewport);
                case FISHING -> renderFishingText(context, viewport);
            }
        }
        context.disableScissor();
        if (pendingGlobalToggleKey != null) {
            drawConfirmationOverlay(context, frame, accent);
        }
        if (showReloadPopup) {
            drawReloadPopup(context, frame, accent);
        }
        drawHeaderMask(context, frame, accent);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderGeneralText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "General");
        Lang.Language lang = config().getLanguage();
        String langLabel = lang == Lang.Language.EN ? "English" : "Deutsch";
        y = drawCycleRow(context, viewport.x, y,
            Lang.t("Sprache", "Language"),
            langLabel,
            true,
            Lang.t("Sprache des Mods umschalten: Deutsch oder Englisch.", "Switch the mod language: German or English."));
        drawActionRow(context, viewport.x, y, Lang.t("Config Reload", "Config Reload"), "",
            Lang.t("Konfiguration neu laden.", "Reload the configuration from disk."));
    }

    private boolean handleGeneralClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            Lang.Language[] langs = Lang.Language.values();
            int next = (config().getLanguage().ordinal() + 1) % langs.length;
            config().setLanguage(langs[next]);
            Lang.set(config().getLanguage());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Sprache des Mods umschalten: Deutsch oder Englisch.", "Switch the mod language: German or English."));
        if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
            horizonClient.getConfigManager().load();
            contentScrollOffset = 0;
            refreshInputs();
            showReloadPopup = true;
            return true;
        }
        return false;
    }

    private int generalContentHeight() {
        return 24
            + toggleRowHeight(Lang.t("Sprache des Mods umschalten: Deutsch oder Englisch.", "Switch the mod language: German or English."))
            + actionRowHeight(Lang.t("Konfiguration neu laden.", "Reload the configuration from disk."));
    }

    private void renderHudText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "HUD");
        y = drawActionRow(context, viewport.x, y, Lang.t("HUD bearbeiten", "Edit HUD"), "HUD reset", Lang.t("Layout bearbeiten oder Positionen zuruecksetzen.", "Edit layout or reset positions."));
        drawHudColorRow(context, viewport.x, y);
    }

    private void renderDungeonText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeDungeonSection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Dungeons / General");
                y = drawToggleRow(context, viewport.x, y, "Party Finder Overlay", config().isDungeonPartyFinderOverlayEnabled(), Lang.t("Zeigt beste S+ Zeiten im Party Finder.", "Shows best S+ times in Party Finder."));
                y = drawToggleRow(context, viewport.x, y, "Rare Room Alerts", config().isDungeonRareRoomAlertsEnabled(), Lang.t("Alert fuer Trinity, Tomioka und Duncan.", "Alert for Trinity, Tomioka and Duncan."));
                drawToggleRow(context, viewport.x, y, "Rag Axe Notification", config().isRagAxeNotificationEnabled(), Lang.t("Rag!-Titel wenn Necron 'I no longer wish to fight...' sagt (M7).", "Shows Rag! title when Necron says 'I no longer wish to fight...' (M7)."));
            }
            case REVIVAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Dungeons / Revive");
                y = drawToggleRow(context, viewport.x, y, "Revive HUD", config().isReviveHudEnabled(), Lang.t("Spirit, Bonzo und Phoenix als Status-Panel.", "Spirit, Bonzo and Phoenix as status panel."));
                y = drawNumberRow(context, viewport.x, y, "Catacombs Level", catacombsInput, inputFocus == InputFocus.CATACOMBS_LEVEL, Lang.t("Nutze [-] und [+] oder tippe direkt.", "Use [-] and [+] or type directly."));
                y = drawToggleRow(context, viewport.x, y, "Boss Only", config().isReviveHudOnlyInBoss(), Lang.t("Nur waehrend Bossphasen.", "Only during boss phases."));
                y = drawToggleRow(context, viewport.x, y, "Always Visible", config().isReviveHudAlwaysVisible(), Lang.t("Auch ausserhalb des Kampfes sichtbar.", "Visible even outside combat."));
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
        drawFieldRow(context, viewport.x, y, Lang.t("Particle Suche", "Particle Search"), particleSearchInput, inputFocus == InputFocus.PARTICLE_SEARCH, Lang.t("Liste filtern.", "Filter list."));
        y += fieldRowHeight(Lang.t("Liste filtern.", "Filter list."));
        int baseY = y - particleScrollOffset;
        List<String> particles = filteredParticleIds();
        for (String particleId : particles) {
            String name = particleFilterService.displayName(particleId);
            boolean enabled = particleFilterService.isEnabled(particleId);
            drawTextLine(context, viewport.x, baseY, "[" + Lang.t(enabled ? "AN" : "AUS", enabled ? "ON" : "OFF") + "] " + name + " - " + particleId, enabled ? TEXT : MUTED);
            baseY += 14;
        }
    }

    private void renderMiscText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Misc");
        y = drawToggleRow(context, viewport.x, y, Lang.t("Zeit HUD", "Time HUD"), config().isTimeHudEnabled(), Lang.t("Lokale Uhrzeit als Overlay.", "Local time as overlay."));
        y = drawToggleRow(context, viewport.x, y, "FPS / TPS / Ping", config().isPerformanceHudEnabled(), Lang.t("Performance-Overlay.", "Performance overlay."));
        y = drawToggleRow(context, viewport.x, y, "System HUD", config().isSystemHudEnabled(), Lang.t("CPU / GPU / Temperaturen.", "CPU / GPU / Temperatures."));
        y = drawToggleRow(context, viewport.x, y, "Solver Debug HUD", config().isSolverDebugHudEnabled(), Lang.t("Diagnoseanzeige fuer Dungeon Solver.", "Diagnostic display for Dungeon Solver."));
        y = drawToggleRow(context, viewport.x, y, "Defense Bar", config().isHideDefenseBar(), Lang.t("Blendet die Vanilla-Ruestungsanzeige aus.", "Hides the vanilla armor display."));
        drawToggleRow(context, viewport.x, y, Lang.t("Kompakte Herzen", "Compact Hearts"), config().isCompactHypixelHealthEnabled(), Lang.t("Fasst Hypixel-Herzen kompakt in einer Reihe zusammen.", "Compacts Hypixel hearts into a single row."));
    }

    private void renderChatText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeChatSection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Chat / General");
                y = drawToggleRow(context, viewport.x, y, Lang.t("Guild Chat verstecken", "Hide Guild Chat"), config().isGuildChatHidden(), Lang.t("Alle Guild-Chat-Nachrichten ausblenden.", "Hide all guild chat messages."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Bridge verstecken", "Hide Bridge"), config().isChatBridgeHidden(), Lang.t("Discord-Bridge-Nachrichten im Guild-Chat ausblenden.", "Hide Discord bridge messages in guild chat."));
                y = drawFieldRow(context, viewport.x, y, "Bridge Bot Name", chatBridgeBotNameInput, inputFocus == InputFocus.CHAT_BRIDGE_BOT_NAME, Lang.t("Ingame-Name des Discord-Bridge-Bots (z.B. catgirlfc).", "In-game name of the Discord bridge bot (e.g. catgirlfc)."));
                ChatCopyMode copyMode = config().getChatCopyMode();
                y = drawCycleRow(context, viewport.x, y, Lang.t("Nachrichten kopieren", "Copy Messages"), copyMode.label(), copyMode != ChatCopyMode.OFF, Lang.t("Modus: Aus, Strg+LK, Rechtsklick oder Beides.", "Mode: Off, Ctrl+LClick, Right Click or Both."));
                drawToggleRow(context, viewport.x + 16, y, Lang.t("Gesamte Nachricht", "Full Message"), config().isChatCopyFullMessage(), Lang.t("Alle Zeilen des Eintrags oder nur die angeklickte Zeile.", "All lines of the entry or only the clicked line."));
            }
            case SPAM_FILTERS -> {
                y = drawSectionTitle(context, viewport.x, y, "Chat / Spam Filters");
                y = drawToggleRow(context, viewport.x, y, Lang.t("Anti Spam Gesamt", "Anti Spam All"), config().isAntiSpamEnabled(), Lang.t("Reduziert Dungeon- und Ability-Noise.", "Reduces dungeon and ability noise."));
                SpamFilterOption.Category prevCat = null;
                for (SpamFilterOption option : SpamFilterOption.values()) {
                    if (option.category() != prevCat) {
                        y = drawSectionTitle(context, viewport.x, y, option.category().label());
                        prevCat = option.category();
                    }
                    y = drawToggleRow(context, viewport.x, y, option.title(), option.isEnabled(config()), option.description());
                }
                y = drawSectionTitle(context, viewport.x, y, "Fishing");
                y = drawToggleRow(context, viewport.x, y, Lang.t("Sea Creatures filtern", "Filter Sea Creatures"),
                        config().isHideSeaCreatureMessages(), SEA_CREATURE_SPAM_DESC);
                y = drawToggleRow(context, viewport.x, y, Lang.t("Elusive Creatures filtern", "Filter Elusive Creatures"),
                        config().isHideElusiveSeaCreatureMessages(), ELUSIVE_SPAM_DESC);
                y = drawToggleRow(context, viewport.x, y, Lang.t("Trophy Fish filtern", "Filter Trophy Fish"),
                        config().isHideTrophyFishMessages(), TROPHY_FISH_SPAM_DESC);
                y = drawToggleRow(context, viewport.x, y, Lang.t("Trophy Frogs filtern", "Filter Trophy Frogs"),
                        config().isHideTrophyFrogMessages(), TROPHY_FROG_SPAM_DESC);
                y = drawToggleRow(context, viewport.x + 16, y, Lang.t("Diamond Trophies filtern", "Filter Diamond Trophies"),
                        config().isHideFishingDiamondTrophies(), FISH_DIAMOND_DESC);
                drawToggleRow(context, viewport.x, y, Lang.t("Good/Great/Outstanding filtern", "Filter Good/Great/Outstanding"),
                        config().isHideGoodGreatOutstandingMessages(), GOOD_GREAT_DESC);
            }
        }
    }

    private void renderScoreboardText(DrawContext context, Rect viewport) {
        if (scoreboardGeneralActive) {
            renderGeneralScoreboardText(context, viewport);
        } else {
            renderIslandScoreboardText(context, viewport);
        }
    }

    private void renderGeneralScoreboardText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Scoreboard / General");
        y = drawToggleRow(context, viewport.x, y, "Custom Scoreboard", config().isCustomScoreboardEnabled(), Lang.t("Eigene Scoreboard-Leiste am unteren Bildschirmrand anzeigen.", "Show custom scoreboard bar at the bottom of the screen."));
        y = drawSectionTitle(context, viewport.x, y, Lang.t("Globale Zeilenfilter", "Global Line Filters"));
        for (String[] entry : GLOBAL_SCOREBOARD_LINES) {
            boolean visible = !config().isScoreboardGlobalLineHidden(entry[0]);
            y = drawScoreboardLineRow(context, viewport.x, y, entry[1], visible, HorizonConfig.scoreboardKeyColor(entry[0]));
        }
    }

    private void renderIslandScoreboardText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Scoreboard / " + activeScoreboardIsland.label());
        Map<String, String> known = islandDisplayLines();
        if (known.isEmpty()) {
            drawTextLine(context, viewport.x, y, Lang.t("Keine Daten gespeichert. Besuche diese Island ingame.", "No data stored. Visit this island in-game."), MUTED);
            return;
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>(known.entrySet());
        int rowH = scoreboardLineRowHeight();
        List<String> keysWithoutDragged = new ArrayList<>();
        if (isDragging) {
            for (Map.Entry<String, String> e : entries) {
                if (!e.getKey().equals(dragKey)) {
                    keysWithoutDragged.add(e.getKey());
                }
            }
        }
        int dropIndex = isDragging ? computeDropIndex(viewport, keysWithoutDragged) : -1;
        int visualIndex = 0;
        for (Map.Entry<String, String> entry : entries) {
            boolean isBeingDragged = isDragging && entry.getKey().equals(dragKey);
            if (isBeingDragged) {
                y += rowH;
                continue;
            }
            if (isDragging && visualIndex == dropIndex) {
                context.fill(viewport.x - 12, y, viewport.x + CONTENT_ROW_WIDTH + 1, y + 2, accentColor());
            }
            boolean visible = !config().isScoreboardLineEffectivelyHidden(activeScoreboardIsland.id(), entry.getKey());
            int rowTop = y;
            y = drawScoreboardLineRow(context, viewport.x, y, HorizonConfig.formatScoreboardKeyLabel(entry.getKey()), visible, HorizonConfig.scoreboardKeyColor(entry.getKey()));
            drawTextLine(context, viewport.x + CONTENT_ROW_WIDTH - 14, rowTop + CARD_PADDING_TOP, "≡", MUTED);
            visualIndex++;
        }
        if (isDragging && visualIndex == dropIndex) {
            context.fill(viewport.x - 12, y, viewport.x + CONTENT_ROW_WIDTH + 1, y + 2, accentColor());
        }
        if (isDragging && dragKey != null) {
            boolean visible = !config().isScoreboardLineEffectivelyHidden(activeScoreboardIsland.id(), dragKey);
            drawScoreboardLineRow(context, viewport.x, dragCurrentMouseY - dragMouseOffsetY,
                HorizonConfig.formatScoreboardKeyLabel(dragKey), visible, HorizonConfig.scoreboardKeyColor(dragKey));
        }
    }

    private void renderMusicText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeMusicSection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Music Control / General");
                String serviceLabel = "YOUTUBE_MUSIC".equals(config().getActiveMusicService()) ? "YouTube" : "Spotify";
                y = drawCycleRow(context, viewport.x, y,
                    Lang.t("Aktiver Dienst", "Active Service"),
                    serviceLabel,
                    true,
                    Lang.t("Welcher Dienst im Inventar angezeigt wird.", "Which service is shown in inventory."));
                drawToggleRow(context, viewport.x, y,
                    Lang.t("Music Control HUD", "Music Control HUD"),
                    config().isSpotifyInventoryControlsEnabled(),
                    Lang.t("Steuerung im Inventar ein- oder ausschalten.", "Enable or disable controls in inventory."));
            }
            case SPOTIFY -> {
                y = drawSectionTitle(context, viewport.x, y, "Music Control / Spotify");
                drawActionRow(context, viewport.x, y, "Spotify Login", "Spotify Logout", spotifyService.auth().getStatusMessage());
            }
            case YOUTUBE_MUSIC -> {
                y = drawSectionTitle(context, viewport.x, y, "Music Control / Youtube Music");
                drawActionRow(context, viewport.x, y, "YouTube Login", "YouTube Logout", youtubeService.auth().getStatusMessage());
            }
        }
    }


    // ── Inventory tab ─────────────────────────────────────────────────────────

    private void renderInventoryText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeInventorySection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Inventory / General");
                drawToggleRow(context, viewport.x, y,
                        "Inventory Buttons",
                        config().isInventoryButtonsEnabled(),
                        "Zeigt konfigurierte Buttons um das Inventar herum.");
            }
            case INVENTORY_BUTTONS -> {
                y = drawSectionTitle(context, viewport.x, y, "Inventory / Inventory Buttons");
                int count = config().getInventoryButtons().size();
                String btnCountLabel = count + " Button" + (count == 1 ? "" : "s") + " konfiguriert";
                drawActionRow(context, viewport.x, y,
                        "Layout bearbeiten", "",
                        btnCountLabel + ". Klicke um Buttons zu platzieren und zu konfigurieren.");
            }
        }
    }

    private boolean handleInventoryClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        switch (activeInventorySection) {
            case GENERAL -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setInventoryButtonsEnabled(!config().isInventoryButtonsEnabled());
                    horizonClient.getConfigManager().save();
                    return true;
                }
            }
            case INVENTORY_BUTTONS -> {
                if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
                    client.setScreen(new InventoryButtonLayoutScreen(this, horizonClient));
                    return true;
                }
            }
        }
        return false;
    }

    private int inventoryContentHeight() {
        return 24 + switch (activeInventorySection) {
            case GENERAL -> toggleRowHeight("Zeigt konfigurierte Buttons um das Inventar herum.");
            case INVENTORY_BUTTONS -> {
                int count = config().getInventoryButtons().size();
                String desc = count + " Buttons konfiguriert. Klicke um Buttons zu platzieren.";
                yield actionRowHeight(desc);
            }
        };
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
            drawTextLine(context, viewport.x, y, Lang.t("Keine Treffer.", "No results."), MUTED);
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
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(enabled ? Lang.t("AN", "ON") : Lang.t("AUS", "OFF")), badge.centerX(), badge.y + 4, 0xFFF7FBFF);
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
        drawInlineAction(context, leftRect, left);
        if (right != null && !right.isBlank()) {
            drawInlineAction(context, actionButtonRect(x, y, false), right);
        }
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
        drawTextLine(context, x, y + CARD_PADDING_TOP, Lang.t("HUD Farbe: ", "HUD Color: ") + fieldValue(hudAccentColorInput, inputFocus == InputFocus.HUD_ACCENT_COLOR), TEXT);
        drawWrappedText(context, x + DESCRIPTION_INDENT, y + CARD_PADDING_TOP + LINE_HEIGHT, Lang.t("Preview und Palette. Hexwert bleibt weiter editierbar.", "Preview and palette. Hex value remains editable."), CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10, MUTED);

        int previewY = y + CARD_PADDING_TOP + LINE_HEIGHT + wrappedLines(Lang.t("Preview und Palette. Hexwert bleibt weiter editierbar.", "Preview and palette. Hex value remains editable."), CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10).size() * LINE_HEIGHT + 6;
        Rect preview = hudColorPreviewRect(x, previewY);
        context.fill(preview.x, preview.y, preview.right(), preview.bottom(), parsePreviewColor());
        drawTextLine(context, preview.right() + 10, preview.y + 6, HudStyle.isCompleteHex(hudAccentColorInput) ? Lang.t("Aktive HUD-Farbe", "Active HUD Color") : Lang.t("Ungueltig -> Default", "Invalid -> Default"), MUTED);

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
        if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
            client.setScreen(new HudLayoutScreen(this, horizonClient));
            return true;
        }
        if (actionButtonRect(viewport.x, y, false).contains(mouseX, mouseY)) {
            horizonClient.getConfigManager().resetPosition("revive_status", 20, 20);
            return true;
        }
        y += actionRowHeight(Lang.t("Layout bearbeiten oder Positionen zuruecksetzen.", "Edit layout or reset positions."));
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
                y += toggleRowHeight(Lang.t("Zeigt beste S+ Zeiten im Party Finder.", "Shows best S+ times in Party Finder."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDungeonRareRoomAlertsEnabled(!config().isDungeonRareRoomAlertsEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Alert fuer Trinity, Tomioka und Duncan.", "Alert for Trinity, Tomioka and Duncan."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setRagAxeNotificationEnabled(!config().isRagAxeNotificationEnabled());
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
            config().setReviveHudEnabled(!config().isReviveHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Spirit, Bonzo und Phoenix als Status-Panel.", "Spirit, Bonzo and Phoenix as status panel."));
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
        y += numberRowHeight(Lang.t("Nutze [-] und [+] oder tippe direkt.", "Use [-] and [+] or type directly."));
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setReviveHudOnlyInBoss(!config().isReviveHudOnlyInBoss());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Nur waehrend Bossphasen.", "Only during boss phases."));
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setReviveHudAlwaysVisible(!config().isReviveHudAlwaysVisible());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Auch ausserhalb des Kampfes sichtbar.", "Visible even outside combat."));
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
        int y = viewport.y + fieldRowHeight(Lang.t("Liste filtern.", "Filter list.")) - particleScrollOffset;
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
        y += toggleRowHeight(Lang.t("Lokale Uhrzeit als Overlay.", "Local time as overlay."));
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setPerformanceHudEnabled(!config().isPerformanceHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Performance-Overlay.", "Performance overlay."));
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setSystemHudEnabled(!config().isSystemHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("CPU / GPU / Temperaturen.", "CPU / GPU / Temperatures."));
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setSolverDebugHudEnabled(!config().isSolverDebugHudEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Diagnoseanzeige fuer Dungeon Solver.", "Diagnostic display for Dungeon Solver."));
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setHideDefenseBar(!config().isHideDefenseBar());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Blendet die Vanilla-Ruestungsanzeige aus.", "Hides the vanilla armor display."));
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
        return switch (activeChatSection) {
            case GENERAL -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setGuildChatHidden(!config().isGuildChatHidden());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Alle Guild-Chat-Nachrichten ausblenden.", "Hide all guild chat messages."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setChatBridgeHidden(!config().isChatBridgeHidden());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Discord-Bridge-Nachrichten im Guild-Chat ausblenden.", "Hide Discord bridge messages in guild chat."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    inputFocus = InputFocus.CHAT_BRIDGE_BOT_NAME;
                    yield true;
                }
                y += fieldRowHeight(Lang.t("Ingame-Name des Discord-Bridge-Bots (z.B. catgirlfc).", "In-game name of the Discord bridge bot (e.g. catgirlfc)."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    ChatCopyMode[] modes = ChatCopyMode.values();
                    int next = (config().getChatCopyMode().ordinal() + 1) % modes.length;
                    config().setChatCopyMode(modes[next]);
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Modus: Aus, Strg+LK, Rechtsklick oder Beides.", "Mode: Off, Ctrl+LClick, Right Click or Both."));
                if (rowRect(viewport.x + 16, y).contains(mouseX, mouseY)) {
                    config().setChatCopyFullMessage(!config().isChatCopyFullMessage());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                yield false;
            }
            case SPAM_FILTERS -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setAntiSpamEnabled(!config().isAntiSpamEnabled());
                    horizonClient.getConfigManager().save();
                    horizonClient.getChatTabManager().repopulateAfterSpamFilterChange(config());
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Reduziert Dungeon- und Ability-Noise.", "Reduces dungeon and ability noise."));
                SpamFilterOption.Category prevCat = null;
                for (SpamFilterOption option : SpamFilterOption.values()) {
                    if (option.category() != prevCat) {
                        y += 24; // section title
                        prevCat = option.category();
                    }
                    if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                        option.toggle(config());
                        horizonClient.getConfigManager().save();
                        horizonClient.getChatTabManager().repopulateAfterSpamFilterChange(config());
                        yield true;
                    }
                    y += toggleRowHeight(option.description());
                }
                y += 24; // "Fishing" section title
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setHideSeaCreatureMessages(!config().isHideSeaCreatureMessages());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(SEA_CREATURE_SPAM_DESC);
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setHideElusiveSeaCreatureMessages(!config().isHideElusiveSeaCreatureMessages());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(ELUSIVE_SPAM_DESC);
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setHideTrophyFishMessages(!config().isHideTrophyFishMessages());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(TROPHY_FISH_SPAM_DESC);
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setHideTrophyFrogMessages(!config().isHideTrophyFrogMessages());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(TROPHY_FROG_SPAM_DESC);
                if (rowRect(viewport.x + 16, y).contains(mouseX, mouseY)) {
                    config().setHideFishingDiamondTrophies(!config().isHideFishingDiamondTrophies());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(FISH_DIAMOND_DESC);
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setHideGoodGreatOutstandingMessages(!config().isHideGoodGreatOutstandingMessages());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                yield false;
            }
        };
    }

    private boolean handleMusicClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24; // skip section title
        switch (activeMusicSection) {
            case GENERAL -> {
                // Active service cycle row
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    String current = config().getActiveMusicService();
                    config().setActiveMusicService("YOUTUBE_MUSIC".equals(current) ? "SPOTIFY" : "YOUTUBE_MUSIC");
                    horizonClient.getConfigManager().save();
                    return true;
                }
                y += toggleRowHeight(Lang.t("Welcher Dienst im Inventar angezeigt wird.", "Which service is shown in inventory."));
                // Music Control HUD toggle
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setSpotifyInventoryControlsEnabled(!config().isSpotifyInventoryControlsEnabled());
                    horizonClient.getConfigManager().save();
                    return true;
                }
            }
            case SPOTIFY -> {
                if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
                    spotifyService.auth().beginLogin();
                    return true;
                }
                if (actionButtonRect(viewport.x, y, false).contains(mouseX, mouseY)) {
                    spotifyService.auth().disconnect();
                    return true;
                }
            }
            case YOUTUBE_MUSIC -> {
                if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
                    youtubeService.auth().beginLogin();
                    return true;
                }
                if (actionButtonRect(viewport.x, y, false).contains(mouseX, mouseY)) {
                    youtubeService.auth().disconnect();
                    return true;
                }
            }
        }
        return false;
    }


    private boolean handleScoreboardClick(double mouseX, double mouseY, Rect frame) {
        if (scoreboardGeneralActive) {
            return handleGeneralScoreboardClick(mouseX, mouseY, frame);
        } else {
            return handleIslandScoreboardClick(mouseX, mouseY, frame);
        }
    }

    private boolean handleGeneralScoreboardClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset;
        y += 24; // section title
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setCustomScoreboardEnabled(!config().isCustomScoreboardEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Eigene Scoreboard-Leiste am unteren Bildschirmrand anzeigen.", "Show custom scoreboard bar at the bottom of the screen."));
        y += 24; // section title "Globale Zeilenfilter"
        for (String[] entry : GLOBAL_SCOREBOARD_LINES) {
            if (rowRect(viewport.x, y, scoreboardLineRowHeight()).contains(mouseX, mouseY)) {
                pendingGlobalToggleKey = entry[0];
                pendingGlobalToggleLabel = entry[1];
                return true;
            }
            y += scoreboardLineRowHeight();
        }
        return false;
    }

    private boolean handleIslandScoreboardClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset;
        y += 24; // section title
        int rowH = scoreboardLineRowHeight();
        Map<String, String> known = islandDisplayLines();
        for (Map.Entry<String, String> entry : known.entrySet()) {
            if (rowRect(viewport.x, y, rowH).contains(mouseX, mouseY)) {
                dragKey = entry.getKey();
                dragMouseOffsetY = (int) mouseY - y;
                dragCurrentMouseY = (int) mouseY;
                isDragging = false;
                return true;
            }
            y += rowH;
        }
        return false;
    }

    private int computeDropIndex(Rect viewport, List<String> keysWithoutDragged) {
        int y = viewport.y - contentScrollOffset + 24;
        int rowH = scoreboardLineRowHeight();
        for (int i = 0; i < keysWithoutDragged.size(); i++) {
            if (dragCurrentMouseY < y + rowH / 2) {
                return i;
            }
            y += rowH;
        }
        return keysWithoutDragged.size();
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

    /**
     * Returns the scoreboard lines for the active island tab.
     * When the player is currently on that island the live snapshot is used,
     * deduplicated by key so that dynamic values (counters, timers, plot numbers,
     * etc.) always produce exactly one row.  Falls back to stored known-lines when
     * the player is on a different island.
     */
    private Map<String, String> islandDisplayLines() {
        return config().getScoreboardKnownLines(activeScoreboardIsland.id());
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
        refreshChatBridgeBotNameInput();
        if (inputFocus == InputFocus.GLOBAL_SEARCH) {
            globalSearchInput = "";
        }
        if (inputFocus == InputFocus.PARTICLE_SEARCH) {
            particleSearchInput = "";
            particleScrollOffset = 0;
        }
    }

    private boolean isAllowedHudColorChar(int codepoint) {
        return codepoint == '#'
            || (codepoint >= '0' && codepoint <= '9')
            || (codepoint >= 'a' && codepoint <= 'f')
            || (codepoint >= 'A' && codepoint <= 'F');
    }

    private void refreshCatacombsInput() {
        catacombsInput = String.valueOf(config().getCatacombsLevel());
    }

    private void refreshHudAccentColorInput() {
        hudAccentColorInput = config().getHudAccentColor();
    }

    private void commitInputs() {
        commitCatacombsInput();
        commitHudAccentColorInput();
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

    private void commitHudAccentColorInput() {
        if (inputFocus != InputFocus.HUD_ACCENT_COLOR) {
            return;
        }
        config().setHudAccentColor(hudAccentColorInput);
        refreshHudAccentColorInput();
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
            case GENERAL -> generalContentHeight();
            case HUD -> hudContentHeight();
            case DUNGEON -> dungeonContentHeight();
            case PARTICLE -> particleContentHeight();
            case MISC -> miscContentHeight();
            case CHAT -> chatContentHeight();
            case MUSIC_CONTROL -> musicContentHeight();
            case SCOREBOARD -> scoreboardContentHeight();
            case INVENTORY -> inventoryContentHeight();
            case FISHING -> fishingContentHeight();
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
        addSearchResult(results, query, "HUD bearbeiten", "HUD", Tab.HUD, null, "hud bearbeiten layout reset");
        addSearchResult(results, query, "HUD Farbe", "HUD", Tab.HUD, null, "hud farbe accent color hex");

        addSearchResult(results, query, "Aktiver Dienst", "Music Control / General", Tab.MUSIC_CONTROL, null, "aktiver dienst spotify youtube music service");
        addSearchResult(results, query, "Music Control HUD", "Music Control / General", Tab.MUSIC_CONTROL, null, "music control hud inventarsteuerung inventar controls");
        addSearchResult(results, query, "Spotify Login", "Music Control / Spotify", Tab.MUSIC_CONTROL, null, "spotify login logout verbinden");
        addSearchResult(results, query, "YouTube Login", "Music Control / Youtube Music", Tab.MUSIC_CONTROL, null, "youtube login logout verbinden google");
        addSearchResult(results, query, "Party Finder Overlay", "Dungeons / General", Tab.DUNGEON, DungeonSection.GENERAL, "party finder overlay dungeon general");
        addSearchResult(results, query, "Rare Room Alerts", "Dungeons / General", Tab.DUNGEON, DungeonSection.GENERAL, "rare room alerts trinity tomioka duncan");
        addSearchResult(results, query, "Revive HUD", "Dungeons / Revive", Tab.DUNGEON, DungeonSection.REVIVAL, "revive hud spirit bonzo phoenix");
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
        addSearchResult(results, query, "Rag Axe Notification", "Dungeons / General", Tab.DUNGEON, DungeonSection.GENERAL, "rag axe notification necron m7 phase dungeon");
        addSearchResult(results, query, "Bridge verstecken", "Chat / General", Tab.CHAT, null, "bridge discord guild bot verstecken ausblenden");
        addSearchResult(results, query, "Bridge Bot Name", "Chat / General", Tab.CHAT, null, "bridge bot name catgirlfc guild discord");
        addSearchResult(results, query, "Nachrichten kopieren", "Chat / General", Tab.CHAT, null, "chat nachricht kopieren clipboard copy ctrl rechts klick");
        addSearchResult(results, query, "Anti Spam Gesamt", "Chat / Spam Filters", Tab.CHAT, null, "anti spam gesamt");
        for (SpamFilterOption option : SpamFilterOption.values()) {
            addSearchResult(results, query, option.title(), "Chat / Spam Filters", Tab.CHAT, null, option.title() + " " + option.description());
        }
        addSearchResult(results, query, Lang.t("Sea Creatures filtern", "Filter Sea Creatures"), "Chat / Spam Filters", Tab.CHAT, null, "sea creatures filtern fishing fang nachrichten atoll lotus");
        addSearchResult(results, query, Lang.t("Elusive Creatures filtern", "Filter Elusive Creatures"), "Chat / Spam Filters", Tab.CHAT, null, "elusive creatures filtern fishing rare fang nachrichten");
        addSearchResult(results, query, Lang.t("Trophy Fish filtern", "Filter Trophy Fish"), "Chat / Spam Filters", Tab.CHAT, null, "trophy fish filtern fishing fang nachrichten");
        addSearchResult(results, query, Lang.t("Trophy Frogs filtern", "Filter Trophy Frogs"), "Chat / Spam Filters", Tab.CHAT, null, "trophy frogs filtern fishing fang nachrichten");
        addSearchResult(results, query, Lang.t("Diamond Trophies filtern", "Filter Diamond Trophies"), "Chat / Spam Filters", Tab.CHAT, null, "diamond trophies filtern fishing fang nachrichten");
        addSearchResult(results, query, Lang.t("Good/Great/Outstanding filtern", "Filter Good/Great/Outstanding"), "Chat / Spam Filters", Tab.CHAT, null, "good great outstanding perfect catch filtern fishing");
        addSearchResult(results, query, "Custom Scoreboard", "Scoreboard", Tab.SCOREBOARD, null, "custom scoreboard sidebar hypixel leiste");
        for (SkyBlockIsland island : SkyBlockIsland.knownIslands()) {
            addSearchResult(results, query, island.label(), "Scoreboard", Tab.SCOREBOARD, null, "scoreboard " + island.label().toLowerCase(Locale.ROOT) + " island zeilen filter");
        }
        addSearchResult(results, query, "Announce Rare Sea Creatures", "Fishing", Tab.FISHING, null, "fishing rare sea creatures elusive announce title sound alert");
        addSearchResult(results, query, Lang.t("Alert Sound", "Alert Sound"), "Fishing", Tab.FISHING, null, "fishing alert sound rare meow katze custom boo womp");
        addSearchResult(results, query, "Creature Filter", "Fishing", Tab.FISHING, null, "fishing creature filter sea creatures toggle enable disable");
        for (ElusiveSeaCreature creature : ElusiveSeaCreature.values()) {
            addSearchResult(results, query, creature.displayName(), "Fishing", Tab.FISHING, null, "fishing " + creature.displayName().toLowerCase(Locale.ROOT) + " elusive sea creature");
        }
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
        int top;
        if (activeTab == Tab.DUNGEON || activeTab == Tab.MUSIC_CONTROL
                || activeTab == Tab.CHAT || activeTab == Tab.INVENTORY) {
            top = frame.y + 62;
        } else if (activeTab == Tab.SCOREBOARD) {
            top = frame.y + 80;
        } else {
            top = frame.y + 40;
        }
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

    private Rect subTabRect(Rect bar, int index, int count) {
        int gap = 8;
        int width = Math.max(60, (bar.width - gap * (count - 1)) / count);
        return new Rect(bar.x + index * (width + gap), bar.y, width, 14);
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
        int descLines = wrappedLines(Lang.t("Preview und Palette. Hexwert bleibt weiter editierbar.", "Preview and palette. Hex value remains editable."), CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10).size();
        int previewY = CARD_PADDING_TOP + LINE_HEIGHT + descLines * LINE_HEIGHT + 6;
        int contentHeight = previewY + 22 + 18;
        return contentHeight + CARD_PADDING_BOTTOM + CARD_GAP;
    }

    private int hudColorPreviewY(int y) {
        int descLines = wrappedLines(Lang.t("Preview und Palette. Hexwert bleibt weiter editierbar.", "Preview and palette. Hex value remains editable."), CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10).size();
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
            + actionRowHeight(Lang.t("Layout bearbeiten oder Positionen zuruecksetzen.", "Edit layout or reset positions."))
            + hudColorRowHeight();
    }

    private int musicContentHeight() {
        return 24 + switch (activeMusicSection) {
            case GENERAL -> toggleRowHeight(Lang.t("Welcher Dienst im Inventar angezeigt wird.", "Which service is shown in inventory."))
                + toggleRowHeight(Lang.t("Steuerung im Inventar ein- oder ausschalten.", "Enable or disable controls in inventory."));
            case SPOTIFY -> actionRowHeight(spotifyService.auth().getStatusMessage());
            case YOUTUBE_MUSIC -> actionRowHeight(youtubeService.auth().getStatusMessage());
        };
    }

    private int dungeonContentHeight() {
        return 24 + switch (activeDungeonSection) {
            case GENERAL -> toggleRowHeight(Lang.t("Zeigt beste S+ Zeiten im Party Finder.", "Shows best S+ times in Party Finder."))
                + toggleRowHeight(Lang.t("Alert fuer Trinity, Tomioka und Duncan.", "Alert for Trinity, Tomioka and Duncan."))
                + toggleRowHeight(Lang.t("Rag!-Titel wenn Necron 'I no longer wish to fight...' sagt (M7).", "Shows Rag! title when Necron says 'I no longer wish to fight...' (M7)."));
            case REVIVAL -> {
                int height = toggleRowHeight(Lang.t("Spirit, Bonzo und Phoenix als Status-Panel.", "Spirit, Bonzo and Phoenix as status panel."))
                    + numberRowHeight(Lang.t("Nutze [-] und [+] oder tippe direkt.", "Use [-] and [+] or type directly."))
                    + toggleRowHeight(Lang.t("Nur waehrend Bossphasen.", "Only during boss phases."))
                    + toggleRowHeight(Lang.t("Auch ausserhalb des Kampfes sichtbar.", "Visible even outside combat."));
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
        return fieldRowHeight(Lang.t("Liste filtern.", "Filter list.")) + Math.max(0, filteredParticleIds().size() * 14);
    }

    private int miscContentHeight() {
        return 24
            + toggleRowHeight(Lang.t("Lokale Uhrzeit als Overlay.", "Local time as overlay."))
            + toggleRowHeight(Lang.t("Performance-Overlay.", "Performance overlay."))
            + toggleRowHeight(Lang.t("CPU / GPU / Temperaturen.", "CPU / GPU / Temperatures."))
            + toggleRowHeight(Lang.t("Diagnoseanzeige fuer Dungeon Solver.", "Diagnostic display for Dungeon Solver."))
            + toggleRowHeight(Lang.t("Blendet die Vanilla-Ruestungsanzeige aus.", "Hides the vanilla armor display."))
            + toggleRowHeight(Lang.t("Fasst Hypixel-Herzen kompakt in einer Reihe zusammen.", "Compacts Hypixel hearts into a single row."));
    }

    private static final String FISH_ALERT_DESC        = Lang.t("Title + Sound wenn ein Elusive Sea Creature erkannt wird.", "Title + sound when an Elusive Sea Creature is detected.");
    private static final String FISH_SOUND_DESC        = Lang.t("Rare Sound, Miau oder Custom (Boo Womp).", "Rare Sound, Meow or Custom (Boo Womp).");
    private static final String FISH_FILTER_DESC       = Lang.t("Einzelne Sea Creatures vom Alert ausschliessen.", "Exclude individual sea creatures from the alert.");
    private static final String SEA_CREATURE_SPAM_DESC  = Lang.t("Fangnachrichten fuer Sea Creatures ausblenden (inkl. Atoll).", "Hide catch messages for sea creatures (incl. Atoll).");
    private static final String ELUSIVE_SPAM_DESC        = Lang.t("Elusive-Catch-Nachrichten im Chat ausblenden (Alert bleibt aktiv).", "Hide Elusive Creature catch messages in chat (alert still fires).");
    private static final String TROPHY_FISH_SPAM_DESC    = Lang.t("Fangnachrichten fuer Trophy Fish ausblenden.", "Hide catch messages for trophy fish.");
    private static final String TROPHY_FROG_SPAM_DESC    = Lang.t("Fangnachrichten fuer Trophy Frogs ausblenden.", "Hide catch messages for trophy frogs.");
    private static final String FISH_DIAMOND_DESC        = Lang.t("Diamond-Tier-Faenge auch filtern (standardmaessig sichtbar).", "Also filter diamond-tier catches (visible by default).");
    private static final String GOOD_GREAT_DESC          = Lang.t("GOOD, GREAT, OUTSTANDING und PERFECT Fangnachrichten ausblenden.", "Hide GOOD, GREAT, OUTSTANDING and PERFECT catch messages.");

    private int fishingContentHeight() {
        int h = 24
            + toggleRowHeight(FISH_ALERT_DESC)
            + toggleRowHeight(FISH_SOUND_DESC)
            + toggleRowHeight(FISH_FILTER_DESC);
        if (fishingCreatureListExpanded) {
            for (ElusiveSeaCreature creature : ElusiveSeaCreature.values()) {
                h += toggleRowHeight(creature.displayName() + ".");
            }
        }
        return h;
    }

    private void renderFishingText(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Fishing");
        y = drawToggleRow(context, viewport.x, y, Lang.t("Announce Rare Sea Creatures", "Announce Rare Sea Creatures"),
                config().isFishingRareAlertEnabled(), FISH_ALERT_DESC);
        FishingAlertSound alertSound = config().getFishingAlertSound();
        y = drawCycleRow(context, viewport.x, y, Lang.t("Alert Sound", "Alert Sound"),
                alertSound.label(), true, FISH_SOUND_DESC);
        int disabled = config().fishingDisabledCount();
        String badgeLabel = disabled == 0
                ? Lang.t("Alle AN", "All ON")
                : disabled + " " + Lang.t("AUS", "OFF");
        String filterTitle = Lang.t("Creature Filter", "Creature Filter")
                + (fishingCreatureListExpanded ? " \u25be" : " \u25b8");
        y = drawCycleRow(context, viewport.x, y, filterTitle, badgeLabel, disabled == 0, FISH_FILTER_DESC);
        if (fishingCreatureListExpanded) {
            for (ElusiveSeaCreature creature : ElusiveSeaCreature.values()) {
                boolean enabled = config().isFishingCreatureEnabled(creature.id());
                y = drawToggleRow(context, viewport.x + 16, y, creature.displayName(), enabled,
                        creature.displayName() + ".");
            }
        }
    }

    private boolean handleFishingClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setFishingRareAlertEnabled(!config().isFishingRareAlertEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(FISH_ALERT_DESC);
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            FishingAlertSound[] sounds = FishingAlertSound.values();
            int next = (config().getFishingAlertSound().ordinal() + 1) % sounds.length;
            config().setFishingAlertSound(sounds[next]);
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(FISH_SOUND_DESC);
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            fishingCreatureListExpanded = !fishingCreatureListExpanded;
            contentScrollOffset = 0;
            return true;
        }
        y += toggleRowHeight(FISH_FILTER_DESC);
        if (fishingCreatureListExpanded) {
            for (ElusiveSeaCreature creature : ElusiveSeaCreature.values()) {
                if (rowRect(viewport.x + 16, y).contains(mouseX, mouseY)) {
                    config().toggleFishingCreature(creature.id());
                    horizonClient.getConfigManager().save();
                    return true;
                }
                y += toggleRowHeight(creature.displayName() + ".");
            }
        }
        return false;
    }


    private int chatContentHeight() {
        return 24 + switch (activeChatSection) {
            case GENERAL -> toggleRowHeight(Lang.t("Alle Guild-Chat-Nachrichten ausblenden.", "Hide all guild chat messages."))
                + toggleRowHeight(Lang.t("Discord-Bridge-Nachrichten im Guild-Chat ausblenden.", "Hide Discord bridge messages in guild chat."))
                + fieldRowHeight(Lang.t("Ingame-Name des Discord-Bridge-Bots (z.B. catgirlfc).", "In-game name of the Discord bridge bot (e.g. catgirlfc)."))
                + toggleRowHeight(Lang.t("Modus: Aus, Strg+LK, Rechtsklick oder Beides.", "Mode: Off, Ctrl+LClick, Right Click or Both."))
                + toggleRowHeight(Lang.t("Alle Zeilen des Eintrags oder nur die angeklickte Zeile.", "All lines of the entry or only the clicked line."));
            case SPAM_FILTERS -> {
                int height = toggleRowHeight(Lang.t("Reduziert Dungeon- und Ability-Noise.", "Reduces dungeon and ability noise."));
                SpamFilterOption.Category prevCat = null;
                for (SpamFilterOption option : SpamFilterOption.values()) {
                    if (option.category() != prevCat) {
                        height += 24; // section title per category
                        prevCat = option.category();
                    }
                    height += toggleRowHeight(option.description());
                }
                height += 24 // "Fishing" section title
                    + toggleRowHeight(SEA_CREATURE_SPAM_DESC)
                    + toggleRowHeight(ELUSIVE_SPAM_DESC)
                    + toggleRowHeight(TROPHY_FISH_SPAM_DESC)
                    + toggleRowHeight(TROPHY_FROG_SPAM_DESC)
                    + toggleRowHeight(FISH_DIAMOND_DESC)
                    + toggleRowHeight(GOOD_GREAT_DESC);
                yield height;
            }
        };
    }

    private int scoreboardContentHeight() {
        if (scoreboardGeneralActive) {
            return 24 // section title
                + toggleRowHeight(Lang.t("Eigene Scoreboard-Leiste am unteren Bildschirmrand anzeigen.", "Show custom scoreboard bar at the bottom of the screen."))
                + 24 // section title "Globale Zeilenfilter"
                + GLOBAL_SCOREBOARD_LINES.length * scoreboardLineRowHeight();
        }
        int height = 24; // section title
        Map<String, String> known = islandDisplayLines();
        if (known.isEmpty()) {
            height += LINE_HEIGHT;
        } else {
            height += known.size() * scoreboardLineRowHeight();
        }
        return height;
    }

    private int scoreboardLineRowHeight() {
        return CARD_PADDING_TOP + LINE_HEIGHT + 6 + CARD_PADDING_BOTTOM + CARD_GAP;
    }

    private int drawScoreboardLineRow(DrawContext context, int x, int y, String lineText, boolean visible, int textColor) {
        int rowHeight = scoreboardLineRowHeight();
        drawSettingCard(context, x, y, rowHeight, visible ? 0xFF2DBA68 : 0xFF8A97A8, false);
        Rect badge = toggleBadgeRect(x, y);
        context.fill(badge.x, badge.y, badge.right(), badge.bottom(), visible ? 0xFF2DBA68 : 0xFF667487);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(visible ? Lang.t("AN", "ON") : Lang.t("AUS", "OFF")), badge.centerX(), badge.y + 4, 0xFFF7FBFF);
        int contentX = badge.right() + 10;
        if (visible) {
            drawTextLine(context, contentX, y + CARD_PADDING_TOP, lineText, textColor);
        } else {
            context.drawTextWithShadow(textRenderer, Text.literal(lineText).formatted(Formatting.STRIKETHROUGH), contentX, y + CARD_PADDING_TOP, MUTED);
        }
        return y + rowHeight;
    }

    private Rect scoreboardSubTabBarRect(Rect frame) {
        int left = sidebarRect(frame).right() + 18;
        return new Rect(left, frame.y + 40, frame.right() - left - 12, 36);
    }

    private Rect scoreboardSubTabRect(Rect bar, int index) {
        int gap = 8;
        int perRow = 6;
        int width = Math.max(60, (bar.width - gap * (perRow - 1)) / perRow);
        int row = index / perRow;
        int col = index % perRow;
        return new Rect(bar.x + col * (width + gap), bar.y + row * 18, width, 14);
    }

    private void drawConfirmationOverlay(DrawContext context, Rect frame, int accent) {
        int w = 320, h = 94;
        int ox = frame.x + (frame.width - w) / 2;
        int oy = frame.y + (frame.height - h) / 2;
        context.fill(ox, oy, ox + w, oy + h, 0xE8151C25);
        context.drawStrokedRectangle(ox, oy, w, h, HudStyle.border());
        drawTextLine(context, ox + 12, oy + 12, Lang.t("Globale Aenderung", "Global Change"), accent);
        drawTextLine(context, ox + 12, oy + 28, "\"" + pendingGlobalToggleLabel + "\"" + Lang.t(" fuer alle Islands toggeln?", " toggle for all islands?"), MUTED);
        Rect yes = confirmYesRect(frame);
        Rect no = confirmNoRect(frame);
        context.fill(yes.x, yes.y, yes.right(), yes.bottom(), 0xFF2DBA68);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(Lang.t("JA", "YES")), yes.centerX(), yes.y + 5, 0xFFF7FBFF);
        context.fill(no.x, no.y, no.right(), no.bottom(), 0xFF8A3A3A);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(Lang.t("NEIN", "NO")), no.centerX(), no.y + 5, 0xFFF7FBFF);
    }

    private void drawReloadPopup(DrawContext context, Rect frame, int accent) {
        int w = 280, h = 82;
        int ox = frame.x + (frame.width - w) / 2;
        int oy = frame.y + (frame.height - h) / 2;
        context.fill(ox, oy, ox + w, oy + h, 0xE8151C25);
        context.drawStrokedRectangle(ox, oy, w, h, HudStyle.border());
        drawTextLine(context, ox + 12, oy + 12, Lang.t("Config Reload", "Config Reload"), accent);
        drawTextLine(context, ox + 12, oy + 28, Lang.t("Konfiguration wurde neu geladen.", "Configuration reloaded successfully."), MUTED);
        int bw = 80, bx = ox + (w - bw) / 2, by = oy + h - 28;
        context.fill(bx, by, bx + bw, by + 18, 0xFF2DBA68);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("OK"), bx + bw / 2, by + 5, 0xFFF7FBFF);
    }

    private Rect confirmYesRect(Rect frame) {
        int w = 320, h = 94;
        int ox = frame.x + (frame.width - w) / 2;
        int oy = frame.y + (frame.height - h) / 2;
        return new Rect(ox + 12, oy + 58, 110, 22);
    }

    private Rect confirmNoRect(Rect frame) {
        int w = 320, h = 94;
        int ox = frame.x + (frame.width - w) / 2;
        int oy = frame.y + (frame.height - h) / 2;
        return new Rect(ox + 198, oy + 58, 110, 22);
    }

    private void drawWindowChrome(DrawContext context, Rect frame, Rect viewport, int accent) {
        context.fill(frame.x, frame.y, frame.right(), frame.bottom(), CONFIG_WINDOW);
        context.fill(viewport.x - 12, frame.y + 35, frame.right() - 1, frame.bottom() - 1, CONFIG_WINDOW);
        context.drawStrokedRectangle(frame.x, frame.y, frame.width, frame.height, HudStyle.border());
        context.fill(frame.x, frame.y, frame.right(), frame.y + 34, CONFIG_WINDOW_HEADER);
        drawTextLine(context, frame.x + 12, frame.y + 12, "HORIZON", accent);
        drawTextLine(context, searchRect(frame).x, searchRect(frame).y + 2, Lang.t("Suche: ", "Search: ") + fieldValue(globalSearchInput, inputFocus == InputFocus.GLOBAL_SEARCH), inputFocus == InputFocus.GLOBAL_SEARCH ? accent : TEXT);
        drawTextLine(context, closeRect(frame).x, closeRect(frame).y + 2, "[X]", WARNING);
    }

    private void drawHeaderMask(DrawContext context, Rect frame, int accent) {
        context.fill(frame.x + 1, frame.y + 1, frame.right() - 1, frame.y + 34, CONFIG_WINDOW_HEADER);
        context.drawStrokedRectangle(frame.x, frame.y, frame.width, frame.height, HudStyle.border());
        drawTextLine(context, frame.x + 12, frame.y + 12, "HORIZON", accent);
        drawTextLine(context, searchRect(frame).x, searchRect(frame).y + 2, Lang.t("Suche: ", "Search: ") + fieldValue(globalSearchInput, inputFocus == InputFocus.GLOBAL_SEARCH), inputFocus == InputFocus.GLOBAL_SEARCH ? accent : TEXT);
        drawTextLine(context, closeRect(frame).x, closeRect(frame).y + 2, "[X]", WARNING);
    }

    private enum Tab {
        GENERAL("General"),
        HUD("HUD"),
        DUNGEON("Dungeons"),
        PARTICLE("Particle"),
        MISC("Misc"),
        CHAT("Chat"),
        MUSIC_CONTROL("Music Control"),
        SCOREBOARD("Scoreboard"),
        INVENTORY("Inventory"),
        FISHING("Fishing");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private enum MusicSection {
        GENERAL("General"),
        SPOTIFY("Spotify"),
        YOUTUBE_MUSIC("Youtube Music");

        private final String label;

        MusicSection(String label) {
            this.label = label;
        }
    }

    private enum ChatSection {
        GENERAL("General"),
        SPAM_FILTERS("Spam Filters");

        private final String label;

        ChatSection(String label) {
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

    private enum InventorySection {
        GENERAL("General"),
        INVENTORY_BUTTONS("Inventory Buttons");

        private final String label;

        InventorySection(String label) {
            this.label = label;
        }
    }

    private enum InputFocus {
        NONE,
        CATACOMBS_LEVEL,
        HUD_ACCENT_COLOR,
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
