package de.horizon;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.horizon.api.HorizonApiClient;
import de.horizon.api.auth.HorizonApiAuthService;
import de.horizon.api.profile.HorizonProfileGateway;
import de.horizon.config.ConfigManager;
import de.horizon.render.PillarboxState;
import de.horizon.feature.chat.ChatCommandService;
import de.horizon.feature.chat.ChatTabManager;
import de.horizon.feature.chat.SpamHider;
import de.horizon.feature.dungeon.BloodCamperService;
import de.horizon.feature.dungeon.DungeonAlertService;
import de.horizon.feature.dungeon.DungeonScoreService;
import de.horizon.feature.dungeon.MimicService;
import de.horizon.feature.dungeon.DoorEspService;
import de.horizon.feature.dungeon.StarredMobService;
import de.horizon.feature.dungeon.TeammateGlowService;
import de.horizon.feature.dungeon.LeapMenuOverlay;
import de.horizon.feature.dungeon.TickTimerService;
import de.horizon.feature.fishing.FishingAlertService;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import de.horizon.feature.dungeon.puzzle.PuzzleSolverService;
import de.horizon.feature.dungeon.terminal.TerminalSolverService;
import de.horizon.feature.dungeon.boss.SimonSaysService;
import de.horizon.feature.dungeon.boss.ArrowAlignService;
import de.horizon.feature.dungeon.boss.PurplePadTimerService;
import de.horizon.feature.dungeon.boss.DragonService;
import de.horizon.feature.dungeon.boss.RelicTimerService;
import de.horizon.feature.dungeon.boss.SharpShooterService;
import de.horizon.feature.dungeon.boss.SpiritBearService;
import de.horizon.feature.dungeon.map.DungeonMapService;
import de.horizon.hud.DungeonMapHudElement;
import de.horizon.hud.DungeonScoreHudElement;
import de.horizon.hud.SpiritBearTimerHudElement;
import de.horizon.feature.inventory.SlotBindService;
import de.horizon.feature.misc.EtherwarpHelperService;
import de.horizon.feature.misc.PingService;
import de.horizon.feature.misc.SystemStatsService;
import de.horizon.feature.misc.TpsTracker;
import de.horizon.feature.misc.WardrobeKeybindService;
import de.horizon.feature.particle.ParticleFilterService;
import de.horizon.feature.revive.ReviveTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import de.horizon.hypixel.HypixelProfileService;
import de.horizon.hypixel.PartyFinderOverlay;
import de.horizon.hypixel.HypixelSidebarOverlay;
import de.horizon.hud.HudElement;
import de.horizon.hud.HudRegistry;
import de.horizon.hud.PerformanceHudElement;
import de.horizon.hud.RevivalStatusHudElement;
import de.horizon.hud.SystemStatsHudElement;
import de.horizon.hud.PurplePadTimerHudElement;
import de.horizon.hud.RelicTimerHudElement;
import de.horizon.hud.TickTimerHudElement;
import de.horizon.hud.TimeHudElement;
import de.horizon.screen.HorizonConfigScreen;
import de.horizon.screen.PlayerProfileScreen;
import de.horizon.feature.inventory.InventoryButtonOverlay;
import de.horizon.feature.inventory.InventoryButtonService;
import de.horizon.mixin.AbstractContainerScreenAccessor;
import de.horizon.spotify.SpotifyInventoryOverlay;
import de.horizon.spotify.SpotifyService;
import de.horizon.youtube.YoutubeMusicInventoryOverlay;
import de.horizon.youtube.YoutubeService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.ChatFormatting;

import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;

