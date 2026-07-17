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
import de.horizon.feature.particle.ParticleFilterService;
import de.horizon.feature.revive.ReviveSource;
import de.horizon.hud.HudStyle;
import de.horizon.spotify.SpotifyService;
import de.horizon.youtube.YoutubeService;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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

    private static final String[][] CHAT_COMMANDS_LIST = {
        {"warp",     "Party Warp"},
        {"inv",      "Party Invite"},
        {"kick",     "Party Kick"},
        {"promote",  "Party Promote"},
        {"demote",   "Party Demote"},
        {"transfer", "Party Transfer"},
        {"coords",   "Coords"},
        {"here",     "Here"},
        {"fps",      "FPS"},
        {"ping",     "Ping"},
        {"tps",      "TPS"},
        {"time",     "Time"},
        {"item",     "Item"},
        {"cf",       "Coin Flip"},
        {"dice",     "Dice Roll"},
        {"8ball",    "8-Ball"},
        {"song",     "Song (Spotify)"},
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
    private DisplaySection activeDisplaySection = DisplaySection.GENERAL;
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
    private int activeSliderIndex = -1;
    private int activeMapColorIndex = -1;
    private int activeMobColorIndex = -1;
    private boolean colorPickerDragging = false;
    private boolean fishingCreatureListExpanded = false;
    private boolean chatCommandListExpanded = false;

    public HorizonConfigScreen(Screen parent, HorizonClient horizonClient) {
        super(Component.literal("Horizon"));
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
    public void onClose() {
        commitInputs();
        horizonClient.getConfigManager().save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
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
            onClose();
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
                    activeMapColorIndex = -1;
                    activeMobColorIndex = -1;
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

        if (activeTab == Tab.DISPLAY) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < DisplaySection.values().length; index++) {
                if (subTabRect(bar, index, DisplaySection.values().length).contains(click.x(), click.y())) {
                    activeDisplaySection = DisplaySection.values()[index];
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
            case DISPLAY -> handleDisplayClick(click.x(), click.y(), frame);
            case CHAT -> handleChatClick(click.x(), click.y(), frame);
            case MUSIC_CONTROL -> handleMusicClick(click.x(), click.y(), frame);
            case SCOREBOARD -> handleScoreboardClick(click.x(), click.y(), frame);
            case INVENTORY -> handleInventoryClick(click.x(), click.y(), frame);
            case FISHING -> handleFishingClick(click.x(), click.y(), frame);
        } || super.mouseClicked(click, doubled);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
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
    public boolean keyPressed(KeyEvent input) {
        // Keybind capture: next key pressed (except ESC) becomes the binding; DELETE/BACKSPACE clears it
        if (inputFocus == InputFocus.SLOT_BIND_KEY || inputFocus == InputFocus.CMD_KEY_PETS
                || inputFocus == InputFocus.CMD_KEY_EQUIPMENT || inputFocus == InputFocus.CMD_KEY_WARDROBE) {
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) {
                boolean clear = input.key() == GLFW.GLFW_KEY_DELETE || input.key() == GLFW.GLFW_KEY_BACKSPACE;
                int store = clear ? -1 : input.key();
                switch (inputFocus) {
                    case SLOT_BIND_KEY      -> config().setSlotBindKey(store);
                    case CMD_KEY_PETS       -> config().setCommandKeybindPets(store);
                    case CMD_KEY_EQUIPMENT  -> config().setCommandKeybindEquipment(store);
                    case CMD_KEY_WARDROBE   -> config().setCommandKeybindWardrobe(store);
                    default -> {}
                }
                horizonClient.getConfigManager().save();
            }
            inputFocus = InputFocus.NONE;
            return true;
        }
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
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (click.button() == 0 && activeSliderIndex >= 0 && ((activeTab == Tab.DISPLAY
                && (activeDisplaySection == DisplaySection.ANIMATIONS || activeDisplaySection == DisplaySection.NO_RENDER
                    || activeDisplaySection == DisplaySection.HELPERS))
                || (activeTab == Tab.DUNGEON && activeDungeonSection == DungeonSection.TERMINAL_SOLVER))) {
            Rect viewport = contentViewportRect(frame());
            applySliderValue(activeSliderIndex, click.x(), viewport.x);
            return true;
        }
        if (click.button() == 0 && colorPickerDragging) {
            handleColorPickerDrag(click.x(), click.y());
            return true;
        }
        if (click.button() == 0 && dragKey != null) {
            isDragging = true;
            dragCurrentMouseY = (int) click.y();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0 && colorPickerDragging) {
            colorPickerDragging = false;
            return true;
        }
        if (click.button() == 0 && activeSliderIndex >= 0) {
            activeSliderIndex = -1;
            return true;
        }
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
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

        if (activeTab == Tab.DISPLAY) {
            Rect bar = subTabBarRect(frame);
            for (int index = 0; index < DisplaySection.values().length; index++) {
                boolean active = DisplaySection.values()[index] == activeDisplaySection;
                Rect rect = subTabRect(bar, index, DisplaySection.values().length);
                drawTextLine(context, rect.x, rect.y, (active ? "[" : "") + DisplaySection.values()[index].label + (active ? "]" : ""), active ? accent : TEXT);
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
                case DISPLAY -> renderDisplayText(context, viewport);
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

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void renderGeneralText(GuiGraphicsExtractor context, Rect viewport) {
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

    private void renderHudText(GuiGraphicsExtractor context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "HUD");
        y = drawActionRow(context, viewport.x, y, Lang.t("HUD bearbeiten", "Edit HUD"), "HUD reset", Lang.t("Layout bearbeiten oder Positionen zuruecksetzen.", "Edit layout or reset positions."));
        drawHudColorRow(context, viewport.x, y);
    }

    private void renderDungeonText(GuiGraphicsExtractor context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeDungeonSection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Dungeons / General");
                y = drawToggleRow(context, viewport.x, y, "Party Finder Overlay", config().isDungeonPartyFinderOverlayEnabled(), Lang.t("Zeigt beste S+ Zeiten im Party Finder.", "Shows best S+ times in Party Finder."));
                y = drawToggleRow(context, viewport.x, y, "Rare Room Alerts", config().isDungeonRareRoomAlertsEnabled(), Lang.t("Alert fuer Trinity, Tomioka und Duncan.", "Alert for Trinity, Tomioka and Duncan."));
                y = drawToggleRow(context, viewport.x, y, "Rag Axe Notification", config().isRagAxeNotificationEnabled(), Lang.t("Rag!-Titel wenn Necron 'I no longer wish to fight...' sagt (M7).", "Shows Rag! title when Necron says 'I no longer wish to fight...' (M7)."));
                y = drawSectionTitle(context, viewport.x, y, "Tick Timer");
                drawToggleRow(context, viewport.x, y, "Damage Tick Timer", config().isTickTimerEnabled(), Lang.t("Countdown bis zum naechsten Goldor-Damage-Tick (F7 P3).", "Countdown to next Goldor damage tick (F7 P3)."));
            }
            case MOBS -> {
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Starred Mobs", "Starred Mobs"));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Non-Starred Mobs verstecken", "Hide Non-Starred Mobs"), config().isHideNonStarredMobsEnabled(), Lang.t("Blendet Nametags aller Mobs ohne Stern im Namen aus.", "Hides nametags of all mobs without a star in their name."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Starred Mobs highlighten", "Highlight Starred Mobs"), config().isHighlightStarredMobsEnabled(), Lang.t("Glow fuer Mobs mit Stern im Namen.", "Glow for mobs with a star in their name."));
                y = drawMobColorSwatchRow(context, viewport.x, y, Lang.t("Starred Mob Farbe", "Starred Mob Color"), config().getStarredMobColor(), 0);
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Weitere Mobs", "Other Mobs"));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Fledermaeuse highlighten", "Highlight Bats"), config().isHighlightBatsEnabled(), Lang.t("Fledermaeuse im Dungeon markieren.", "Highlight bats in dungeons."));
                y = drawMobColorSwatchRow(context, viewport.x, y, Lang.t("Fledermaus Farbe", "Bat Color"), config().getBatHighlightColor(), 1);
                y = drawToggleRow(context, viewport.x, y, Lang.t("Fels highlighten", "Highlight Fels"), config().isHighlightFelsEnabled(), Lang.t("Unsichtbare Fels (Endermen) im Dungeon markieren.", "Highlight invisible Fels (Endermen) in dungeons."));
                y = drawMobColorSwatchRow(context, viewport.x, y, Lang.t("Fel Farbe", "Fel Color"), config().getFelHighlightColor(), 2);
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Teamkameraden", "Teammates"));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Teammate Glow", "Teammate Glow"), config().isTeammateGlowEnabled(), Lang.t("Dungeon-Teamkameraden per Glow markieren.", "Highlight dungeon teammates with glow."));
                y = drawMobColorSwatchRow(context, viewport.x, y, "Archer", config().getClassColorArcher(), 3);
                y = drawMobColorSwatchRow(context, viewport.x, y, "Berserk", config().getClassColorBerserk(), 4);
                y = drawMobColorSwatchRow(context, viewport.x, y, "Healer", config().getClassColorHealer(), 5);
                y = drawMobColorSwatchRow(context, viewport.x, y, "Mage", config().getClassColorMage(), 6);
                y = drawMobColorSwatchRow(context, viewport.x, y, "Tank", config().getClassColorTank(), 7);
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Mimic & Prince", "Mimic & Prince"));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Mimic Erkennung", "Mimic Detection"), config().isMimicDetectionEnabled(), Lang.t("Erkennt Mimic-Kill per Tod-Event (F6+).", "Detects mimic kill via death event (F6+)."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Mimic Nachricht", "Mimic Message"), config().isMimicMessageEnabled(), Lang.t("Sendet 'Mimic killed!' in den Party-Chat.", "Sends 'Mimic killed!' to party chat."));
                drawToggleRow(context, viewport.x, y, Lang.t("Prince Nachricht", "Prince Message"), config().isPrinceMessageEnabled(), Lang.t("Sendet 'Prince killed!' in den Party-Chat.", "Sends 'Prince killed!' to party chat."));
            }
            case DOORS -> {
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Wither Doors", "Wither Doors"));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Wither Door ESP", "Wither Door ESP"), config().isWitherDoorEspEnabled(), Lang.t("Wither-Tueren im Dungeon hervorheben.", "Highlight wither doors in dungeons."));
                y = drawMobColorSwatchRow(context, viewport.x, y, Lang.t("Wither Door Farbe", "Wither Door Color"), config().getWitherDoorColor(), 8);
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Blood Doors", "Blood Doors"));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Blood Door ESP", "Blood Door ESP"), config().isBloodDoorEspEnabled(), Lang.t("Blood-Tueren im Dungeon hervorheben.", "Highlight blood doors in dungeons."));
                y = drawMobColorSwatchRow(context, viewport.x, y, Lang.t("Blood Door Farbe", "Blood Door Color"), config().getBloodDoorColor(), 9);
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Schluessel", "Keys"));
                drawToggleRow(context, viewport.x, y, Lang.t("Schluessel Highlight", "Key Highlight"), config().isDoorKeyHighlightEnabled(), Lang.t("Zeigt Box und Tracer zu Wither/Blood Keys.", "Shows box and tracer to Wither/Blood keys."));
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
            case MAP -> {
                y = drawSectionTitle(context, viewport.x, y, "Dungeon Map");
                y = drawToggleRow(context, viewport.x, y, "Dungeon Map", config().isDungeonMapEnabled(), Lang.t("Minimap im Dungeon. Groesse per HUD-Layout aenderbar.", "Dungeon minimap. Scale adjustable via HUD layout."));
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Kartenfarben", "Map Colors"));
                y = drawColorSwatchRow(context, viewport.x, y, Lang.t("Hintergrund", "Background"), config().getMapColorBackground());
                y = drawColorSwatchRow(context, viewport.x, y, Lang.t("Normal", "Normal"), config().getMapColorNormal());
                y = drawColorSwatchRow(context, viewport.x, y, "Puzzle", config().getMapColorPuzzle());
                y = drawColorSwatchRow(context, viewport.x, y, "Trap", config().getMapColorTrap());
                y = drawColorSwatchRow(context, viewport.x, y, Lang.t("Eingang", "Entrance"), config().getMapColorEntrance());
                y = drawColorSwatchRow(context, viewport.x, y, "Miniboss", config().getMapColorMiniboss());
                y = drawColorSwatchRow(context, viewport.x, y, "Blood", config().getMapColorBlood());
                y = drawColorSwatchRow(context, viewport.x, y, "Rare", config().getMapColorRare());
                y = drawSectionTitle(context, viewport.x, y, "Leap Menu");
                y = drawToggleRow(context, viewport.x, y, "Leap Menu", config().isLeapMenuEnabled(), Lang.t("Eigenes Quadranten-GUI fuer Spirit Leap.", "Custom quadrant GUI for Spirit Leap."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Ansage im Party-Chat", "Announce in Party Chat"), config().isLeapMenuAnnounce(), Lang.t("Leap-Ziel im Party-Chat ankuendigen.", "Announce leap destination in party chat."));
                String[] sortLabels = { "Klasse-Quadrant", "Klasse A-Z", "Name A-Z" };
                int sortMode = config().getLeapMenuSortMode();
                drawCycleRow(context, viewport.x, y, Lang.t("Sortierung", "Sort Mode"), sortLabels[Math.min(sortMode, 2)], true, Lang.t("Klasse-Quadrant, Klasse A-Z oder Name A-Z.", "Class quadrant, class A-Z or name A-Z."));
            }
            case PUZZLE_SOLVER -> {
                y = drawSectionTitle(context, viewport.x, y, "Puzzle Solver");
                y = drawToggleRow(context, viewport.x, y, "Puzzle Solver", config().isPuzzleSolverEnabled(), Lang.t("Loesungen fuer Blaze, Boulder, Eis, Quiz, Wasser, Creeper Beams, Three Weirdos.", "Solutions for Blaze, Boulder, Ice Fill, Quiz, Water, Creeper Beams, Three Weirdos."));
                String[] styleDE = { "Gefuellt", "Umriss", "Gefuellt + Umriss" };
                String[] styleEN = { "Filled", "Outline", "Filled + Outline" };
                int ps = Math.max(0, Math.min(2, config().getPuzzleSolverStyle()));
                drawCycleRow(context, viewport.x, y, Lang.t("Stil", "Style"), Lang.t(styleDE[ps], styleEN[ps]), config().isPuzzleSolverEnabled(), Lang.t("Render-Stil der Loesung.", "Render style of the solution."));
            }
            case TERMINAL_SOLVER -> {
                y = drawSectionTitle(context, viewport.x, y, "Terminal Solver");
                y = drawToggleRow(context, viewport.x, y, "Terminal Solver", config().isTerminalSolverEnabled(), Lang.t("Markiert korrekte Slots in F7-Terminals (Panes, Rubix, Order, Starts With, Select All).", "Highlights correct slots in F7 terminals (Panes, Rubix, Order, Starts With, Select All)."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Falsche Klicks blockieren", "Block Wrong Clicks"), config().isTerminalSolverBlockWrongClicks(), Lang.t("Falsche Terminal-Klicks unterdrucken.", "Suppress incorrect terminal clicks."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Custom-Modus", "Custom Mode"), config().isTerminalSolverCustomMode(), Lang.t("Nicht-relevante Slots vollstaendig ausblenden statt nur abzudunkeln.", "Fully hide non-relevant slots instead of just dimming them."));
                drawSliderRow(context, viewport.x, y, "GUI Scale", config().getTerminalGuiScale(), 0.5, 3.0, Lang.t("Skalierung des Terminal-GUIs.", "Scale of the terminal GUI."));
            }
            case BOSS -> {
                y = drawSectionTitle(context, viewport.x, y, "Boss Solver (F7 P3)");
                y = drawToggleRow(context, viewport.x, y, "Simon Says", config().isSimonSaysEnabled(), Lang.t("Hebt die korrekte Schaltflaechen-Reihenfolge beim Goldor-Device hervor.", "Highlights the correct button sequence for the Goldor device."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Falsche Klicks blockieren", "Block Wrong Clicks"), config().isSimonSaysBlockWrongClicks(), Lang.t("Blockiert Klicks auf falsche Simon-Says-Knoepfe.", "Blocks clicks on incorrect Simon Says buttons."));
                y = drawToggleRow(context, viewport.x, y, "Arrow Align", config().isArrowAlignEnabled(), Lang.t("Zeigt Klickanzahl fuer jede Pfeil-Bilderrahmen.", "Shows click count for each arrow item frame."));
                y = drawToggleRow(context, viewport.x, y, "Arrow Device (I4)", config().isSharpShooterEnabled(), Lang.t("Hebt getroffene Smaragdbloecke beim Arrow-Device hervor.", "Highlights hit emerald blocks at the arrow device."));
                y = drawToggleRow(context, viewport.x, y, "Purple Pad Timer", config().isPurplePadTimerEnabled(), Lang.t("Countdown bis zum Purple-Pad-Zeitpunkt (F7 P2).", "Countdown until purple pad timing (F7 P2)."));
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Allgemein", "General"));
                y = drawToggleRow(context, viewport.x, y, "Blood Camper", config().isBloodCamperEnabled(), Lang.t("Zeigt Blood-Room-Fortschritt und Timer an.", "Shows blood room wave progress and timer."));
                y = drawToggleRow(context, viewport.x, y, "Dungeon Score", config().isDungeonScoreEnabled(), Lang.t("Zeigt geschaetzte Dungeon-Punktzahl als HUD an.", "Shows estimated dungeon score as HUD overlay."));
                y = drawSectionTitle(context, viewport.x, y, "M7 Dragons (P5)");
                y = drawToggleRow(context, viewport.x, y, "Dragon Overlay", config().isDragonEnabled(), Lang.t("Zeigt Dragon-Spawn-Prioritaet, Boxen und Timer in M7 P5.", "Shows dragon spawn priority, boxes and timer in M7 P5."));
                y = drawToggleRow(context, viewport.x, y, "Dragon Boxes", config().isDragonBoxes(), Lang.t("Zeigt farbige Boxen an den Spawn-Positionen.", "Shows colored boxes at spawn positions."));
                y = drawToggleRow(context, viewport.x, y, "Dragon Timer", config().isDragonTimer(), Lang.t("Zeigt Countdown bis zum Spawn.", "Shows countdown until spawn."));
                y = drawToggleRow(context, viewport.x, y, "Spawn Alert", config().isDragonSpawnAlert(), Lang.t("Zeigt Spawn-Warnung im Chat.", "Shows spawn alert in chat."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Prioritaet", "Priority"), config().isDragonPriority(), Lang.t("Zeigt empfohlene Kill-Reihenfolge.", "Shows recommended kill order."));
                y = drawSectionTitle(context, viewport.x, y, "M7 Relic Timer");
                drawToggleRow(context, viewport.x, y, "Relic Timer", config().isRelicTimerEnabled(), Lang.t("Countdown bis zum Relic-Spawn nach Necron.", "Countdown until relic spawn after Necron."));
            }
            case FLOOR_SPECIALS -> {
                y = drawSectionTitle(context, viewport.x, y, "F4 / M4 Spirit Bear");
                y = drawToggleRow(context, viewport.x, y, "Spirit Bear Timer", config().isSpiritBearTimerEnabled(), Lang.t("Fortschritt und Countdown bis zum Spirit Bear Spawn.", "Progress and countdown until Spirit Bear spawn."));
                y = drawToggleRow(context, viewport.x, y, "Spirit Bear Highlight", config().isSpiritBearHighlightEnabled(), Lang.t("Spirit Bear per Glow hervorheben.", "Highlight Spirit Bear with glow."));
                drawMobColorSwatchRow(context, viewport.x, y, Lang.t("Spirit Bear Farbe", "Spirit Bear Color"), config().getSpiritBearHighlightColor(), 10);
            }
        }
    }

    private static final String BREAK_PARTICLES_DESC = Lang.t("Block-Abbauen-Partikel anzeigen.", "Show block breaking particles.");

    private void renderParticleText(GuiGraphicsExtractor context, Rect viewport) {
        int y = viewport.y;
        y = drawToggleRow(context, viewport.x, y, "Break Particles", config().isBreakParticlesEnabled(), BREAK_PARTICLES_DESC);
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

    private void renderMiscText(GuiGraphicsExtractor context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Misc");
        y = drawToggleRow(context, viewport.x, y, Lang.t("Zeit HUD", "Time HUD"), config().isTimeHudEnabled(), Lang.t("Lokale Uhrzeit als Overlay.", "Local time as overlay."));
        y = drawToggleRow(context, viewport.x, y, "FPS / TPS / Ping", config().isPerformanceHudEnabled(), Lang.t("Performance-Overlay.", "Performance overlay."));
        y = drawToggleRow(context, viewport.x, y, "System HUD", config().isSystemHudEnabled(), Lang.t("CPU / GPU / Temperaturen.", "CPU / GPU / Temperatures."));
        y = drawToggleRow(context, viewport.x, y, "Defense Bar", config().isHideDefenseBar(), Lang.t("Blendet die Vanilla-Ruestungsanzeige aus.", "Hides the vanilla armor display."));
        drawToggleRow(context, viewport.x, y, Lang.t("Kompakte Herzen", "Compact Hearts"), config().isCompactHypixelHealthEnabled(), Lang.t("Fasst Hypixel-Herzen kompakt in einer Reihe zusammen.", "Compacts Hypixel hearts into a single row."));
    }

    private void renderDisplayText(GuiGraphicsExtractor context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeDisplaySection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Anzeige / General");
                drawToggleRow(context, viewport.x, y, "16:9 Pillarbox", config().isPillarboxEnabled(),
                    Lang.t("Begrenzt die Spielansicht auf 16:9 mit schwarzen Balken links und rechts (Samsung Odyssey G9).",
                           "Limits game view to 16:9 with black bars on the sides (Samsung Odyssey G9)."));
            }
            case ANIMATIONS -> {
                y = drawSectionTitle(context, viewport.x, y, "Anzeige / Animationen");
                y = drawSliderRow(context, viewport.x, y, "Position X", config().getItemPositionX(), -1.5, 1.5,
                    Lang.t("Horizontale Position des gehaltenen Items.", "Horizontal position of the held item."));
                y = drawSliderRow(context, viewport.x, y, "Position Y", config().getItemPositionY(), -1.5, 1.5,
                    Lang.t("Vertikale Position des gehaltenen Items.", "Vertical position of the held item."));
                y = drawSliderRow(context, viewport.x, y, "Position Z", config().getItemPositionZ(), -1.5, 1.5,
                    Lang.t("Tiefe des gehaltenen Items.", "Depth of the held item."));
                y = drawSliderRow(context, viewport.x, y, Lang.t("Groesse", "Scale"), config().getItemScale(), 0.1, 2.0,
                    Lang.t("Skalierung des gehaltenen Items.", "Scale of the held item."));
                drawSliderRow(context, viewport.x, y, Lang.t("Schlaggeschwindigkeit", "Swing Speed"), config().getSwingSpeed(), 0.1, 4.0,
                    Lang.t("Geschwindigkeit der Schlaganimation.", "Speed of the swing animation."));
            }
            case NO_RENDER -> {
                y = drawSectionTitle(context, viewport.x, y, "Anzeige / NoRender");
                y = drawToggleRow(context, viewport.x, y, Lang.t("Feuer-Overlay", "Fire Overlay"), !config().isFireOverlayDisabled(),
                    Lang.t("Feuer-Overlay ausblenden wenn man brennt.", "Disable the fire overlay when on fire."));
                drawSliderRow(context, viewport.x, y, Lang.t("Hurtcam Intensitaet", "Hurtcam Intensity"),
                    config().getHurtCamIntensity(), 0.0, 1.0,
                    Lang.t("0 = komplett aus, 1 = normal.", "0 = completely off, 1 = normal."));
            }
            case HELPERS -> {
                y = drawSectionTitle(context, viewport.x, y, "Anzeige / Helpers");
                y = drawToggleRow(context, viewport.x, y, "Etherwarp Helper", config().isEtherwarpEnabled(), Lang.t("Zeigt Teleport-Ziel fuer Aspect of the Void/Dragons.", "Shows teleport destination for Aspect of the Void/Dragons."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Nur beim Schleichen", "Sneak Only"), config().isEtherwarpSneakOnly(), Lang.t("Box nur beim Schleichen anzeigen.", "Only show box while sneaking."));
                String[] etherStyleDE = { "Gefuellt", "Umriss", "Gefuellt + Umriss" };
                String[] etherStyleEN = { "Filled", "Outline", "Filled + Outline" };
                int etherStyle = Math.max(0, Math.min(2, config().getEtherwarpRenderStyle()));
                y = drawCycleRow(context, viewport.x, y, Lang.t("Stil", "Style"), Lang.t(etherStyleDE[etherStyle], etherStyleEN[etherStyle]), true, Lang.t("Render-Stil der Ziel-Box.", "Render style of the destination box."));
                y = drawToggleRow(context, viewport.x, y, "Depth Check", config().isEtherwarpDepthCheck(), Lang.t("Ziel-Box durch Waende ausblenden.", "Hide destination box through walls."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Etherwarp Sound", "Etherwarp Sound"), config().isEtherwarpSoundEnabled(), Lang.t("Sound beim Teleportieren abspielen.", "Play sound when teleporting."));
                String[] etherSoundDE = { "Ender Drache", "Chorus Fruit" };
                String[] etherSoundEN = { "Ender Dragon", "Chorus Fruit" };
                int etherSoundIdx = config().getEtherwarpSoundIndex();
                y = drawCycleRow(context, viewport.x, y, "Sound", Lang.t(etherSoundDE[etherSoundIdx], etherSoundEN[etherSoundIdx]), config().isEtherwarpSoundEnabled(), Lang.t("Sound-Effekt fuer Etherwarp.", "Sound effect for Etherwarp."));
                y = drawSliderRow(context, viewport.x, y, Lang.t("Lautstaerke", "Volume"), config().getEtherwarpSoundVolume(), 0.0, 2.0, Lang.t("Lautstaerke des Sounds (0-2).", "Volume of the sound (0-2)."));
                drawSliderRow(context, viewport.x, y, "Pitch", config().getEtherwarpSoundPitch(), 0.0, 2.0, Lang.t("Tonhoehe des Sounds (0-2).", "Pitch of the sound (0-2)."));
            }
        }
    }

    private boolean handleDisplayClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        return switch (activeDisplaySection) {
            case GENERAL -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setPillarboxEnabled(!config().isPillarboxEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                yield false;
            }
            case ANIMATIONS -> {
                for (int i = 0; i < 5; i++) {
                    if (sliderRect(viewport.x, y).contains(mouseX, mouseY)) {
                        activeSliderIndex = i;
                        applySliderValue(i, mouseX, viewport.x);
                        yield true;
                    }
                    y += sliderRowHeight();
                }
                yield false;
            }
            case NO_RENDER -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setFireOverlayDisabled(!config().isFireOverlayDisabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Feuer-Overlay ausblenden wenn man brennt.", "Disable the fire overlay when on fire."));
                if (sliderRect(viewport.x, y).contains(mouseX, mouseY)) {
                    activeSliderIndex = 10;
                    applySliderValue(10, mouseX, viewport.x);
                    yield true;
                }
                yield false;
            }
            case HELPERS -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setEtherwarpEnabled(!config().isEtherwarpEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt Teleport-Ziel fuer Aspect of the Void/Dragons.", "Shows teleport destination for Aspect of the Void/Dragons."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setEtherwarpSneakOnly(!config().isEtherwarpSneakOnly());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Box nur beim Schleichen anzeigen.", "Only show box while sneaking."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setEtherwarpRenderStyle((config().getEtherwarpRenderStyle() + 1) % 3);
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Render-Stil der Ziel-Box.", "Render style of the destination box."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setEtherwarpDepthCheck(!config().isEtherwarpDepthCheck());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Ziel-Box durch Waende ausblenden.", "Hide destination box through walls."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setEtherwarpSoundEnabled(!config().isEtherwarpSoundEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Sound beim Teleportieren abspielen.", "Play sound when teleporting."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setEtherwarpSoundIndex((config().getEtherwarpSoundIndex() + 1) % 2);
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Sound-Effekt fuer Etherwarp.", "Sound effect for Etherwarp."));
                if (sliderRect(viewport.x, y).contains(mouseX, mouseY)) {
                    activeSliderIndex = 11;
                    applySliderValue(11, mouseX, viewport.x);
                    yield true;
                }
                y += sliderRowHeight();
                if (sliderRect(viewport.x, y).contains(mouseX, mouseY)) {
                    activeSliderIndex = 12;
                    applySliderValue(12, mouseX, viewport.x);
                    yield true;
                }
                yield false;
            }
        };
    }

    private int displayContentHeight() {
        return 24 + switch (activeDisplaySection) {
            case GENERAL -> toggleRowHeight(Lang.t(
                "Begrenzt die Spielansicht auf 16:9 mit schwarzen Balken links und rechts (Samsung Odyssey G9).",
                "Limits game view to 16:9 with black bars on the sides (Samsung Odyssey G9)."));
            case ANIMATIONS -> 5 * sliderRowHeight();
            case NO_RENDER -> toggleRowHeight(Lang.t("Feuer-Overlay ausblenden wenn man brennt.", "Disable the fire overlay when on fire."))
                + sliderRowHeight();
            case HELPERS -> toggleRowHeight(Lang.t("Zeigt Teleport-Ziel fuer Aspect of the Void/Dragons.", "Shows teleport destination for Aspect of the Void/Dragons."))
                + toggleRowHeight(Lang.t("Box nur beim Schleichen anzeigen.", "Only show box while sneaking."))
                + toggleRowHeight(Lang.t("Render-Stil der Ziel-Box.", "Render style of the destination box."))
                + toggleRowHeight(Lang.t("Ziel-Box durch Waende ausblenden.", "Hide destination box through walls."))
                + toggleRowHeight(Lang.t("Sound beim Teleportieren abspielen.", "Play sound when teleporting."))
                + toggleRowHeight(Lang.t("Sound-Effekt fuer Etherwarp.", "Sound effect for Etherwarp."))
                + sliderRowHeight()
                + sliderRowHeight();
        };
    }

    private void renderChatText(GuiGraphicsExtractor context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeChatSection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Chat / General");
                y = drawToggleRow(context, viewport.x, y, Lang.t("Guild Chat verstecken", "Hide Guild Chat"), config().isGuildChatHidden(), Lang.t("Alle Guild-Chat-Nachrichten ausblenden.", "Hide all guild chat messages."));
                y = drawToggleRow(context, viewport.x, y, Lang.t("Bridge verstecken", "Hide Bridge"), config().isChatBridgeHidden(), Lang.t("Discord-Bridge-Nachrichten im Guild-Chat ausblenden.", "Hide Discord bridge messages in guild chat."));
                y = drawFieldRow(context, viewport.x, y, "Bridge Bot Name", chatBridgeBotNameInput, inputFocus == InputFocus.CHAT_BRIDGE_BOT_NAME, Lang.t("Ingame-Name des Discord-Bridge-Bots (z.B. catgirlfc).", "In-game name of the Discord bridge bot (e.g. catgirlfc)."));
                ChatCopyMode copyMode = config().getChatCopyMode();
                y = drawCycleRow(context, viewport.x, y, Lang.t("Nachrichten kopieren", "Copy Messages"), copyMode.label(), copyMode != ChatCopyMode.OFF, Lang.t("Modus: Aus, Strg+LK, Rechtsklick oder Beides.", "Mode: Off, Ctrl+LClick, Right MouseButtonEvent or Both."));
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
            case CHAT_COMMANDS -> {
                y = drawSectionTitle(context, viewport.x, y, "Chat / Chat Commands");
                y = drawToggleRow(context, viewport.x, y, "Chat Commands", config().isChatCommandsEnabled(), Lang.t("!-Befehle im Party/Gilde/Privat-Chat ausfuehren.", "Execute !-commands in party/guild/private chat."));
                y = drawToggleRow(context, viewport.x, y, "Party", config().isChatCommandsParty(), Lang.t("!-Befehle im Party-Chat erlauben.", "Allow !-commands in party chat."));
                y = drawToggleRow(context, viewport.x, y, "Guild", config().isChatCommandsGuild(), Lang.t("!-Befehle im Gilde-Chat erlauben.", "Allow !-commands in guild chat."));
                y = drawToggleRow(context, viewport.x, y, "Private", config().isChatCommandsPrivate(), Lang.t("!-Befehle in Privatnachrichten erlauben.", "Allow !-commands in private messages."));
                long disabledCount = java.util.Arrays.stream(CHAT_COMMANDS_LIST).filter(e -> !config().isChatCommandEnabled(e[0])).count();
                String cmdBadge = disabledCount == 0 ? Lang.t("Alle AN", "All ON") : disabledCount + " " + Lang.t("AUS", "OFF");
                String cmdLabel = Lang.t("Befehle", "Commands") + (chatCommandListExpanded ? " \u25be" : " \u25b8");
                y = drawCycleRow(context, viewport.x, y, cmdLabel, cmdBadge, disabledCount == 0, Lang.t("Einzelne !-Befehle an-/ausschalten.", "Enable/disable individual !-commands."));
                if (chatCommandListExpanded) {
                    for (String[] entry : CHAT_COMMANDS_LIST) {
                        boolean en = config().isChatCommandEnabled(entry[0]);
                        y = drawToggleRow(context, viewport.x + 16, y, "!" + entry[0] + " — " + entry[1], en, entry[1]);
                    }
                }
            }
            case SHORTCUTS -> {
                y = drawSectionTitle(context, viewport.x, y, "Chat / Shortcuts");
                y = drawToggleRow(context, viewport.x, y, Lang.t("Command Shortcuts", "Command Shortcuts"), config().isCommandShortcutsEnabled(),
                    Lang.t("/f1-/f7, /m1-/m7, /d, /dh als Kurzbefehle verwenden.", "Use /f1-/f7, /m1-/m7, /d, /dh as command shortcuts."));
                drawTextLine(context, viewport.x + DESCRIPTION_INDENT, y, "/f1-/f7  \u2192  /joininstance CATACOMBS_FLOOR_...", MUTED);
                y += LINE_HEIGHT;
                drawTextLine(context, viewport.x + DESCRIPTION_INDENT, y, "/m1-/m7  \u2192  /joininstance MASTER_CATACOMBS_FLOOR_...", MUTED);
                y += LINE_HEIGHT;
                drawTextLine(context, viewport.x + DESCRIPTION_INDENT, y, "/d, /dh  \u2192  /warp dungeon_hub", MUTED);
            }
        }
    }

    private void renderScoreboardText(GuiGraphicsExtractor context, Rect viewport) {
        if (scoreboardGeneralActive) {
            renderGeneralScoreboardText(context, viewport);
        } else {
            renderIslandScoreboardText(context, viewport);
        }
    }

    private void renderGeneralScoreboardText(GuiGraphicsExtractor context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionTitle(context, viewport.x, y, "Scoreboard / General");
        y = drawToggleRow(context, viewport.x, y, "Custom Scoreboard", config().isCustomScoreboardEnabled(), Lang.t("Eigene Scoreboard-Leiste am unteren Bildschirmrand anzeigen.", "Show custom scoreboard bar at the bottom of the screen."));
        y = drawSectionTitle(context, viewport.x, y, Lang.t("Globale Zeilenfilter", "Global Line Filters"));
        for (String[] entry : GLOBAL_SCOREBOARD_LINES) {
            boolean visible = !config().isScoreboardGlobalLineHidden(entry[0]);
            y = drawScoreboardLineRow(context, viewport.x, y, entry[1], visible, HorizonConfig.scoreboardKeyColor(entry[0]));
        }
    }

    private void renderIslandScoreboardText(GuiGraphicsExtractor context, Rect viewport) {
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

    private void renderMusicText(GuiGraphicsExtractor context, Rect viewport) {
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

    private void renderInventoryText(GuiGraphicsExtractor context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        switch (activeInventorySection) {
            case GENERAL -> {
                y = drawSectionTitle(context, viewport.x, y, "Inventory / General");
                y = drawToggleRow(context, viewport.x, y,
                        "Wardrobe Keybinds",
                        config().isWardrobeKeybindsEnabled(),
                        Lang.t("Pfeiltasten und Zifferntasten im Wardrobe-Screen.", "Arrow keys and number keys in the wardrobe screen."));
                y = drawToggleRow(context, viewport.x, y,
                        "Slot Binds",
                        config().isSlotBindsEnabled(),
                        Lang.t("Shift-Klick tauscht gebundene Inventory-Slots.", "Shift-click swaps bound inventory slots."));
                boolean capturingSlotBind = inputFocus == InputFocus.SLOT_BIND_KEY;
                y = drawCycleRow(context, viewport.x, y,
                        "Slot Bind Key",
                        capturingSlotBind ? Lang.t("Taste druecken...", "Press key...") : keyName(config().getSlotBindKey()),
                        config().getSlotBindKey() >= 0 || capturingSlotBind,
                        Lang.t("Taste die im Inventar ueber Slots gedrueckt wird um Binds zu setzen.", "Key pressed over a slot to create/remove a bind."));
                y = drawSectionTitle(context, viewport.x, y, Lang.t("Command Keybinds", "Command Keybinds"));
                y = drawCycleRow(context, viewport.x, y,
                        "/pets",
                        inputFocus == InputFocus.CMD_KEY_PETS ? Lang.t("Taste druecken...", "Press key...") : keyName(config().getCommandKeybindPets()),
                        config().getCommandKeybindPets() >= 0 || inputFocus == InputFocus.CMD_KEY_PETS,
                        "/pets");
                y = drawCycleRow(context, viewport.x, y,
                        "/equipment",
                        inputFocus == InputFocus.CMD_KEY_EQUIPMENT ? Lang.t("Taste druecken...", "Press key...") : keyName(config().getCommandKeybindEquipment()),
                        config().getCommandKeybindEquipment() >= 0 || inputFocus == InputFocus.CMD_KEY_EQUIPMENT,
                        "/equipment");
                drawCycleRow(context, viewport.x, y,
                        "/wardrobe",
                        inputFocus == InputFocus.CMD_KEY_WARDROBE ? Lang.t("Taste druecken...", "Press key...") : keyName(config().getCommandKeybindWardrobe()),
                        config().getCommandKeybindWardrobe() >= 0 || inputFocus == InputFocus.CMD_KEY_WARDROBE,
                        "/wardrobe");
            }
            case INVENTORY_BUTTONS -> {
                y = drawSectionTitle(context, viewport.x, y, "Inventory / Inventory Buttons");
                int count = config().getInventoryButtons().size();
                String btnCountLabel = count + " Button" + (count == 1 ? "" : "s") + " konfiguriert";
                y = drawToggleRow(context, viewport.x, y,
                        "Inventory Buttons",
                        config().isInventoryButtonsEnabled(),
                        Lang.t("Zeigt konfigurierte Buttons um das Inventar herum.", "Shows configured buttons around the inventory."));
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
                    config().setWardrobeKeybindsEnabled(!config().isWardrobeKeybindsEnabled());
                    horizonClient.getConfigManager().save();
                    return true;
                }
                y += toggleRowHeight(Lang.t("Pfeiltasten und Zifferntasten im Wardrobe-Screen.", "Arrow keys and number keys in the wardrobe screen."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setSlotBindsEnabled(!config().isSlotBindsEnabled());
                    horizonClient.getConfigManager().save();
                    return true;
                }
                y += toggleRowHeight(Lang.t("Shift-Klick tauscht gebundene Inventory-Slots.", "Shift-click swaps bound inventory slots."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    inputFocus = InputFocus.SLOT_BIND_KEY;
                    return true;
                }
                y += toggleRowHeight(Lang.t("Taste die im Inventar ueber Slots gedrueckt wird um Binds zu setzen.", "Key pressed over a slot to create/remove a bind."));
                y += 24; // "Command Keybinds" section title
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    inputFocus = InputFocus.CMD_KEY_PETS;
                    return true;
                }
                y += toggleRowHeight("/pets");
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    inputFocus = InputFocus.CMD_KEY_EQUIPMENT;
                    return true;
                }
                y += toggleRowHeight("/equipment");
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    inputFocus = InputFocus.CMD_KEY_WARDROBE;
                    return true;
                }
            }
            case INVENTORY_BUTTONS -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setInventoryButtonsEnabled(!config().isInventoryButtonsEnabled());
                    horizonClient.getConfigManager().save();
                    return true;
                }
                y += toggleRowHeight(Lang.t("Zeigt konfigurierte Buttons um das Inventar herum.", "Shows configured buttons around the inventory."));
                if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
                    minecraft.setScreen(new InventoryButtonLayoutScreen(this, horizonClient));
                    return true;
                }
            }
        }
        return false;
    }

    private int inventoryContentHeight() {
        return 24 + switch (activeInventorySection) {
            case GENERAL -> toggleRowHeight(Lang.t("Pfeiltasten und Zifferntasten im Wardrobe-Screen.", "Arrow keys and number keys in the wardrobe screen."))
                + toggleRowHeight(Lang.t("Shift-Klick tauscht gebundene Inventory-Slots.", "Shift-click swaps bound inventory slots."))
                + toggleRowHeight(Lang.t("Taste die im Inventar ueber Slots gedrueckt wird um Binds zu setzen.", "Key pressed over a slot to create/remove a bind."))
                + 24 // "Command Keybinds" section title
                + toggleRowHeight("/pets")
                + toggleRowHeight("/equipment")
                + toggleRowHeight("/wardrobe");
            case INVENTORY_BUTTONS -> {
                int count = config().getInventoryButtons().size();
                String desc = count + " Button" + (count == 1 ? "" : "s") + " konfiguriert. Klicke um Buttons zu platzieren und zu konfigurieren.";
                yield toggleRowHeight(Lang.t("Zeigt konfigurierte Buttons um das Inventar herum.", "Shows configured buttons around the inventory."))
                    + actionRowHeight(desc);
            }
        };
    }

    private static String keyName(int keyCode) {
        if (keyCode < 0) return Lang.t("Keine", "None");
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null && !name.isBlank()) return name.toUpperCase(java.util.Locale.ROOT);
        return "KEY_" + keyCode;
    }

    private void renderSearchResults(GuiGraphicsExtractor context, Rect viewport) {
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

    private int drawSectionTitle(GuiGraphicsExtractor context, int x, int y, String title) {
        drawTextLine(context, x, y, title, accentColor());
        context.fill(x, y + 14, x + CONTENT_ROW_WIDTH, y + 15, HudStyle.border());
        return y + 24;
    }

    private int drawToggleRow(GuiGraphicsExtractor context, int x, int y, String title, boolean enabled, String description) {
        int rowHeight = toggleRowHeight(description);
        drawSettingCard(context, x, y, rowHeight, enabled ? 0xFF2DBA68 : 0xFF8A97A8, false);
        Rect badge = toggleBadgeRect(x, y);
        context.fill(badge.x, badge.y, badge.right(), badge.bottom(), enabled ? 0xFF2DBA68 : 0xFF667487);
        context.centeredText(font, Component.literal(enabled ? Lang.t("AN", "ON") : Lang.t("AUS", "OFF")), badge.centerX(), badge.y + 4, 0xFFF7FBFF);
        int contentX = badge.right() + 10;
        int contentWidth = Math.max(80, CONTENT_ROW_WIDTH - (contentX - x) - 10);
        drawTextLine(context, contentX, y + CARD_PADDING_TOP, title, TEXT);
        drawWrappedText(context, contentX, y + CARD_PADDING_TOP + LINE_HEIGHT, description, contentWidth, MUTED);
        return y + rowHeight;
    }

    private static final int SLIDER_WIDTH = 200;
    private static final int SLIDER_HEIGHT = 10;
    private static final int SLIDER_ROW_HEIGHT = CARD_PADDING_TOP + LINE_HEIGHT + SLIDER_HEIGHT + 8 + CARD_PADDING_BOTTOM + CARD_GAP;

    private int drawSliderRow(GuiGraphicsExtractor context, int x, int y, String title, double value, double min, double max, String description) {
        int rowHeight = sliderRowHeight();
        drawSettingCard(context, x, y, rowHeight, accentColor(), false);
        String formatted = String.format("%.2f", value);
        drawTextLine(context, x + 4, y + CARD_PADDING_TOP, title + ": " + formatted, TEXT);
        int sliderX = x + CONTENT_ROW_WIDTH - SLIDER_WIDTH - 10;
        int sliderY = y + CARD_PADDING_TOP;
        context.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + SLIDER_HEIGHT, 0xFF3A3F4B);
        double fraction = (value - min) / (max - min);
        int thumbX = sliderX + (int) (fraction * (SLIDER_WIDTH - 6));
        context.fill(thumbX, sliderY, thumbX + 6, sliderY + SLIDER_HEIGHT, accentColor());
        drawWrappedText(context, x + DESCRIPTION_INDENT, y + CARD_PADDING_TOP + LINE_HEIGHT + 2, description, CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10, MUTED);
        return y + rowHeight;
    }

    private int sliderRowHeight() {
        return SLIDER_ROW_HEIGHT;
    }

    private Rect sliderRect(int x, int y) {
        int sliderX = x + CONTENT_ROW_WIDTH - SLIDER_WIDTH - 10;
        return new Rect(sliderX, y + CARD_PADDING_TOP, SLIDER_WIDTH, SLIDER_HEIGHT);
    }

    private double sliderValueFromMouse(double mouseX, int viewportX, double min, double max) {
        int sliderX = viewportX + CONTENT_ROW_WIDTH - SLIDER_WIDTH - 10;
        double fraction = (mouseX - sliderX) / SLIDER_WIDTH;
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        double raw = min + fraction * (max - min);
        return Math.round(raw * 100.0) / 100.0;
    }

    private void applySliderValue(int index, double mouseX, int viewportX) {
        switch (index) {
            case 0 -> config().setItemPositionX(sliderValueFromMouse(mouseX, viewportX, -1.5, 1.5));
            case 1 -> config().setItemPositionY(sliderValueFromMouse(mouseX, viewportX, -1.5, 1.5));
            case 2 -> config().setItemPositionZ(sliderValueFromMouse(mouseX, viewportX, -1.5, 1.5));
            case 3 -> config().setItemScale(sliderValueFromMouse(mouseX, viewportX, 0.1, 2.0));
            case 4 -> config().setSwingSpeed(sliderValueFromMouse(mouseX, viewportX, 0.1, 4.0));
            case 10 -> config().setHurtCamIntensity((float) sliderValueFromMouse(mouseX, viewportX, 0.0, 1.0));
            case 11 -> config().setEtherwarpSoundVolume((float) sliderValueFromMouse(mouseX, viewportX, 0.0, 2.0));
            case 12 -> config().setEtherwarpSoundPitch((float) sliderValueFromMouse(mouseX, viewportX, 0.0, 2.0));
            case 20 -> config().setTerminalGuiScale((float) sliderValueFromMouse(mouseX, viewportX, 0.5, 3.0));
        }
        horizonClient.getConfigManager().save();
    }

    private int drawCycleRow(GuiGraphicsExtractor context, int x, int y, String title, String modeLabel, boolean active, String description) {
        int rowHeight = toggleRowHeight(description);
        drawSettingCard(context, x, y, rowHeight, active ? 0xFF2DBA68 : 0xFF8A97A8, false);
        Rect badge = cycleBadgeRect(x, y);
        context.fill(badge.x, badge.y, badge.right(), badge.bottom(), active ? 0xFF2DBA68 : 0xFF667487);
        context.centeredText(font, Component.literal(modeLabel), badge.centerX(), badge.y + 4, 0xFFF7FBFF);
        int contentX = badge.right() + 10;
        int contentWidth = Math.max(80, CONTENT_ROW_WIDTH - (contentX - x) - 10);
        drawTextLine(context, contentX, y + CARD_PADDING_TOP, title, TEXT);
        drawWrappedText(context, contentX, y + CARD_PADDING_TOP + LINE_HEIGHT, description, contentWidth, MUTED);
        return y + rowHeight;
    }

    private Rect cycleBadgeRect(int x, int y) {
        return new Rect(x, y + CARD_PADDING_TOP - 1, 54, 18);
    }

    private int drawActionRow(GuiGraphicsExtractor context, int x, int y, String left, String right, String description) {
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

    private int drawFieldRow(GuiGraphicsExtractor context, int x, int y, String title, String value, boolean focused, String description) {
        int rowHeight = fieldRowHeight(description);
        drawSettingCard(context, x, y, rowHeight, focused ? HudStyle.accent() : HudStyle.border(), focused);
        drawTextLine(context, x, y + CARD_PADDING_TOP, title + ": " + fieldValue(value, focused), TEXT);
        drawWrappedText(context, x + DESCRIPTION_INDENT, y + CARD_PADDING_TOP + LINE_HEIGHT, description, CONTENT_ROW_WIDTH - DESCRIPTION_INDENT - 10, MUTED);
        return y + rowHeight;
    }

    private int drawHudColorRow(GuiGraphicsExtractor context, int x, int y) {
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

    private int drawNumberRow(GuiGraphicsExtractor context, int x, int y, String title, String value, boolean focused, String description) {
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

    private static final int COLOR_SWATCH_ROW_HEIGHT = CARD_PADDING_TOP + LINE_HEIGHT + CARD_PADDING_BOTTOM + CARD_GAP;
    private static final int COLOR_PICKER_WIDTH = 200;
    private static final int HUE_BAR_HEIGHT = 12;
    private static final int SV_FIELD_HEIGHT = 80;
    private static final int COLOR_PICKER_EXPANDED_HEIGHT = COLOR_SWATCH_ROW_HEIGHT + HUE_BAR_HEIGHT + 4 + SV_FIELD_HEIGHT + 4;

    private static final int HUE_STEP = 2;
    private static final int SV_STEP = 4;

    private int drawColorSwatchRow(GuiGraphicsExtractor context, int x, int y, String label, int currentColor) {
        boolean expanded = activeMapColorIndex >= 0 && isMatchingMapColorRow(label);
        int rowHeight = expanded ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
        drawSettingCard(context, x, y, rowHeight, currentColor | 0xFF000000, false);
        int swatchX = x + 4;
        int swatchY = y + CARD_PADDING_TOP;
        context.fill(swatchX, swatchY, swatchX + 12, swatchY + 12, currentColor | 0xFF000000);
        String hex = String.format("#%06X", currentColor & 0x00FFFFFF);
        drawTextLine(context, swatchX + 16, swatchY, label + ": " + hex, TEXT);
        if (expanded) {
            drawColorPickerFields(context, x + 4, y + CARD_PADDING_TOP + LINE_HEIGHT + 4, currentColor);
        }
        return y + rowHeight;
    }

    private int drawMobColorSwatchRow(GuiGraphicsExtractor context, int x, int y, String label, int currentColor, int mobIndex) {
        boolean expanded = activeMobColorIndex == mobIndex;
        int rowHeight = expanded ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
        drawSettingCard(context, x, y, rowHeight, currentColor | 0xFF000000, false);
        int swatchX = x + 4;
        int swatchY = y + CARD_PADDING_TOP;
        context.fill(swatchX, swatchY, swatchX + 12, swatchY + 12, currentColor | 0xFF000000);
        String hex = String.format("#%06X", currentColor & 0x00FFFFFF);
        drawTextLine(context, swatchX + 16, swatchY, label + ": " + hex, TEXT);
        if (expanded) {
            drawColorPickerFields(context, x + 4, y + CARD_PADDING_TOP + LINE_HEIGHT + 4, currentColor);
        }
        return y + rowHeight;
    }

    private void drawColorPickerFields(GuiGraphicsExtractor context, int pickerX, int hueY, int currentColor) {
        // Hue bar (step size HUE_STEP)
        for (int i = 0; i < COLOR_PICKER_WIDTH; i += HUE_STEP) {
            float hue = (float) i / COLOR_PICKER_WIDTH;
            int hueColor = java.awt.Color.HSBtoRGB(hue, 1f, 1f) | 0xFF000000;
            context.fill(pickerX + i, hueY, pickerX + i + HUE_STEP, hueY + HUE_BAR_HEIGHT, hueColor);
        }
        // SV field (step size SV_STEP)
        float[] hsb = new float[3];
        java.awt.Color.RGBtoHSB((currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF, hsb);
        float selectedHue = hsb[0];
        int svY = hueY + HUE_BAR_HEIGHT + 4;
        for (int sx = 0; sx < COLOR_PICKER_WIDTH; sx += SV_STEP) {
            float sat = (float) sx / COLOR_PICKER_WIDTH;
            for (int sy = 0; sy < SV_FIELD_HEIGHT; sy += SV_STEP) {
                float val = 1f - (float) sy / SV_FIELD_HEIGHT;
                int c = java.awt.Color.HSBtoRGB(selectedHue, sat, val) | 0xFF000000;
                context.fill(pickerX + sx, svY + sy, pickerX + sx + SV_STEP, svY + sy + SV_STEP, c);
            }
        }
        // Crosshair on current position
        int crossX = pickerX + (int)(hsb[1] * COLOR_PICKER_WIDTH);
        int crossY = svY + (int)((1f - hsb[2]) * SV_FIELD_HEIGHT);
        context.fill(crossX - 2, crossY, crossX + 2, crossY + 1, 0xFFFFFFFF);
        context.fill(crossX, crossY - 2, crossX + 1, crossY + 2, 0xFFFFFFFF);
    }

    private boolean isMatchingMapColorRow(String label) {
        String[] labels = { Lang.t("Hintergrund", "Background"), Lang.t("Normal", "Normal"), "Puzzle", "Trap", Lang.t("Eingang", "Entrance"), "Miniboss", "Blood", "Rare" };
        return activeMapColorIndex >= 0 && activeMapColorIndex < labels.length && labels[activeMapColorIndex].equals(label);
    }

    private int getMapColor(int index) {
        return switch (index) {
            case 0 -> config().getMapColorBackground();
            case 1 -> config().getMapColorNormal();
            case 2 -> config().getMapColorPuzzle();
            case 3 -> config().getMapColorTrap();
            case 4 -> config().getMapColorEntrance();
            case 5 -> config().getMapColorMiniboss();
            case 6 -> config().getMapColorBlood();
            case 7 -> config().getMapColorRare();
            default -> 0xFFFFFF;
        };
    }

    private void setMapColor(int index, int color) {
        switch (index) {
            case 0 -> config().setMapColorBackground(color);
            case 1 -> config().setMapColorNormal(color);
            case 2 -> config().setMapColorPuzzle(color);
            case 3 -> config().setMapColorTrap(color);
            case 4 -> config().setMapColorEntrance(color);
            case 5 -> config().setMapColorMiniboss(color);
            case 6 -> config().setMapColorBlood(color);
            case 7 -> config().setMapColorRare(color);
        }
    }

    private boolean handleColorPickerClick(double mouseX, double mouseY, int pickerX, int rowY, int colorIndex) {
        return handleGenericColorPickerClick(mouseX, mouseY, pickerX, rowY, colorIndex, this::getMapColor, this::setMapColor);
    }

    private boolean handleGenericColorPickerClick(double mouseX, double mouseY, int pickerX, int rowY, int colorIndex,
                                                   java.util.function.IntUnaryOperator getter, java.util.function.BiConsumer<Integer, Integer> setter) {
        int hueY = rowY + CARD_PADDING_TOP + LINE_HEIGHT + 4;
        int svY = hueY + HUE_BAR_HEIGHT + 4;
        int currentColor = getter.applyAsInt(colorIndex);
        float[] hsb = new float[3];
        java.awt.Color.RGBtoHSB((currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF, hsb);

        // Check hue bar click
        if (mouseX >= pickerX && mouseX < pickerX + COLOR_PICKER_WIDTH
                && mouseY >= hueY && mouseY < hueY + HUE_BAR_HEIGHT) {
            float hue = (float)(mouseX - pickerX) / COLOR_PICKER_WIDTH;
            int rgb = java.awt.Color.HSBtoRGB(hue, hsb[1], hsb[2]) & 0x00FFFFFF;
            setter.accept(colorIndex, rgb);
            horizonClient.getConfigManager().save();
            colorPickerDragging = true;
            return true;
        }
        // Check SV field click
        if (mouseX >= pickerX && mouseX < pickerX + COLOR_PICKER_WIDTH
                && mouseY >= svY && mouseY < svY + SV_FIELD_HEIGHT) {
            float sat = (float)(mouseX - pickerX) / COLOR_PICKER_WIDTH;
            float val = 1f - (float)(mouseY - svY) / SV_FIELD_HEIGHT;
            int rgb = java.awt.Color.HSBtoRGB(hsb[0], sat, val) & 0x00FFFFFF;
            setter.accept(colorIndex, rgb);
            horizonClient.getConfigManager().save();
            colorPickerDragging = true;
            return true;
        }
        return false;
    }

    private int getMobColor(int index) {
        return switch (index) {
            case 0 -> config().getStarredMobColor();
            case 1 -> config().getBatHighlightColor();
            case 2 -> config().getFelHighlightColor();
            case 3 -> config().getClassColorArcher();
            case 4 -> config().getClassColorBerserk();
            case 5 -> config().getClassColorHealer();
            case 6 -> config().getClassColorMage();
            case 7 -> config().getClassColorTank();
            case 8 -> config().getWitherDoorColor();
            case 9 -> config().getBloodDoorColor();
            case 10 -> config().getSpiritBearHighlightColor();
            default -> 0xFFFFFF;
        };
    }

    private int setMobColor(int index, int color) {
        switch (index) {
            case 0 -> config().setStarredMobColor(color);
            case 1 -> config().setBatHighlightColor(color);
            case 2 -> config().setFelHighlightColor(color);
            case 3 -> config().setClassColorArcher(color);
            case 4 -> config().setClassColorBerserk(color);
            case 5 -> config().setClassColorHealer(color);
            case 6 -> config().setClassColorMage(color);
            case 7 -> config().setClassColorTank(color);
            case 8 -> config().setWitherDoorColor(color);
            case 9 -> config().setBloodDoorColor(color);
            case 10 -> config().setSpiritBearHighlightColor(color);
        }
        return color;
    }

    private int mobsColorSwatchHeight(int index) {
        return activeMobColorIndex == index ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
    }

    private void handleColorPickerDrag(double mouseX, double mouseY) {
        Rect frame = frame();
        Rect viewport = contentViewportRect(frame);

        if (activeMapColorIndex >= 0 && activeMapColorIndex < 8) {
            int y = viewport.y - contentScrollOffset + 24;
            y += toggleRowHeight(Lang.t("Minimap im Dungeon. Groesse per HUD-Layout aenderbar.", "Dungeon minimap. Scale adjustable via HUD layout."));
            y += 24; // "Room Colors" section title
            for (int ci = 0; ci < activeMapColorIndex; ci++) {
                y += COLOR_SWATCH_ROW_HEIGHT;
            }
            handleColorPickerClick(mouseX, mouseY, viewport.x + 4, y, activeMapColorIndex);
        } else if (activeMobColorIndex >= 0 && activeMobColorIndex < 8) {
            int y = viewport.y - contentScrollOffset + 24;
            // Starred Mobs section: 4 rows before first color swatch
            y += toggleRowHeight(Lang.t("Blendet Nametags aller Mobs ohne Stern im Namen aus.", "Hides nametags of all mobs without a star in their name."));
            y += toggleRowHeight(Lang.t("Box/Glow fuer Mobs mit Stern im Namen.", "Box/Glow for mobs with a star in their name."));
            y += toggleRowHeight(Lang.t("Box, Outline, Beides oder Glow.", "Box, Outline, Both or Glow."));
            y += toggleRowHeight(Lang.t("Highlight durch Waende sichtbar.", "Highlight visible through walls."));
            // Color swatch 0 (starred mob)
            if (activeMobColorIndex == 0) {
                handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 0, this::getMobColor, this::setMobColor);
                return;
            }
            y += mobsColorSwatchHeight(0);
            y += 24; // "Other Mobs" section title
            y += toggleRowHeight(Lang.t("Fledermaeuse im Dungeon markieren.", "Highlight bats in dungeons."));
            // Color swatch 1 (bat)
            if (activeMobColorIndex == 1) {
                handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 1, this::getMobColor, this::setMobColor);
                return;
            }
            y += mobsColorSwatchHeight(1);
            y += toggleRowHeight(Lang.t("Unsichtbare Fels (Endermen) im Dungeon markieren.", "Highlight invisible Fels (Endermen) in dungeons."));
            // Color swatch 2 (fel)
            if (activeMobColorIndex == 2) {
                handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 2, this::getMobColor, this::setMobColor);
                return;
            }
            y += mobsColorSwatchHeight(2);
            y += 24; // "Teammates" section title
            y += toggleRowHeight(Lang.t("Dungeon-Teamkameraden per Glow markieren.", "Highlight dungeon teammates with glow."));
            // Class color swatches (indices 3-7)
            for (int ci = 3; ci <= 7; ci++) {
                if (activeMobColorIndex == ci) {
                    handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, ci, this::getMobColor, this::setMobColor);
                    return;
                }
                y += mobsColorSwatchHeight(ci);
            }
        }
    }

    private void drawSettingCard(GuiGraphicsExtractor context, int x, int y, int height, int markerColor, boolean focused) {
        int top = y;
        int bottom = y + height - CARD_GAP + 1;
        int left = x - 12;
        int right = x + CONTENT_ROW_WIDTH + 1;
        context.fill(left, top, right, bottom, focused ? CONFIG_CARD_FOCUSED : CONFIG_CARD);
        context.fill(left, top, left + 3, bottom, markerColor);
    }

    private void drawTextLine(GuiGraphicsExtractor context, int x, int y, String text, int color) {
        context.text(font, Component.literal(text), x, y, color);
    }

    private String fieldValue(String value, boolean focused) {
        String display = value == null || value.isBlank() ? "<leer>" : value;
        return focused ? display + ((System.currentTimeMillis() / 400L) % 2L == 0L ? "_" : "") : display;
    }

    private boolean handleHudClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y - contentScrollOffset + 24;
        if (actionButtonRect(viewport.x, y, true).contains(mouseX, mouseY)) {
            minecraft.setScreen(new HudLayoutScreen(this, horizonClient));
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
                y += toggleRowHeight(Lang.t("Rag!-Titel wenn Necron 'I no longer wish to fight...' sagt (M7).", "Shows Rag! title when Necron says 'I no longer wish to fight...' (M7)."));
                y += 24; // "Tick Timer" section title
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setTickTimerEnabled(!config().isTickTimerEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                yield false;
            }
            case MOBS -> {
                yield handleMobsClick(mouseX, mouseY, viewport, y);
            }
            case DOORS -> {
                yield handleDoorsClick(mouseX, mouseY, viewport, y);
            }
            case REVIVAL -> handleReviveClick(mouseX, mouseY, viewport, y);
            case MAP -> handleMapClick(mouseX, mouseY, viewport, y);
            case PUZZLE_SOLVER -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setPuzzleSolverEnabled(!config().isPuzzleSolverEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Loesungen fuer Blaze, Boulder, Eis, Quiz, Wasser, Creeper Beams, Three Weirdos.", "Solutions for Blaze, Boulder, Ice Fill, Quiz, Water, Creeper Beams, Three Weirdos."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setPuzzleSolverStyle((config().getPuzzleSolverStyle() + 1) % 3);
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                yield false;
            }
            case TERMINAL_SOLVER -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setTerminalSolverEnabled(!config().isTerminalSolverEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Markiert korrekte Slots in F7-Terminals (Panes, Rubix, Order, Starts With, Select All).", "Highlights correct slots in F7 terminals (Panes, Rubix, Order, Starts With, Select All)."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setTerminalSolverBlockWrongClicks(!config().isTerminalSolverBlockWrongClicks());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Falsche Terminal-Klicks unterdrucken.", "Suppress incorrect terminal clicks."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setTerminalSolverCustomMode(!config().isTerminalSolverCustomMode());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Nicht-relevante Slots vollstaendig ausblenden statt nur abzudunkeln.", "Fully hide non-relevant slots instead of just dimming them."));
                if (sliderRect(viewport.x, y).contains(mouseX, mouseY)) {
                    activeSliderIndex = 20;
                    applySliderValue(20, mouseX, viewport.x);
                    yield true;
                }
                yield false;
            }
            case BOSS -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setSimonSaysEnabled(!config().isSimonSaysEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Hebt die korrekte Schaltflaechen-Reihenfolge beim Goldor-Device hervor.", "Highlights the correct button sequence for the Goldor device."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setSimonSaysBlockWrongClicks(!config().isSimonSaysBlockWrongClicks());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Blockiert Klicks auf falsche Simon-Says-Knoepfe.", "Blocks clicks on incorrect Simon Says buttons."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setArrowAlignEnabled(!config().isArrowAlignEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt Klickanzahl fuer jede Pfeil-Bilderrahmen.", "Shows click count for each arrow item frame."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setSharpShooterEnabled(!config().isSharpShooterEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Hebt getroffene Smaragdbloecke beim Arrow-Device hervor.", "Highlights hit emerald blocks at the arrow device."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setPurplePadTimerEnabled(!config().isPurplePadTimerEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Countdown bis zum Purple-Pad-Zeitpunkt (F7 P2).", "Countdown until purple pad timing (F7 P2)."));
                y += 24; // "General" section title
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setBloodCamperEnabled(!config().isBloodCamperEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt Blood-Room-Fortschritt und Timer an.", "Shows blood room wave progress and timer."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDungeonScoreEnabled(!config().isDungeonScoreEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt geschaetzte Dungeon-Punktzahl als HUD an.", "Shows estimated dungeon score as HUD overlay."));
                y += 24; // "M7 Dragons (P5)" section title
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDragonEnabled(!config().isDragonEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt Dragon-Spawn-Prioritaet, Boxen und Timer in M7 P5.", "Shows dragon spawn priority, boxes and timer in M7 P5."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDragonBoxes(!config().isDragonBoxes());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt farbige Boxen an den Spawn-Positionen.", "Shows colored boxes at spawn positions."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDragonTimer(!config().isDragonTimer());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt Countdown bis zum Spawn.", "Shows countdown until spawn."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDragonSpawnAlert(!config().isDragonSpawnAlert());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt Spawn-Warnung im Chat.", "Shows spawn alert in chat."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setDragonPriority(!config().isDragonPriority());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Zeigt empfohlene Kill-Reihenfolge.", "Shows recommended kill order."));
                y += 24; // "M7 Relic Timer" section title
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setRelicTimerEnabled(!config().isRelicTimerEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                yield false;
            }
            case FLOOR_SPECIALS -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setSpiritBearTimerEnabled(!config().isSpiritBearTimerEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Fortschritt und Countdown bis zum Spirit Bear Spawn.", "Progress and countdown until Spirit Bear spawn."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setSpiritBearHighlightEnabled(!config().isSpiritBearHighlightEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Spirit Bear per Glow hervorheben.", "Highlight Spirit Bear with glow."));
                int sbRowH = activeMobColorIndex == 10 ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
                if (rowRect(viewport.x, y, sbRowH).contains(mouseX, mouseY)) {
                    if (activeMobColorIndex == 10) {
                        if (handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 10, this::getMobColor, this::setMobColor)) yield true;
                    }
                    activeMobColorIndex = activeMobColorIndex == 10 ? -1 : 10;
                    yield true;
                }
                yield false;
            }
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

    private boolean handleMobsClick(double mouseX, double mouseY, Rect viewport, int y) {
        // "Starred Mobs" section title
        // Hide Non-Starred Mobs
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setHideNonStarredMobsEnabled(!config().isHideNonStarredMobsEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Blendet Nametags aller Mobs ohne Stern im Namen aus.", "Hides nametags of all mobs without a star in their name."));
        // Highlight Starred Mobs
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setHighlightStarredMobsEnabled(!config().isHighlightStarredMobsEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Glow fuer Mobs mit Stern im Namen.", "Glow for mobs with a star in their name."));
        // Starred Mob Color (index 0)
        int smRowH = activeMobColorIndex == 0 ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
        if (rowRect(viewport.x, y, smRowH).contains(mouseX, mouseY)) {
            if (activeMobColorIndex == 0) {
                if (handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 0, this::getMobColor, this::setMobColor)) return true;
            }
            activeMobColorIndex = activeMobColorIndex == 0 ? -1 : 0;
            return true;
        }
        y += smRowH;
        y += 24; // "Other Mobs" section title
        // Highlight Bats
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setHighlightBatsEnabled(!config().isHighlightBatsEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Fledermaeuse im Dungeon markieren.", "Highlight bats in dungeons."));
        // Bat Color (index 1)
        int batRowH = activeMobColorIndex == 1 ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
        if (rowRect(viewport.x, y, batRowH).contains(mouseX, mouseY)) {
            if (activeMobColorIndex == 1) {
                if (handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 1, this::getMobColor, this::setMobColor)) return true;
            }
            activeMobColorIndex = activeMobColorIndex == 1 ? -1 : 1;
            return true;
        }
        y += batRowH;
        // Highlight Fels
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setHighlightFelsEnabled(!config().isHighlightFelsEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Unsichtbare Fels (Endermen) im Dungeon markieren.", "Highlight invisible Fels (Endermen) in dungeons."));
        // Fel Color (index 2)
        int felRowH = activeMobColorIndex == 2 ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
        if (rowRect(viewport.x, y, felRowH).contains(mouseX, mouseY)) {
            if (activeMobColorIndex == 2) {
                if (handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 2, this::getMobColor, this::setMobColor)) return true;
            }
            activeMobColorIndex = activeMobColorIndex == 2 ? -1 : 2;
            return true;
        }
        y += felRowH;
        y += 24; // "Teammates" section title
        // Teammate Glow
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setTeammateGlowEnabled(!config().isTeammateGlowEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Dungeon-Teamkameraden per Glow markieren.", "Highlight dungeon teammates with glow."));
        // Class color rows (indices 3-7: Archer, Berserk, Healer, Mage, Tank)
        for (int ci = 3; ci <= 7; ci++) {
            int rowH = activeMobColorIndex == ci ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
            if (rowRect(viewport.x, y, rowH).contains(mouseX, mouseY)) {
                if (activeMobColorIndex == ci) {
                    if (handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, ci, this::getMobColor, this::setMobColor)) return true;
                }
                activeMobColorIndex = activeMobColorIndex == ci ? -1 : ci;
                return true;
            }
            y += rowH;
        }
        // "Mimic & Prince" section title
        y += 24;
        // Mimic Detection toggle
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setMimicDetectionEnabled(!config().isMimicDetectionEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Erkennt Mimic-Kill per Tod-Event (F6+).", "Detects mimic kill via death event (F6+)."));
        // Mimic Message toggle
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setMimicMessageEnabled(!config().isMimicMessageEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Sendet 'Mimic killed!' in den Party-Chat.", "Sends 'Mimic killed!' to party chat."));
        // Prince Message toggle
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setPrinceMessageEnabled(!config().isPrinceMessageEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        return false;
    }

    private boolean handleDoorsClick(double mouseX, double mouseY, Rect viewport, int y) {
        // "Wither Doors" section title
        // Wither Door ESP toggle
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setWitherDoorEspEnabled(!config().isWitherDoorEspEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Wither-Tueren im Dungeon hervorheben.", "Highlight wither doors in dungeons."));
        // Wither Door Color (index 8)
        int wdRowH = activeMobColorIndex == 8 ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
        if (rowRect(viewport.x, y, wdRowH).contains(mouseX, mouseY)) {
            if (activeMobColorIndex == 8) {
                if (handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 8, this::getMobColor, this::setMobColor)) return true;
            }
            activeMobColorIndex = activeMobColorIndex == 8 ? -1 : 8;
            return true;
        }
        y += wdRowH;
        y += 24; // "Blood Doors" section title
        // Blood Door ESP toggle
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setBloodDoorEspEnabled(!config().isBloodDoorEspEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Blood-Tueren im Dungeon hervorheben.", "Highlight blood doors in dungeons."));
        // Blood Door Color (index 9)
        int bdRowH = activeMobColorIndex == 9 ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
        if (rowRect(viewport.x, y, bdRowH).contains(mouseX, mouseY)) {
            if (activeMobColorIndex == 9) {
                if (handleGenericColorPickerClick(mouseX, mouseY, viewport.x + 4, y, 9, this::getMobColor, this::setMobColor)) return true;
            }
            activeMobColorIndex = activeMobColorIndex == 9 ? -1 : 9;
            return true;
        }
        y += bdRowH;
        y += 24; // "Keys" section title
        // Key Highlight toggle
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setDoorKeyHighlightEnabled(!config().isDoorKeyHighlightEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        return false;
    }

    private boolean handleMapClick(double mouseX, double mouseY, Rect viewport, int y) {
        // y is already past the first section title ("Dungeon Map")
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setDungeonMapEnabled(!config().isDungeonMapEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Minimap im Dungeon. Groesse per HUD-Layout aenderbar.", "Dungeon minimap. Scale adjustable via HUD layout."));
        y += 24; // "Map Colors" section title
        // 8 color rows (background + 7 room types)
        for (int ci = 0; ci < 8; ci++) {
            int rowH = activeMapColorIndex == ci ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
            if (rowRect(viewport.x, y, rowH).contains(mouseX, mouseY)) {
                if (activeMapColorIndex == ci) {
                    // Check HSV picker click
                    if (handleColorPickerClick(mouseX, mouseY, viewport.x + 4, y, ci)) {
                        return true;
                    }
                }
                activeMapColorIndex = activeMapColorIndex == ci ? -1 : ci;
                return true;
            }
            y += rowH;
        }
        y += 24; // "Leap Menu" section title
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setLeapMenuEnabled(!config().isLeapMenuEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Eigenes Quadranten-GUI fuer Spirit Leap.", "Custom quadrant GUI for Spirit Leap."));
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setLeapMenuAnnounce(!config().isLeapMenuAnnounce());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(Lang.t("Leap-Ziel im Party-Chat ankuendigen.", "Announce leap destination in party chat."));
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setLeapMenuSortMode((config().getLeapMenuSortMode() + 1) % 3);
            horizonClient.getConfigManager().save();
            return true;
        }
        return false;
    }

    private boolean handleParticleClick(double mouseX, double mouseY, Rect frame) {
        Rect viewport = contentViewportRect(frame);
        int y = viewport.y;
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            config().setBreakParticlesEnabled(!config().isBreakParticlesEnabled());
            horizonClient.getConfigManager().save();
            return true;
        }
        y += toggleRowHeight(BREAK_PARTICLES_DESC);
        if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
            inputFocus = InputFocus.PARTICLE_SEARCH;
            return true;
        }
        y += fieldRowHeight(Lang.t("Liste filtern.", "Filter list."));
        y -= particleScrollOffset;
        for (String particleId : filteredParticleIds()) {
            if (rowRect(viewport.x, y, 14).contains(mouseX, mouseY)) {
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
                y += toggleRowHeight(Lang.t("Modus: Aus, Strg+LK, Rechtsklick oder Beides.", "Mode: Off, Ctrl+LClick, Right MouseButtonEvent or Both."));
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
            case CHAT_COMMANDS -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setChatCommandsEnabled(!config().isChatCommandsEnabled());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("!-Befehle im Party/Gilde/Privat-Chat ausfuehren.", "Execute !-commands in party/guild/private chat."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setChatCommandsParty(!config().isChatCommandsParty());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("!-Befehle im Party-Chat erlauben.", "Allow !-commands in party chat."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setChatCommandsGuild(!config().isChatCommandsGuild());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("!-Befehle im Gilde-Chat erlauben.", "Allow !-commands in guild chat."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setChatCommandsPrivate(!config().isChatCommandsPrivate());
                    horizonClient.getConfigManager().save();
                    yield true;
                }
                y += toggleRowHeight(Lang.t("!-Befehle in Privatnachrichten erlauben.", "Allow !-commands in private messages."));
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    chatCommandListExpanded = !chatCommandListExpanded;
                    yield true;
                }
                y += toggleRowHeight(Lang.t("Einzelne !-Befehle an-/ausschalten.", "Enable/disable individual !-commands."));
                if (chatCommandListExpanded) {
                    for (String[] entry : CHAT_COMMANDS_LIST) {
                        if (rowRect(viewport.x + 16, y).contains(mouseX, mouseY)) {
                            config().setChatCommandEnabled(entry[0], !config().isChatCommandEnabled(entry[0]));
                            horizonClient.getConfigManager().save();
                            yield true;
                        }
                        y += toggleRowHeight(entry[1]);
                    }
                }
                yield false;
            }
            case SHORTCUTS -> {
                if (rowRect(viewport.x, y).contains(mouseX, mouseY)) {
                    config().setCommandShortcutsEnabled(!config().isCommandShortcutsEnabled());
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
        if (minecraft == null) {
            return;
        }
        String clipboard = minecraft.keyboardHandler.getClipboard();
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
        if (minecraft == null) {
            return;
        }
        minecraft.keyboardHandler.setClipboard(switch (inputFocus) {
            case CATACOMBS_LEVEL -> catacombsInput;
            case HUD_ACCENT_COLOR -> hudAccentColorInput;
            case CHAT_BRIDGE_BOT_NAME -> chatBridgeBotNameInput;
            case GLOBAL_SEARCH -> globalSearchInput;
            case PARTICLE_SEARCH -> particleSearchInput;
            default -> "";
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
            case DISPLAY -> displayContentHeight();
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
        addSearchResult(results, query, "Break Particles", "Particle", Tab.PARTICLE, null, "break particles block abbauen partikel");
        addSearchResult(results, query, "Particle Suche", "Particle", Tab.PARTICLE, null, "particle suche filter");
        addSearchResult(results, query, "Zeit HUD", "Misc", Tab.MISC, null, "zeit hud clock");
        addSearchResult(results, query, "FPS / TPS / Ping", "Misc", Tab.MISC, null, "fps tps ping performance");
        addSearchResult(results, query, "System HUD", "Misc", Tab.MISC, null, "system hud cpu gpu temperatur");
        addSearchResult(results, query, "Defense Bar", "Misc", Tab.MISC, null, "defense bar ruestung armor");
        addSearchResult(results, query, "Kompakte Herzen", "Misc", Tab.MISC, null, "kompakte herzen hypixel health herz absorption");
        addSearchResult(results, query, "Rag Axe Notification", "Dungeons / General", Tab.DUNGEON, DungeonSection.GENERAL, "rag axe notification necron m7 phase dungeon");
        addSearchResult(results, query, "Starred Mobs", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS, "starred mobs highlight glow stern dungeon");
        addSearchResult(results, query, "Highlight Bats", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS, "bats fledermaeuse highlight dungeon");
        addSearchResult(results, query, "Highlight Fels", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS, "fels enderman invisible highlight dungeon");
        addSearchResult(results, query, "Teammate Glow", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS, "teammate glow dungeon party class archer berserk healer mage tank");
        addSearchResult(results, query, "Mimic Detection", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS, "mimic detection kill zombie baby dungeon f6 f7");
        addSearchResult(results, query, "Mimic Message", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS, "mimic party chat message killed dungeon");
        addSearchResult(results, query, "Prince Message", "Dungeons / Mobs", Tab.DUNGEON, DungeonSection.MOBS, "prince party chat message killed dungeon bonus score");
        addSearchResult(results, query, "Wither Door ESP", "Dungeons / Doors", Tab.DUNGEON, DungeonSection.DOORS, "wither door esp highlight dungeon");
        addSearchResult(results, query, "Blood Door ESP", "Dungeons / Doors", Tab.DUNGEON, DungeonSection.DOORS, "blood door esp highlight dungeon");
        addSearchResult(results, query, "Door Key Highlight", "Dungeons / Doors", Tab.DUNGEON, DungeonSection.DOORS, "door key highlight wither blood tracer dungeon");
        addSearchResult(results, query, "Spirit Bear Timer", "Dungeons / Specials", Tab.DUNGEON, DungeonSection.FLOOR_SPECIALS, "spirit bear timer f4 m4 boss spawn dungeon");
        addSearchResult(results, query, "Spirit Bear Highlight", "Dungeons / Specials", Tab.DUNGEON, DungeonSection.FLOOR_SPECIALS, "spirit bear highlight glow f4 m4 dungeon");
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
        addSearchResult(results, query, "Command Shortcuts", "Chat / Shortcuts", Tab.CHAT, null, "command shortcuts f1 f7 m1 m7 joininstance catacombs master dungeon hub warp");
        addSearchResult(results, query, "Custom Scoreboard", "Scoreboard", Tab.SCOREBOARD, null, "custom scoreboard sidebar hypixel leiste");
        for (SkyBlockIsland island : SkyBlockIsland.knownIslands()) {
            addSearchResult(results, query, island.label(), "Scoreboard", Tab.SCOREBOARD, null, "scoreboard " + island.label().toLowerCase(Locale.ROOT) + " island zeilen filter");
        }
        addSearchResult(results, query, "Inventory Buttons", "Inventory / General", Tab.INVENTORY, null, "inventory buttons inventar");
        addSearchResult(results, query, "Announce Rare Sea Creatures", "Fishing", Tab.FISHING, null, "fishing rare sea creatures elusive announce title sound alert");
        addSearchResult(results, query, Lang.t("Alert Sound", "Alert Sound"), "Fishing", Tab.FISHING, null, "fishing alert sound rare meow katze custom boo womp");
        addSearchResult(results, query, "Creature Filter", "Fishing", Tab.FISHING, null, "fishing creature filter sea creatures toggle enable disable");
        for (ElusiveSeaCreature creature : ElusiveSeaCreature.values()) {
            addSearchResult(results, query, creature.displayName(), "Fishing", Tab.FISHING, null, "fishing " + creature.displayName().toLowerCase(Locale.ROOT) + " elusive sea creature");
        }
        addSearchResult(results, query, "16:9 Pillarbox", "Anzeige", Tab.DISPLAY, null, "pillarbox 16:9 anzeige display monitor ultrawide 32:9 odyssey g9 schwarze balken letterbox");
        addSearchResult(results, query, "Position X", "Anzeige / Animationen", Tab.DISPLAY, null, "animation hand item position x horizontal");
        addSearchResult(results, query, "Position Y", "Anzeige / Animationen", Tab.DISPLAY, null, "animation hand item position y vertikal vertical");
        addSearchResult(results, query, "Position Z", "Anzeige / Animationen", Tab.DISPLAY, null, "animation hand item position z tiefe depth");
        addSearchResult(results, query, Lang.t("Groesse", "Scale"), "Anzeige / Animationen", Tab.DISPLAY, null, "animation hand item groesse scale size skalierung");
        addSearchResult(results, query, Lang.t("Schlaggeschwindigkeit", "Swing Speed"), "Anzeige / Animationen", Tab.DISPLAY, null, "animation hand swing speed schlaggeschwindigkeit geschwindigkeit");
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

    private void drawWrappedText(GuiGraphicsExtractor context, int x, int y, String text, int maxWidth, int color) {
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
            if (font.width(candidate) > maxWidth && !current.isEmpty()) {
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

    private void drawInlineAction(GuiGraphicsExtractor context, Rect rect, String label) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), CONFIG_BUTTON);
        context.centeredText(font, Component.literal(label), rect.centerX(), rect.y + 5, CONFIG_BUTTON_TEXT);
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
                + toggleRowHeight(Lang.t("Rag!-Titel wenn Necron 'I no longer wish to fight...' sagt (M7).", "Shows Rag! title when Necron says 'I no longer wish to fight...' (M7)."))
                + 24 // "Tick Timer" section title
                + toggleRowHeight(Lang.t("Countdown bis zum naechsten Goldor-Damage-Tick (F7 P3).", "Countdown to next Goldor damage tick (F7 P3)."));
            case MOBS -> {
                int mobsH = toggleRowHeight(Lang.t("Blendet Nametags aller Mobs ohne Stern im Namen aus.", "Hides nametags of all mobs without a star in their name."))
                    + toggleRowHeight(Lang.t("Box/Glow fuer Mobs mit Stern im Namen.", "Box/Glow for mobs with a star in their name."))
                    + toggleRowHeight(Lang.t("Box, Outline, Beides oder Glow.", "Box, Outline, Both or Glow."))
                    + toggleRowHeight(Lang.t("Highlight durch Waende sichtbar.", "Highlight visible through walls."))
                    + mobsColorSwatchHeight(0)
                    + 24 // "Other Mobs" section title
                    + toggleRowHeight(Lang.t("Fledermaeuse im Dungeon markieren.", "Highlight bats in dungeons."))
                    + mobsColorSwatchHeight(1)
                    + toggleRowHeight(Lang.t("Unsichtbare Fels (Endermen) im Dungeon markieren.", "Highlight invisible Fels (Endermen) in dungeons."))
                    + mobsColorSwatchHeight(2)
                    + 24 // "Teammates" section title
                    + toggleRowHeight(Lang.t("Dungeon-Teamkameraden per Glow markieren.", "Highlight dungeon teammates with glow."))
                    + mobsColorSwatchHeight(3)
                    + mobsColorSwatchHeight(4)
                    + mobsColorSwatchHeight(5)
                    + mobsColorSwatchHeight(6)
                    + mobsColorSwatchHeight(7)
                    + 24 // "Mimic & Prince" section title
                    + toggleRowHeight(Lang.t("Erkennt Mimic-Kill per Tod-Event (F6+).", "Detects mimic kill via death event (F6+)."))
                    + toggleRowHeight(Lang.t("Sendet 'Mimic killed!' in den Party-Chat.", "Sends 'Mimic killed!' to party chat."))
                    + toggleRowHeight(Lang.t("Sendet 'Prince killed!' in den Party-Chat.", "Sends 'Prince killed!' to party chat."));
                yield mobsH;
            }
            case DOORS -> toggleRowHeight(Lang.t("Wither-Tueren im Dungeon hervorheben.", "Highlight wither doors in dungeons."))
                + mobsColorSwatchHeight(8)
                + 24 // "Blood Doors" section title
                + toggleRowHeight(Lang.t("Blood-Tueren im Dungeon hervorheben.", "Highlight blood doors in dungeons."))
                + mobsColorSwatchHeight(9)
                + 24 // "Keys" section title
                + toggleRowHeight(Lang.t("Zeigt Box und Tracer zu Wither/Blood Keys.", "Shows box and tracer to Wither/Blood keys."));
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
            case MAP -> {
                // "Dungeon Map" section title already counted in base 24
                int mapH = toggleRowHeight(Lang.t("Minimap im Dungeon. Groesse per HUD-Layout aenderbar.", "Dungeon minimap. Scale adjustable via HUD layout."))
                    + 24; // "Map Colors" section title
                for (int ci = 0; ci < 8; ci++) {
                    mapH += activeMapColorIndex == ci ? COLOR_PICKER_EXPANDED_HEIGHT : COLOR_SWATCH_ROW_HEIGHT;
                }
                mapH += 24 // "Leap Menu" section title
                    + toggleRowHeight(Lang.t("Eigenes Quadranten-GUI fuer Spirit Leap.", "Custom quadrant GUI for Spirit Leap."))
                    + toggleRowHeight(Lang.t("Leap-Ziel im Party-Chat ankuendigen.", "Announce leap destination in party chat."))
                    + toggleRowHeight(Lang.t("Klasse-Quadrant, Klasse A-Z oder Name A-Z.", "Class quadrant, class A-Z or name A-Z."));
                yield mapH;
            }
            case PUZZLE_SOLVER -> 24
                + toggleRowHeight(Lang.t("Loesungen fuer Blaze, Boulder, Eis, Quiz, Wasser, Creeper Beams, Three Weirdos.", "Solutions for Blaze, Boulder, Ice Fill, Quiz, Water, Creeper Beams, Three Weirdos."))
                + toggleRowHeight(Lang.t("Render-Stil der Loesung.", "Render style of the solution."));
            case TERMINAL_SOLVER -> 24
                + toggleRowHeight(Lang.t("Markiert korrekte Slots in F7-Terminals (Panes, Rubix, Order, Starts With, Select All).", "Highlights correct slots in F7 terminals (Panes, Rubix, Order, Starts With, Select All)."))
                + toggleRowHeight(Lang.t("Falsche Terminal-Klicks unterdrucken.", "Suppress incorrect terminal clicks."))
                + toggleRowHeight(Lang.t("Nicht-relevante Slots vollstaendig ausblenden statt nur abzudunkeln.", "Fully hide non-relevant slots instead of just dimming them."))
                + sliderRowHeight();
            case BOSS -> 24
                + toggleRowHeight(Lang.t("Hebt die korrekte Schaltflaechen-Reihenfolge beim Goldor-Device hervor.", "Highlights the correct button sequence for the Goldor device."))
                + toggleRowHeight(Lang.t("Blockiert Klicks auf falsche Simon-Says-Knoepfe.", "Blocks clicks on incorrect Simon Says buttons."))
                + toggleRowHeight(Lang.t("Zeigt Klickanzahl fuer jede Pfeil-Bilderrahmen.", "Shows click count for each arrow item frame."))
                + toggleRowHeight(Lang.t("Hebt getroffene Smaragdbloecke beim Arrow-Device hervor.", "Highlights hit emerald blocks at the arrow device."))
                + toggleRowHeight(Lang.t("Countdown bis zum Purple-Pad-Zeitpunkt (F7 P2).", "Countdown until purple pad timing (F7 P2)."))
                + 24 // "General" section title
                + toggleRowHeight(Lang.t("Zeigt Blood-Room-Fortschritt und Timer an.", "Shows blood room wave progress and timer."))
                + toggleRowHeight(Lang.t("Zeigt geschaetzte Dungeon-Punktzahl als HUD an.", "Shows estimated dungeon score as HUD overlay."))
                + 24 // "M7 Dragons (P5)" section title
                + toggleRowHeight(Lang.t("Zeigt Dragon-Spawn-Prioritaet, Boxen und Timer in M7 P5.", "Shows dragon spawn priority, boxes and timer in M7 P5."))
                + toggleRowHeight(Lang.t("Zeigt farbige Boxen an den Spawn-Positionen.", "Shows colored boxes at spawn positions."))
                + toggleRowHeight(Lang.t("Zeigt Countdown bis zum Spawn.", "Shows countdown until spawn."))
                + toggleRowHeight(Lang.t("Zeigt Spawn-Warnung im Chat.", "Shows spawn alert in chat."))
                + toggleRowHeight(Lang.t("Zeigt empfohlene Kill-Reihenfolge.", "Shows recommended kill order."))
                + 24 // "M7 Relic Timer" section title
                + toggleRowHeight(Lang.t("Countdown bis zum Relic-Spawn nach Necron.", "Countdown until relic spawn after Necron."));
            case FLOOR_SPECIALS -> toggleRowHeight(Lang.t("Fortschritt und Countdown bis zum Spirit Bear Spawn.", "Progress and countdown until Spirit Bear spawn."))
                + toggleRowHeight(Lang.t("Spirit Bear per Glow hervorheben.", "Highlight Spirit Bear with glow."))
                + mobsColorSwatchHeight(10);
        };
    }

    private int particleContentHeight() {
        return toggleRowHeight(BREAK_PARTICLES_DESC) + fieldRowHeight(Lang.t("Liste filtern.", "Filter list.")) + Math.max(0, filteredParticleIds().size() * 14);
    }

    private int miscContentHeight() {
        return 24
            + toggleRowHeight(Lang.t("Lokale Uhrzeit als Overlay.", "Local time as overlay."))
            + toggleRowHeight(Lang.t("Performance-Overlay.", "Performance overlay."))
            + toggleRowHeight(Lang.t("CPU / GPU / Temperaturen.", "CPU / GPU / Temperatures."))
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

    private void renderFishingText(GuiGraphicsExtractor context, Rect viewport) {
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
                + toggleRowHeight(Lang.t("Modus: Aus, Strg+LK, Rechtsklick oder Beides.", "Mode: Off, Ctrl+LClick, Right MouseButtonEvent or Both."))
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
            case CHAT_COMMANDS -> {
                int height = toggleRowHeight(Lang.t("!-Befehle im Party/Gilde/Privat-Chat ausfuehren.", "Execute !-commands in party/guild/private chat."))
                    + toggleRowHeight(Lang.t("!-Befehle im Party-Chat erlauben.", "Allow !-commands in party chat."))
                    + toggleRowHeight(Lang.t("!-Befehle im Gilde-Chat erlauben.", "Allow !-commands in guild chat."))
                    + toggleRowHeight(Lang.t("!-Befehle in Privatnachrichten erlauben.", "Allow !-commands in private messages."))
                    + toggleRowHeight(Lang.t("Einzelne !-Befehle an-/ausschalten.", "Enable/disable individual !-commands."));
                if (chatCommandListExpanded) {
                    for (String[] entry : CHAT_COMMANDS_LIST) {
                        height += toggleRowHeight(entry[1]);
                    }
                }
                yield height;
            }
            case SHORTCUTS -> toggleRowHeight(Lang.t("/f1-/f7, /m1-/m7, /d, /dh als Kurzbefehle verwenden.", "Use /f1-/f7, /m1-/m7, /d, /dh as command shortcuts."))
                + 3 * LINE_HEIGHT;
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

    private int drawScoreboardLineRow(GuiGraphicsExtractor context, int x, int y, String lineText, boolean visible, int textColor) {
        int rowHeight = scoreboardLineRowHeight();
        drawSettingCard(context, x, y, rowHeight, visible ? 0xFF2DBA68 : 0xFF8A97A8, false);
        Rect badge = toggleBadgeRect(x, y);
        context.fill(badge.x, badge.y, badge.right(), badge.bottom(), visible ? 0xFF2DBA68 : 0xFF667487);
        context.centeredText(font, Component.literal(visible ? Lang.t("AN", "ON") : Lang.t("AUS", "OFF")), badge.centerX(), badge.y + 4, 0xFFF7FBFF);
        int contentX = badge.right() + 10;
        if (visible) {
            drawTextLine(context, contentX, y + CARD_PADDING_TOP, lineText, textColor);
        } else {
            context.text(font, Component.literal(lineText).withStyle(ChatFormatting.STRIKETHROUGH), contentX, y + CARD_PADDING_TOP, MUTED);
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

    private void drawConfirmationOverlay(GuiGraphicsExtractor context, Rect frame, int accent) {
        int w = 320, h = 94;
        int ox = frame.x + (frame.width - w) / 2;
        int oy = frame.y + (frame.height - h) / 2;
        context.fill(ox, oy, ox + w, oy + h, 0xE8151C25);
        context.outline(ox, oy, w, h, HudStyle.border());
        drawTextLine(context, ox + 12, oy + 12, Lang.t("Globale Aenderung", "Global Change"), accent);
        drawTextLine(context, ox + 12, oy + 28, "\"" + pendingGlobalToggleLabel + "\"" + Lang.t(" fuer alle Islands toggeln?", " toggle for all islands?"), MUTED);
        Rect yes = confirmYesRect(frame);
        Rect no = confirmNoRect(frame);
        context.fill(yes.x, yes.y, yes.right(), yes.bottom(), 0xFF2DBA68);
        context.centeredText(font, Component.literal(Lang.t("JA", "YES")), yes.centerX(), yes.y + 5, 0xFFF7FBFF);
        context.fill(no.x, no.y, no.right(), no.bottom(), 0xFF8A3A3A);
        context.centeredText(font, Component.literal(Lang.t("NEIN", "NO")), no.centerX(), no.y + 5, 0xFFF7FBFF);
    }

    private void drawReloadPopup(GuiGraphicsExtractor context, Rect frame, int accent) {
        int w = 280, h = 82;
        int ox = frame.x + (frame.width - w) / 2;
        int oy = frame.y + (frame.height - h) / 2;
        context.fill(ox, oy, ox + w, oy + h, 0xE8151C25);
        context.outline(ox, oy, w, h, HudStyle.border());
        drawTextLine(context, ox + 12, oy + 12, Lang.t("Config Reload", "Config Reload"), accent);
        drawTextLine(context, ox + 12, oy + 28, Lang.t("Konfiguration wurde neu geladen.", "Configuration reloaded successfully."), MUTED);
        int bw = 80, bx = ox + (w - bw) / 2, by = oy + h - 28;
        context.fill(bx, by, bx + bw, by + 18, 0xFF2DBA68);
        context.centeredText(font, Component.literal("OK"), bx + bw / 2, by + 5, 0xFFF7FBFF);
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

    private void drawWindowChrome(GuiGraphicsExtractor context, Rect frame, Rect viewport, int accent) {
        context.fill(frame.x, frame.y, frame.right(), frame.bottom(), CONFIG_WINDOW);
        context.fill(viewport.x - 12, frame.y + 35, frame.right() - 1, frame.bottom() - 1, CONFIG_WINDOW);
        context.outline(frame.x, frame.y, frame.width, frame.height, HudStyle.border());
        context.fill(frame.x, frame.y, frame.right(), frame.y + 34, CONFIG_WINDOW_HEADER);
        drawTextLine(context, frame.x + 12, frame.y + 12, "HORIZON", accent);
        drawTextLine(context, searchRect(frame).x, searchRect(frame).y + 2, Lang.t("Suche: ", "Search: ") + fieldValue(globalSearchInput, inputFocus == InputFocus.GLOBAL_SEARCH), inputFocus == InputFocus.GLOBAL_SEARCH ? accent : TEXT);
        drawTextLine(context, closeRect(frame).x, closeRect(frame).y + 2, "[X]", WARNING);
    }

    private void drawHeaderMask(GuiGraphicsExtractor context, Rect frame, int accent) {
        context.fill(frame.x + 1, frame.y + 1, frame.right() - 1, frame.y + 34, CONFIG_WINDOW_HEADER);
        context.outline(frame.x, frame.y, frame.width, frame.height, HudStyle.border());
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
        DISPLAY("Anzeige"),
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
        SPAM_FILTERS("Spam Filters"),
        CHAT_COMMANDS("Chat Commands"),
        SHORTCUTS("Shortcuts");

        private final String label;

        ChatSection(String label) {
            this.label = label;
        }
    }

    private enum DungeonSection {
        GENERAL("General"),
        MOBS("Mobs"),
        DOORS("Doors"),
        REVIVAL("Revive"),
        MAP("Map"),
        PUZZLE_SOLVER("Puzzles"),
        TERMINAL_SOLVER("Terminal"),
        BOSS("Boss"),
        FLOOR_SPECIALS("Specials");

        private final String label;

        DungeonSection(String label) {
            this.label = label;
        }
    }

    private enum DisplaySection {
        GENERAL("General"),
        ANIMATIONS("Animationen"),
        NO_RENDER("NoRender"),
        HELPERS("Helpers");

        private final String label;

        DisplaySection(String label) {
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
        PARTICLE_SEARCH,
        SLOT_BIND_KEY,
        CMD_KEY_PETS,
        CMD_KEY_EQUIPMENT,
        CMD_KEY_WARDROBE
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