public final class HorizonClient implements ClientModInitializer {
    private static HorizonClient instance;
    private static final Pattern FORMATTING_STRIP = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");
    private static final KeyMapping.Category HORIZON_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("horizon", "controls"));

    private final ConfigManager configManager = new ConfigManager();
    private final SpamHider spamHider = new SpamHider();
    private final ChatTabManager chatTabManager = new ChatTabManager();
    private final ReviveTracker reviveTracker = new ReviveTracker();
    private final DungeonAlertService dungeonAlertService = new DungeonAlertService();
    private final FishingAlertService fishingAlertService = new FishingAlertService();
    private final DungeonStateService dungeonStateService = new DungeonStateService();
    private final DungeonRoomDetector dungeonRoomDetector = new DungeonRoomDetector();
    private final HudRegistry hudRegistry = new HudRegistry();
    private final ParticleFilterService particleFilterService = new ParticleFilterService(configManager);
    private final TpsTracker tpsTracker = new TpsTracker();
    private final PingService pingService = new PingService();
    private final SystemStatsService systemStatsService = new SystemStatsService();
    private final SpotifyService spotifyService = new SpotifyService(configManager);
    private final SpotifyInventoryOverlay spotifyInventoryOverlay = new SpotifyInventoryOverlay(spotifyService);
    private final YoutubeService youtubeService = new YoutubeService(configManager);
    private final YoutubeMusicInventoryOverlay youtubeMusicInventoryOverlay = new YoutubeMusicInventoryOverlay(youtubeService);
    private final InventoryButtonService inventoryButtonService = new InventoryButtonService(configManager);
    private final InventoryButtonOverlay inventoryButtonOverlay = new InventoryButtonOverlay(configManager, inventoryButtonService);
    private final HypixelProfileService hypixelProfileService = new HypixelProfileService(configManager);
    private final HorizonApiAuthService horizonApiAuthService = new HorizonApiAuthService(configManager);
    private final HorizonApiClient horizonApiClient = new HorizonApiClient(configManager, horizonApiAuthService);
    private final HorizonProfileGateway horizonProfileGateway = new HorizonProfileGateway(horizonApiClient);
    private final PartyFinderOverlay partyFinderOverlay = new PartyFinderOverlay(hypixelProfileService);
    private final HypixelSidebarOverlay hypixelSidebarOverlay = new HypixelSidebarOverlay();
    private final LeapMenuOverlay leapMenuOverlay = new LeapMenuOverlay();
    private final EtherwarpHelperService etherwarpHelperService = new EtherwarpHelperService();
    private final WardrobeKeybindService wardrobeKeybindService = new WardrobeKeybindService();
    private final SlotBindService slotBindService = new SlotBindService();
    private final ChatCommandService chatCommandService = new ChatCommandService(pingService, tpsTracker, spotifyService);
    private final TickTimerService tickTimerService = new TickTimerService();
    private final PuzzleSolverService puzzleSolverService = new PuzzleSolverService();
    private final TerminalSolverService terminalSolverService = new TerminalSolverService();
    private final SimonSaysService simonSaysService = new SimonSaysService();
    private final ArrowAlignService arrowAlignService = new ArrowAlignService();
    private final SharpShooterService sharpShooterService = new SharpShooterService();
    private final PurplePadTimerService purplePadTimerService = new PurplePadTimerService();
    private final BloodCamperService bloodCamperService = new BloodCamperService();
    private final DungeonScoreService dungeonScoreService = new DungeonScoreService();
    private final DragonService dragonService = new DragonService();
    private final RelicTimerService relicTimerService = new RelicTimerService();
    private final MimicService mimicService = new MimicService();
    private final SpiritBearService spiritBearService = new SpiritBearService();
    private final DungeonMapService dungeonMapService = new DungeonMapService();
    private final DoorEspService doorEspService = new DoorEspService();
    private final TeammateGlowService teammateGlowService = new TeammateGlowService();
    private boolean quizColoringSending = false;
    private KeyMapping openConfigKeyBinding;
    private Screen pendingScreen;
    private final java.util.Set<Integer> pressedLastTick = new java.util.HashSet<>();

    public static HorizonClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        HorizonMod.LOGGER.info("Initializing Horizon client");

        HorizonSounds.register();
        configManager.load();
        Lang.set(configManager.getConfig().getLanguage());
        hudRegistry.register(new RevivalStatusHudElement(configManager, reviveTracker, dungeonStateService));
        hudRegistry.register(new TimeHudElement());
        hudRegistry.register(new PerformanceHudElement());
        hudRegistry.register(new SystemStatsHudElement());
        hudRegistry.register(new TickTimerHudElement(tickTimerService, dungeonStateService));
        hudRegistry.register(new PurplePadTimerHudElement(purplePadTimerService));
        hudRegistry.register(new DungeonMapHudElement(dungeonMapService, dungeonStateService, teammateGlowService));
        hudRegistry.register(new DungeonScoreHudElement(dungeonScoreService, dungeonStateService));
        hudRegistry.register(new RelicTimerHudElement(relicTimerService));
        hudRegistry.register(new SpiritBearTimerHudElement(spiritBearService));
        openConfigKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.horizon.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            HORIZON_CATEGORY
        ));

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("horizon", "hud"), this::renderHud);
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            etherwarpHelperService.renderWorld(context, configManager.getConfig());
            puzzleSolverService.renderWorld(context, configManager.getConfig(), Minecraft.getInstance());
            simonSaysService.renderWorld(context, configManager.getConfig());
            arrowAlignService.renderWorld(context, configManager.getConfig());
            sharpShooterService.renderWorld(context, configManager.getConfig());
            bloodCamperService.renderWorld(context, Minecraft.getInstance(), configManager.getConfig().isBloodCamperEnabled());
            dragonService.renderWorld(context, configManager.getConfig());
            doorEspService.renderWorld(context, configManager.getConfig(), dungeonStateService.isInDungeon(), dungeonStateService.isInBoss());
            renderStarredMobHighlights(context);
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String raw = message.getString();
            dungeonStateService.handleChatMessage(raw);
            dungeonRoomDetector.handleChatMessage(raw);
            reviveTracker.handleChatMessage(raw, configManager.getConfig());
            fishingAlertService.handleChatMessage(raw, configManager.getConfig());
            handleRagAxeNotification(raw);
            handleChatCommand(raw);
            handleTickTimerMessage(raw);
            puzzleSolverService.handleChatMessage(raw, Minecraft.getInstance());
            simonSaysService.handleChatMessage(raw);
            bloodCamperService.handleChatMessage(raw, configManager.getConfig().isBloodCamperEnabled());
            dungeonScoreService.handleChatMessage(raw);
            dragonService.handleChatMessage(raw, dungeonStateService);
            relicTimerService.handleChatMessage(raw, dungeonStateService, configManager.getConfig());
            doorEspService.handleChatMessage(raw);
            mimicService.handleChatMessage(raw, configManager.getConfig());
            spiritBearService.handleChatMessage(raw, dungeonStateService);
            // Quiz answer coloring: replace option messages with colored versions
            if (!quizColoringSending && configManager.getConfig().isPuzzleSolverEnabled()) {
                var colored = puzzleSolverService.colorQuizOption(raw);
                if (colored != null) {
                    quizColoringSending = true;
                    Minecraft.getInstance().player.sendSystemMessage(colored);
                    quizColoringSending = false;
                    return false;
                }
            }
            return !spamHider.shouldHide(raw, configManager.getConfig(), dungeonStateService.isInDungeon())
                    && !fishingAlertService.shouldHideMessage(raw, configManager.getConfig());
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String raw = message.getString();
            dungeonStateService.handleChatMessage(raw);
            dungeonRoomDetector.handleChatMessage(raw);
            reviveTracker.handleChatMessage(raw, configManager.getConfig());
            fishingAlertService.handleChatMessage(raw, configManager.getConfig());
            handleChatCommand(raw);
            handleTickTimerMessage(raw);
            puzzleSolverService.handleChatMessage(raw, Minecraft.getInstance());
            simonSaysService.handleChatMessage(raw);
            bloodCamperService.handleChatMessage(raw, configManager.getConfig().isBloodCamperEnabled());
            dungeonScoreService.handleChatMessage(raw);
            dragonService.handleChatMessage(raw, dungeonStateService);
            relicTimerService.handleChatMessage(raw, dungeonStateService, configManager.getConfig());
            doorEspService.handleChatMessage(raw);
            mimicService.handleChatMessage(raw, configManager.getConfig());
            spiritBearService.handleChatMessage(raw, dungeonStateService);
            if (!quizColoringSending && configManager.getConfig().isPuzzleSolverEnabled()) {
                var colored = puzzleSolverService.colorQuizOption(raw);
                if (colored != null) {
                    quizColoringSending = true;
                    Minecraft.getInstance().player.sendSystemMessage(colored);
                    quizColoringSending = false;
                    return false;
                }
            }
            return !spamHider.shouldHide(raw, configManager.getConfig(), dungeonStateService.isInDungeon())
                    && !fishingAlertService.shouldHideMessage(raw, configManager.getConfig());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            inventoryButtonService.onDisconnect();
            resetDungeonServices();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            resetDungeonServices());
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> !executeLocalCommand(command, Minecraft.getInstance() == null ? null : Minecraft.getInstance().screen));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("horizon")
                .executes(context -> {
                    openConfigScreen(null);
                    return 1;
                }))
        );
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("hv")
                .executes(context -> openProfileScreen(""))
                .then(ClientCommands.argument("player", StringArgumentType.greedyString())
                    .executes(context -> openProfileScreen(StringArgumentType.getString(context, "player")))))
        );
        registerScreenHooks();
    }

    public boolean executeLocalCommand(String command, Screen parentScreen) {
        String trimmed = command == null ? "" : command.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        Screen normalizedParent = normalizeCommandParent(parentScreen);
        if (trimmed.equalsIgnoreCase("horizon")) {
            HorizonMod.LOGGER.info("Opening Horizon config through command fallback");
            openConfigScreen(normalizedParent);
            return true;
        }
        if (trimmed.equalsIgnoreCase("hv")) {
            HorizonMod.LOGGER.info("Opening Horizon profile through command fallback");
            openProfileScreen("", normalizedParent);
            return true;
        }
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("hv ")) {
            HorizonMod.LOGGER.info("Opening Horizon profile for {} through command fallback", trimmed.substring(3).trim());
            openProfileScreen(trimmed.substring(3), normalizedParent);
            return true;
        }
        // Command shortcuts (/f1-/f7, /m1-/m7, /d, /dh)
        String shortcutCommand = resolveCommandShortcut(lower);
        if (shortcutCommand != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.connection.sendCommand(shortcutCommand);
            }
            return true;
        }
        return false;
    }

    private static final String[] FLOOR_NAMES = {
        "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN"
    };

    private String resolveCommandShortcut(String lower) {
        if (!configManager.getConfig().isCommandShortcutsEnabled()) return null;
        if (lower.equals("d") || lower.equals("dh")) {
            return "warp dungeon_hub";
        }
        if (lower.length() == 2) {
            char prefix = lower.charAt(0);
            char digit = lower.charAt(1);
            if (digit >= '1' && digit <= '7') {
                int floor = digit - '1';
                if (prefix == 'f') {
                    return "joininstance CATACOMBS_FLOOR_" + FLOOR_NAMES[floor];
                } else if (prefix == 'm') {
                    return "joininstance MASTER_CATACOMBS_FLOOR_" + FLOOR_NAMES[floor];
                }
            }
        }
        return null;
    }

    private void onClientTick(Minecraft client) {
        if (pendingScreen != null) {
            Screen nextScreen = pendingScreen;
            pendingScreen = null;
            client.setScreen(nextScreen);
            return;
        }
        horizonApiAuthService.tick();
        dungeonStateService.tick(client);
        dungeonRoomDetector.tick(client, dungeonStateService);
        dungeonAlertService.tick(client, configManager.getConfig(), dungeonStateService, dungeonRoomDetector);
        etherwarpHelperService.tick(configManager.getConfig());
        tickTimerService.tick(dungeonStateService);
        purplePadTimerService.tick();
        puzzleSolverService.tick(client, dungeonStateService, dungeonRoomDetector, configManager.getConfig());
        simonSaysService.tick(client, dungeonStateService, configManager.getConfig());
        arrowAlignService.tick(client, dungeonStateService, configManager.getConfig());
        dungeonScoreService.tick(client, dungeonStateService);
        dragonService.tick(client, dungeonStateService, configManager.getConfig());
        relicTimerService.tick();
        spiritBearService.tick(client, configManager.getConfig());
        teammateGlowService.tick(client, dungeonStateService.isInDungeon());
        if (dungeonStateService.isInDungeon()) {
            StarredMobService.tick(client);
            bloodCamperService.tick(client);
            doorEspService.tick(client, true, dungeonStateService.isInBoss(), dungeonRoomDetector);
            if (!dungeonStateService.isInBoss()) {
                dungeonMapService.scan(client, dungeonRoomDetector, dungeonStateService.getCurrentFloor());
            }
        }
        pingService.tick(client);
        reviveTracker.tick();
        fishingAlertService.tick(client, configManager.getConfig());
        inventoryButtonService.tick(client);
        while (openConfigKeyBinding != null && openConfigKeyBinding.consumeClick()) {
            HorizonMod.LOGGER.info("Opening Horizon config through keybind");
            openConfigScreen(client.screen);
        }
        tickCommandKeybinds(client);
    }

    private void tickCommandKeybinds(Minecraft mc) {
        if (mc == null || mc.screen != null || mc.player == null || mc.getWindow() == null) {
            pressedLastTick.clear();
            return;
        }
        long window = mc.getWindow().handle();
        checkAndFireKey(window, configManager.getConfig().getCommandKeybindPets(), "pets");
        checkAndFireKey(window, configManager.getConfig().getCommandKeybindEquipment(), "equipment");
        checkAndFireKey(window, configManager.getConfig().getCommandKeybindWardrobe(), "wardrobe");
    }

    private void checkAndFireKey(long window, int keyCode, String command) {
        if (keyCode < 0) return;
        boolean down = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
        if (down && !pressedLastTick.contains(keyCode)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.connection.sendCommand(command);
            }
        }
        if (down) pressedLastTick.add(keyCode);
        else pressedLastTick.remove(keyCode);
    }

    private void handleChatCommand(String raw) {
        String cmd = chatCommandService.handleMessage(raw, configManager.getConfig());
        if (cmd != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.connection.sendCommand(cmd);
            }
        }
    }

    private void handleTickTimerMessage(String raw) {
        tickTimerService.handleChatMessage(raw, dungeonStateService, configManager.getConfig());
        purplePadTimerService.handleChatMessage(raw, configManager.getConfig());
    }

    public void openConfigScreen(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        pendingScreen = new HorizonConfigScreen(parent, this);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ReviveTracker getReviveTracker() {
        return reviveTracker;
    }

    public HudRegistry getHudRegistry() {
        return hudRegistry;
    }

    public SpotifyService getSpotifyService() {
        return spotifyService;
    }

    public YoutubeService getYoutubeService() {
        return youtubeService;
    }

    public HypixelProfileService getHypixelProfileService() {
        return hypixelProfileService;
    }

    public ParticleFilterService getParticleFilterService() {
        return particleFilterService;
    }

    public HorizonApiAuthService getHorizonApiAuthService() {
        return horizonApiAuthService;
    }

    public TpsTracker getTpsTracker() {
        return tpsTracker;
    }

    public PingService getPingService() {
        return pingService;
    }

    public DungeonStateService getDungeonStateService() {
        return dungeonStateService;
    }

    public DungeonMapService getDungeonMapService() {
        return dungeonMapService;
    }

    public TeammateGlowService getTeammateGlowService() {
        return teammateGlowService;
    }

    public ChatTabManager getChatTabManager() {
        return chatTabManager;
    }

    private void resetDungeonServices() {
        dungeonStateService.onWorldChange();
        StarredMobService.onWorldChange();
        teammateGlowService.onWorldChange();
        doorEspService.reset();
        dungeonMapService.reset();
        tickTimerService.reset();
        purplePadTimerService.reset();
        simonSaysService.reset();
        arrowAlignService.reset();
        sharpShooterService.reset();
        terminalSolverService.reset();
        bloodCamperService.reset();
        dungeonScoreService.reset();
        dragonService.reset();
        relicTimerService.reset();
        spiritBearService.reset();
        mimicService.reset();
    }

    public void onMimicKill() {
        mimicService.onBabyZombieDeath(dungeonStateService, configManager.getConfig());
    }

    public void onDragonParticle(int x, int z) {
        dragonService.onDragonParticle(x, z);
    }

    public void onMapItemData(byte[] colors, Iterable<MapDecoration> decorations, int centerX, int centerZ, byte scale) {
        if (dungeonStateService.isInDungeon()) {
            // Only accept map data with player markers (dungeon map), skip TicTacToe/quiz maps
            boolean hasMarkers = decorations != null && decorations.iterator().hasNext();
            if (hasMarkers) {
                dungeonMapService.onMapData(colors, decorations, centerX, centerZ, scale);
            }
        }
    }

    public void onTeleportMaze(double newX, double newZ, double oldX, double oldZ, float yaw) {
        puzzleSolverService.onTeleport(newX, newZ, oldX, oldZ, yaw);
    }

    public void onBlockInteract(BlockPos pos) {
        simonSaysService.onBlockInteract(pos, configManager.getConfig());
        puzzleSolverService.onBlockInteract(pos);
    }

    public void onBlockUpdate(BlockPos pos, BlockState newState, BlockState oldState, Minecraft mc) {
        puzzleSolverService.onBlockChange(pos, mc);
        simonSaysService.onBlockUpdate(pos, newState);
        sharpShooterService.onBlockUpdate(pos, oldState, newState);
        spiritBearService.onBlockUpdate(pos, newState, oldState);
    }

    public void onSimonSaysReset() {
        simonSaysService.onSectionReset(16);
    }

    public TerminalSolverService getTerminalSolverService() {
        return terminalSolverService;
    }

    public SystemStatsService getSystemStatsService() {
        return systemStatsService;
    }

    public InventoryButtonService getInventoryButtonService() {
        return inventoryButtonService;
    }

    public InventoryButtonOverlay getInventoryButtonOverlay() {
        return inventoryButtonOverlay;
    }

    public SlotBindService getSlotBindService() {
        return slotBindService;
    }

    private int openProfileScreen(String player) {
        return openProfileScreen(player, Minecraft.getInstance() == null ? null : Minecraft.getInstance().screen);
    }

    private int openProfileScreen(String player, Screen parent) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return 0;
        }

        String target = player == null || player.isBlank() ? client.player.getName().getString() : player.trim();
        pendingScreen = new PlayerProfileScreen(normalizeCommandParent(parent), target, horizonProfileGateway);
        return 1;
    }

    private void handleRagAxeNotification(String raw) {
        if (!configManager.getConfig().isRagAxeNotificationEnabled()) return;
        if (!dungeonStateService.isInDungeon()) return;
        String plain = FORMATTING_STRIP.matcher(raw).replaceAll("").toLowerCase(java.util.Locale.ROOT);
        if (!plain.contains("i no longer wish to fight, but i know that will not stop you")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui == null) return;
        mc.gui.setTitle(net.minecraft.network.chat.Component.literal("Rag!").withStyle(ChatFormatting.GOLD));
        mc.gui.setTimes(5, 40, 10);
    }

    private Screen normalizeCommandParent(Screen parent) {
        return parent instanceof ChatScreen ? null : parent;
    }

    private void renderStarredMobHighlights(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
        if (!dungeonStateService.isInDungeon()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        var config = configManager.getConfig();

        boolean showFels = config.isHighlightFelsEnabled();
        if (!showFels) return;

        int felColor  = (config.getFelHighlightColor() & 0x00FFFFFF) | 0x60000000;

        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof net.minecraft.client.player.LocalPlayer) continue;
            if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) continue;

            if (StarredMobService.isFel(e)) {
                net.minecraft.world.phys.AABB bb = e.getBoundingBox();
                de.horizon.feature.dungeon.puzzle.DungeonRenderUtil.drawBox(ctx, bb, felColor, 2, false);
            }
        }
    }

    private void renderHud(GuiGraphicsExtractor drawContext, net.minecraft.client.DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options.hideGui || client.player == null) {
            return;
        }

        int barScaled = PillarboxState.scaledBarWidth();
        if (barScaled > 0) {
            drawContext.pose().pushMatrix();
            drawContext.pose().translate(barScaled, 0.0f);
        }

        for (HudElement element : hudRegistry.getElements()) {
            if (!element.isEnabled(configManager.getConfig())) {
                continue;
            }
            element.render(drawContext, client, configManager.getOrCreatePosition(element.id(), element.defaultX(), element.defaultY()), false);
        }
        if (configManager.getConfig().isCustomScoreboardEnabled()) {
            hypixelSidebarOverlay.render(drawContext, client);
        }

        if (barScaled > 0) {
            drawContext.pose().popMatrix();
        }
        renderPillarboxBars(drawContext, client);
    }

    private void renderPillarboxBars(GuiGraphicsExtractor drawContext, Minecraft client) {
        if (!configManager.getConfig().isPillarboxEnabled()) return;
        int fbW = client.getWindow().getWidth();
        int fbH = client.getWindow().getHeight();
        if ((long) fbW * 9 <= (long) fbH * 16) return;
        int scaledW = client.getWindow().getGuiScaledWidth();
        int scaledH = client.getWindow().getGuiScaledHeight();
        int targetFbW = fbH * 16 / 9;
        int barFbW = (fbW - targetFbW) / 2;
        int sf = Math.max(1, Math.round((float) fbH / scaledH));
        int barScaled = (int) Math.ceil((double) barFbW / sf);
        drawContext.fill(0, 0, barScaled, scaledH, 0xFF000000);
        drawContext.fill(scaledW - barScaled, 0, scaledW, scaledH, 0xFF000000);
    }

    private void registerScreenHooks() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> handledScreen)) {
                return;
            }

            // Detect terminal screen opens and set custom mode flag
            if (configManager.getConfig().isTerminalSolverEnabled()) {
                terminalSolverService.onScreenOpen(handledScreen);
            }
            terminalSolverService.updateCustomModeFlag(configManager.getConfig());

            ScreenEvents.afterExtract(screen).register((currentScreen, context, mouseX, mouseY, delta) -> {
                if ("YOUTUBE_MUSIC".equals(configManager.getConfig().getActiveMusicService())) {
                    youtubeMusicInventoryOverlay.render(handledScreen, context, mouseX, mouseY);
                } else {
                    spotifyInventoryOverlay.render(handledScreen, context, mouseX, mouseY);
                }
                partyFinderOverlay.render(handledScreen, context);
                // Leap Menu overlay
                if (leapMenuOverlay.isLeapScreen(handledScreen)) {
                    leapMenuOverlay.render(handledScreen, context, mouseX, mouseY, configManager.getConfig());
                }
                // Terminal solver rendering
                if (configManager.getConfig().isTerminalSolverEnabled()) {
                    terminalSolverService.onScreenTick(handledScreen);
                    terminalSolverService.render(handledScreen, context, configManager.getConfig());
                }
                // Slot Bind visual feedback
                var sbAccessor = (AbstractContainerScreenAccessor)(Object) handledScreen;
                int sbKey = configManager.getConfig().getSlotBindKey();
                Minecraft sbMc = Minecraft.getInstance();
                boolean showActive = sbKey >= 0 && sbMc != null
                    && org.lwjgl.glfw.GLFW.glfwGetKey(sbMc.getWindow().handle(), sbKey) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                slotBindService.renderOverlay(context, handledScreen, sbAccessor.getLeftPos(), sbAccessor.getTopPos(),
                    mouseX, mouseY, configManager.getConfig(), showActive);
            });
            ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, click) -> {
                // Leap Menu: intercept clicks on the Spirit Leap screen
                if (leapMenuOverlay.isLeapScreen(handledScreen) && configManager.getConfig().isLeapMenuEnabled()) {
                    int slotIdx = leapMenuOverlay.getClickedSlot(handledScreen, (int) click.x(), (int) click.y(), configManager.getConfig());
                    if (slotIdx >= 0) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            mc.gameMode.handleContainerInput(
                                handledScreen.getMenu().containerId, slotIdx, 0, ContainerInput.PICKUP, mc.player);
                        }
                        return false; // cancel vanilla click
                    }
                }
                // Terminal solver: block wrong clicks when solver is active
                if (configManager.getConfig().isTerminalSolverEnabled()
                    && terminalSolverService.getCurrentType() != TerminalSolverService.TerminalType.NONE
                    && terminalSolverService.getCurrentType() != TerminalSolverService.TerminalType.MELODY) {
                    // Custom mode: map click via custom grid
                    if (TerminalSolverService.isCustomModeRendering()) {
                        int customSlot = terminalSolverService.getCustomModeSlotIndex(click.x(), click.y());
                        if (customSlot < 0) return false; // click outside grid → block
                        boolean isLeft = click.button() == 0;
                        if (terminalSolverService.shouldBlockClick(customSlot, isLeft)) return false;
                        terminalSolverService.onSlotClicked(customSlot);
                        // Send container click for this slot
                        Minecraft mc2 = Minecraft.getInstance();
                        if (mc2 != null && mc2.player != null && mc2.gameMode != null) {
                            mc2.gameMode.handleContainerInput(
                                handledScreen.getMenu().containerId, customSlot,
                                isLeft ? 0 : 1, ContainerInput.PICKUP, mc2.player);
                        }
                        return false; // consume click
                    }
                    // Normal mode: check vanilla slot positions
                    var accessor = (AbstractContainerScreenAccessor)(Object) handledScreen;
                    for (var s : handledScreen.getMenu().slots) {
                        int sx = accessor.getLeftPos() + s.x;
                        int sy = accessor.getTopPos() + s.y;
                        if (click.x() >= sx && click.x() < sx + 16 && click.y() >= sy && click.y() < sy + 16) {
                            boolean isLeft = click.button() == 0;
                            if (terminalSolverService.shouldBlockClick(s.index, isLeft)) return false;
                            terminalSolverService.onSlotClicked(s.index);
                            break;
                        }
                    }
                }
                // Slot Binds: intercept shift-clicks in player inventory
                if (click.button() == 0 && currentScreen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && (click.modifiers() & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0) {
                        var accessor = (AbstractContainerScreenAccessor)(Object) handledScreen;
                        for (var s : handledScreen.getMenu().slots) {
                            int sx = accessor.getLeftPos() + s.x;
                            int sy = accessor.getTopPos() + s.y;
                            if (click.x() >= sx && click.x() < sx + 16 && click.y() >= sy && click.y() < sy + 16) {
                                if (slotBindService.handleShiftClick(handledScreen.getMenu(), s.index, configManager.getConfig(), mc)) {
                                    return false; // cancel vanilla click
                                }
                                break;
                            }
                        }
                    }
                }
                return true;
            });
            ScreenMouseEvents.afterMouseClick(screen).register((currentScreen, click, doubled) -> {
                if ("YOUTUBE_MUSIC".equals(configManager.getConfig().getActiveMusicService())) {
                    youtubeMusicInventoryOverlay.mouseClicked(click.x(), click.y(), click.button());
                } else {
                    spotifyInventoryOverlay.mouseClicked(click.x(), click.y(), click.button());
                }
                inventoryButtonOverlay.mouseClicked(click.x(), click.y(), click.button());
                return false;
            });
            ScreenMouseEvents.afterMouseDrag(screen).register((currentScreen, click, deltaX, deltaY, cancelled) ->
                "YOUTUBE_MUSIC".equals(configManager.getConfig().getActiveMusicService())
                    ? youtubeMusicInventoryOverlay.mouseDragged(click.x(), click.y(), click.button())
                    : spotifyInventoryOverlay.mouseDragged(click.x(), click.y(), click.button())
            );
            ScreenMouseEvents.afterMouseRelease(screen).register((currentScreen, click, cancelled) ->
                "YOUTUBE_MUSIC".equals(configManager.getConfig().getActiveMusicService())
                    ? youtubeMusicInventoryOverlay.mouseReleased(click.x(), click.y(), click.button())
                    : spotifyInventoryOverlay.mouseReleased(click.x(), click.y(), click.button())
            );
            // allowKeyPress fires before vanilla processing → can cancel hotbar-swap for wardrobe
            ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, input) -> {
                // Terminal solver custom mode: drop key acts as left click on hovered solver slot
                if (TerminalSolverService.isCustomModeRendering()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.options.keyDrop.matches(input)) {
                        double mouseX = mc.mouseHandler.getScaledXPos(mc.getWindow());
                        double mouseY = mc.mouseHandler.getScaledYPos(mc.getWindow());
                        int customSlot = terminalSolverService.getCustomModeSlotIndex(mouseX, mouseY);
                        if (customSlot >= 0 && !terminalSolverService.shouldBlockClick(customSlot, true)) {
                            terminalSolverService.onSlotClicked(customSlot);
                            if (mc.gameMode != null && mc.player != null) {
                                mc.gameMode.handleContainerInput(
                                    handledScreen.getMenu().containerId, customSlot, 0,
                                    ContainerInput.PICKUP, mc.player);
                            }
                        }
                        return false; // block default drop behavior
                    }
                }
                if (wardrobeKeybindService.handleKeyPress(handledScreen, input.key(), configManager.getConfig())) {
                    return false; // cancel vanilla (prevents hotbar slot swap)
                }
                return true;
            });
            ScreenKeyboardEvents.afterKeyPress(screen).register((currentScreen, input) -> {
                if ("YOUTUBE_MUSIC".equals(configManager.getConfig().getActiveMusicService())) {
                    youtubeMusicInventoryOverlay.keyPressed(input.key());
                } else {
                    spotifyInventoryOverlay.keyPressed(input.key());
                }
                // Slot bind key
                int sbKey = configManager.getConfig().getSlotBindKey();
                if (sbKey >= 0 && input.key() == sbKey) {
                    var accessor = (AbstractContainerScreenAccessor)(Object) handledScreen;
                    var hoveredSlot = accessor.getHoveredSlot();
                    if (hoveredSlot != null) {
                        Minecraft mc2 = Minecraft.getInstance();
                        String msg = slotBindService.handleBindKeyPress(hoveredSlot.index, configManager.getConfig(), mc2);
                        if (msg != null && mc2 != null && mc2.gui != null) {
                            mc2.gui.setOverlayMessage(net.minecraft.network.chat.Component.literal(msg), false);
                        }
                    }
                }
            });
        });
    }

}
