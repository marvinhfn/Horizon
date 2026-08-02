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
    private final de.horizon.feature.misc.LoadoutKeybindService loadoutKeybindService = new de.horizon.feature.misc.LoadoutKeybindService();
    private final SlotBindService slotBindService = new SlotBindService();
    private final ChatCommandService chatCommandService = new ChatCommandService(pingService, tpsTracker, spotifyService);
    private final TickTimerService tickTimerService = new TickTimerService();
    private final PuzzleSolverService puzzleSolverService = new PuzzleSolverService();
    private final TerminalSolverService terminalSolverService = new TerminalSolverService();
    private final de.horizon.feature.helper.ExperimentTableSolver experimentTableSolver = new de.horizon.feature.helper.ExperimentTableSolver();
    private final de.horizon.feature.dungeon.terminal.TerminalWaypointService terminalWaypointService = new de.horizon.feature.dungeon.terminal.TerminalWaypointService();
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
    private final de.horizon.feature.dungeon.SoulweaverService soulweaverService = new de.horizon.feature.dungeon.SoulweaverService();
    private final DungeonMapService dungeonMapService = new DungeonMapService();
    private final DoorEspService doorEspService = new DoorEspService();
    private final TeammateGlowService teammateGlowService = new TeammateGlowService();
    private final de.horizon.feature.skyblock.MayorService mayorService = new de.horizon.feature.skyblock.MayorService();
    private final de.horizon.feature.skyblock.SkyblockPriceService priceService = new de.horizon.feature.skyblock.SkyblockPriceService();
    private final de.horizon.feature.dungeon.ChestProfitService chestProfitService = new de.horizon.feature.dungeon.ChestProfitService(priceService);
    private final de.horizon.feature.skyblock.ItemCraftValueService craftValueService = new de.horizon.feature.skyblock.ItemCraftValueService(priceService);
    private final de.horizon.feature.inventory.PetHighlightService petHighlightService = new de.horizon.feature.inventory.PetHighlightService();
    private final de.horizon.feature.dungeon.secret.SecretWaypointService secretWaypointService = new de.horizon.feature.dungeon.secret.SecretWaypointService();
    private final de.horizon.feature.storage.StorageOverlayService storageOverlayService = new de.horizon.feature.storage.StorageOverlayService();
    private final de.horizon.feature.waypoint.WaypointService waypointService = new de.horizon.feature.waypoint.WaypointService();
    private boolean quizColoringSending = false;
    private KeyMapping openConfigKeyBinding;
    private Screen pendingScreen;
    private final java.util.Set<Integer> pressedLastTick = new java.util.HashSet<>();

    public static HorizonClient getInstance() {
        return instance;
    }

    // Real server-tick counter (incremented once per bundle packet = once per server tick). HUD timers
    // anchor to this so they freeze when the server tick freezes instead of running on client time.
    private static volatile long serverTicks = 0L;
    public static void onServerTick() { serverTicks++; }
    public static long serverTicks() { return serverTicks; }

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
        hudRegistry.register(new de.horizon.hud.BlessingHudElement(dungeonStateService));
        waypointService.wire(dungeonStateService, dungeonRoomDetector);
        hudRegistry.register(new RelicTimerHudElement(relicTimerService));
        hudRegistry.register(new SpiritBearTimerHudElement(spiritBearService));
        hudRegistry.register(new de.horizon.hud.DragonSpawnHudElement(dragonService, configManager));
        openConfigKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.horizon.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            HORIZON_CATEGORY
        ));

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("horizon", "hud"), this::renderHud);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() && hitResult != null) {
                secretWaypointService.onBlockInteract(hitResult.getBlockPos());
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            // Each render is isolated: a transient exception in one service must not
            // abort the remaining renders for the frame (that made secrets/puzzle/door
            // renders blink out together intermittently).
            safeRender("etherwarp", () -> etherwarpHelperService.renderWorld(context, configManager.getConfig()));
            safeRender("puzzle", () -> puzzleSolverService.renderWorld(context, configManager.getConfig(), Minecraft.getInstance()));
            safeRender("simonSays", () -> simonSaysService.renderWorld(context, configManager.getConfig()));
            safeRender("arrowAlign", () -> arrowAlignService.renderWorld(context, configManager.getConfig()));
            safeRender("sharpShooter", () -> sharpShooterService.renderWorld(context, configManager.getConfig()));
            safeRender("bloodCamper", () -> bloodCamperService.renderWorld(context, Minecraft.getInstance(), configManager.getConfig().isBloodCamperEnabled()));
            safeRender("dragon", () -> dragonService.renderWorld(context, configManager.getConfig()));
            safeRender("relic", () -> relicTimerService.renderWorld(context, dungeonStateService, configManager.getConfig()));
            safeRender("doorEsp", () -> doorEspService.renderWorld(context, configManager.getConfig(), dungeonStateService.isInDungeon(), dungeonStateService.isInBoss()));
            safeRender("secretWaypoint", () -> secretWaypointService.renderWorld(context, configManager.getConfig(), dungeonRoomDetector, dungeonStateService.isInDungeon(), dungeonStateService.isInBoss()));
            safeRender("terminalWaypoint", () -> terminalWaypointService.renderWorld(context, configManager.getConfig()));
            safeRender("starredMobs", () -> renderStarredMobHighlights(context));
            safeRender("waypoints", () -> waypointService.renderWorld(context));
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        // Item tooltips: append price/craft lines and recolour maxed enchants. Uses the Fabric callback
        // (fires at the end of the vanilla tooltip build) rather than a mixin, for mod compatibility.
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register(
            (stack, tooltipContext, tooltipType, tooltipLines) -> decorateTooltip(stack, tooltipLines));
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String raw = message.getString();
            dungeonStateService.handleChatMessage(raw);
            // Fresh instance: the boss warp keeps the map/rooms (JOIN doesn't reset them), so a NEW
            // run must drop the previous run's map + room calibration here — otherwise run 2+ is stuck
            // on stale state until something re-syncs near the boss.
            if (raw.toLowerCase(java.util.Locale.ROOT).contains("dungeon starts in")) {
                dungeonMapService.reset();
                dungeonRoomDetector.reset();
            }
            dungeonRoomDetector.handleChatMessage(raw);
            reviveTracker.handleChatMessage(raw, configManager.getConfig());
            fishingAlertService.handleChatMessage(raw, configManager.getConfig());
            handleRagAxeNotification(raw);
            handleChatCommand(raw);
            handleTickTimerMessage(raw);
            puzzleSolverService.handleChatMessage(raw, Minecraft.getInstance());
            simonSaysService.handleChatMessage(raw);
            sharpShooterService.handleChatMessage(raw, configManager.getConfig());
            bloodCamperService.handleChatMessage(raw, configManager.getConfig().isBloodCamperEnabled());
            dungeonScoreService.handleChatMessage(raw);
            dragonService.handleChatMessage(raw, dungeonStateService);
            relicTimerService.handleChatMessage(raw, dungeonStateService, configManager.getConfig());
            doorEspService.handleChatMessage(raw);
            secretWaypointService.handleChatMessage(raw, dungeonRoomDetector);
            terminalWaypointService.handleChatMessage(raw, Minecraft.getInstance());
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
            // Fresh instance: the boss warp keeps the map/rooms (JOIN doesn't reset them), so a NEW
            // run must drop the previous run's map + room calibration here — otherwise run 2+ is stuck
            // on stale state until something re-syncs near the boss.
            if (raw.toLowerCase(java.util.Locale.ROOT).contains("dungeon starts in")) {
                dungeonMapService.reset();
                dungeonRoomDetector.reset();
            }
            dungeonRoomDetector.handleChatMessage(raw);
            reviveTracker.handleChatMessage(raw, configManager.getConfig());
            fishingAlertService.handleChatMessage(raw, configManager.getConfig());
            handleChatCommand(raw);
            handleTickTimerMessage(raw);
            puzzleSolverService.handleChatMessage(raw, Minecraft.getInstance());
            simonSaysService.handleChatMessage(raw);
            sharpShooterService.handleChatMessage(raw, configManager.getConfig());
            bloodCamperService.handleChatMessage(raw, configManager.getConfig().isBloodCamperEnabled());
            dungeonScoreService.handleChatMessage(raw);
            dragonService.handleChatMessage(raw, dungeonStateService);
            relicTimerService.handleChatMessage(raw, dungeonStateService, configManager.getConfig());
            doorEspService.handleChatMessage(raw);
            secretWaypointService.handleChatMessage(raw, dungeonRoomDetector);
            terminalWaypointService.handleChatMessage(raw, Minecraft.getInstance());
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
            resetDungeonServices(true);
        });
        // A JOIN also fires on the in-dungeon boss warp (a server transfer). Do NOT reset the dungeon
        // STATE there, or the map/puzzle/renders would blank on every boss warp — the tick latch and
        // the "dungeon starts" chat handle genuine instance changes instead.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            resetDungeonServices(false));
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> !executeLocalCommand(command, Minecraft.getInstance() == null ? null : Minecraft.getInstance().screen));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("horizon")
                .then(ClientCommands.literal("lookcords")
                    .executes(context -> { toggleLookCoords(); return 1; }))
                .then(ClientCommands.literal("waypoints")
                    .executes(context -> {
                        pendingScreen = new de.horizon.screen.WaypointScreen(null, waypointService, null);
                        return 1;
                    }))
                .then(ClientCommands.literal("price")
                    .executes(context -> { printHeldItemPrice(); return 1; }))
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
        // Front Cam: skip the front-facing third-person view so F5 only toggles 1st/3rd person.
        if (configManager.getConfig().isFrontCamDisabled()
            && client.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT) {
            client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        }
        dungeonStateService.tick(client);
        dungeonRoomDetector.tick(client, dungeonStateService);
        dungeonAlertService.tick(client, configManager.getConfig(), dungeonStateService, dungeonRoomDetector);
        etherwarpHelperService.tick(configManager.getConfig());
        tickTimerService.tick(dungeonStateService);
        purplePadTimerService.tick();
        puzzleSolverService.tick(client, dungeonStateService, dungeonRoomDetector, configManager.getConfig());
        simonSaysService.tick(client, dungeonStateService, configManager.getConfig());
        arrowAlignService.tick(client, dungeonStateService, configManager.getConfig());
        sharpShooterService.tick(client, configManager.getConfig());
        terminalWaypointService.tick(client, configManager.getConfig(), dungeonStateService.isInDungeon());
        dungeonScoreService.tick(client, dungeonStateService);
        dragonService.tick(client, dungeonStateService, teammateGlowService, configManager.getConfig());
        relicTimerService.tick(client, dungeonStateService, configManager.getConfig());
        spiritBearService.tick(client, configManager.getConfig());
        if (configManager.getConfig().isSoulweaverSkullsHidden()) soulweaverService.tick(client);
        teammateGlowService.tick(client, dungeonStateService.isInDungeon());
        mayorService.tick();
        var pcfg = configManager.getConfig();
        if (pcfg.isCroesusProfitEnabled() || pcfg.isItemPriceTooltip()
                || pcfg.isBazaarValueTooltip() || pcfg.isAuctionValueTooltip()) {
            priceService.tick();
        }
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
        checkAndFireKey(window, configManager.getConfig().getCommandKeybindLoadouts(), "loadouts");
        checkAndFireKey(window, configManager.getConfig().getCommandKeybindStats(), "stats");
        for (var c : configManager.getConfig().getCustomCommandKeybinds()) {
            if (c.key >= 0 && c.command != null && !c.command.isBlank()) {
                checkAndFireKey(window, c.key, c.command.startsWith("/") ? c.command.substring(1) : c.command);
            }
        }
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

    public MimicService getMimicService() {
        return mimicService;
    }

    public DungeonMapService getDungeonMapService() {
        return dungeonMapService;
    }

    public TeammateGlowService getTeammateGlowService() {
        return teammateGlowService;
    }

    public de.horizon.feature.skyblock.MayorService getMayorService() {
        return mayorService;
    }

    public ChatTabManager getChatTabManager() {
        return chatTabManager;
    }

    private void resetDungeonServices() {
        resetDungeonServices(true);
    }

    /**
     * @param resetState when false (a JOIN, i.e. possibly the boss warp) the dungeon STATE
     *     (inDungeon/inBoss/floor/phase) is preserved so renders survive the warp; the per-encounter
     *     services still reset and refill from the new instance's packets/chat.
     */
    private void resetDungeonServices(boolean resetState) {
        // A JOIN/warp while NOT already in a dungeon = entering a fresh instance from the hub/another
        // island → clear the map + rooms IMMEDIATELY (so the previous run's map doesn't show until the
        // "dungeon starts in" countdown). A JOIN while already in a dungeon = the mid-run boss warp →
        // keep the map so it stays constant across that transfer.
        boolean newInstanceWarp = !resetState && !dungeonStateService.isInDungeon();
        if (resetState) dungeonStateService.onWorldChange();
        else dungeonStateService.onWarp(); // JOIN/server-transfer = a warp → clear the boss latch
        if (resetState || newInstanceWarp) {
            dungeonRoomDetector.reset();
            dungeonMapService.reset();
        }
        StarredMobService.onWorldChange();
        teammateGlowService.onWorldChange();
        doorEspService.reset();
        secretWaypointService.reset();
        tickTimerService.reset();
        purplePadTimerService.reset();
        simonSaysService.reset();
        arrowAlignService.reset();
        sharpShooterService.reset();
        terminalSolverService.reset();
        terminalWaypointService.reset();
        bloodCamperService.reset();
        dungeonScoreService.reset();
        dragonService.reset();
        relicTimerService.reset();
        spiritBearService.reset();
        soulweaverService.reset();
        mimicService.reset();
    }

    public void onMimicKill() {
        mimicService.onBabyZombieDeath(dungeonStateService, configManager.getConfig());
    }

    public void onDragonParticle(int x, int z) {
        dragonService.onDragonParticle(x, z);
    }

    public void onTabFooter(String footer) {
        dungeonStateService.onTabFooter(footer);
    }

    public void onMapItemData(byte[] colors, java.util.Map<String, MapDecoration> decorations, int centerX, int centerZ, byte scale) {
        if (dungeonStateService.isInDungeon()) {
            // Only accept map data with player markers (dungeon map), skip TicTacToe/quiz maps
            if (decorations != null && !decorations.isEmpty()) {
                dungeonMapService.onMapData(colors, decorations, centerX, centerZ, scale);
            }
        }
    }

    public void onTeleportMaze(double newX, double newZ, double oldX, double oldZ, float yaw) {
        puzzleSolverService.onTeleport(newX, newZ, oldX, oldZ, yaw);
    }

    /** Toggled by {@code /horizon lookcords}: print the coordinates of each right-clicked block. */
    private boolean lookCoordsEnabled = false;
    private BlockPos lastLookCoordsPos = null;
    private long lastLookCoordsTime = 0L;

    private void toggleLookCoords() {
        lookCoordsEnabled = !lookCoordsEnabled;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[HRZN] ")
            .withStyle(ChatFormatting.AQUA)
            .append(net.minecraft.network.chat.Component.literal(
                lookCoordsEnabled ? "LookCoords aktiviert — rechtsklicke einen Block." : "LookCoords deaktiviert.")
                .withStyle(ChatFormatting.WHITE)));
    }

    /** @return true if the block interaction should be cancelled (Simon Says block-wrong-clicks). */
    public boolean onBlockInteract(BlockPos pos) {
        if (lookCoordsEnabled && pos != null) {
            long now = System.currentTimeMillis();
            // useItemOn can fire for both hands on one click — dedupe same pos within 200ms.
            if (!pos.equals(lastLookCoordsPos) || now - lastLookCoordsTime > 200) {
                lastLookCoordsPos = pos.immutable();
                lastLookCoordsTime = now;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[HRZN] ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(net.minecraft.network.chat.Component.literal(
                            pos.getX() + "/" + pos.getY() + "/" + pos.getZ()).withStyle(ChatFormatting.WHITE)));
                }
            }
        }
        // Lever sound: any lever right-clicked (incl. shift-right-click) in a dungeon.
        Minecraft lm = Minecraft.getInstance();
        if (pos != null && lm != null && lm.level != null && dungeonStateService.isInDungeon()
            && lm.level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.LeverBlock) {
            de.horizon.feature.misc.CustomSoundPlayer.play(configManager.getConfig().getLeverSound());
        }
        // Secret sound: right-clicking a tracked secret (chest/lever/essence/…).
        if (pos != null && secretWaypointService.isSecretAt(pos, dungeonRoomDetector)) {
            de.horizon.feature.misc.CustomSoundPlayer.play(configManager.getConfig().getSecretSound());
        }
        // Waypoint edit mode: right-click a block creates a waypoint there (and blocks the vanilla use).
        if (waypointService.isEditMode() && waypointService.onBlockInteract(pos)) return true;
        // (left-click handled via onWaypointLeftClick from the game-mode mixin)
        boolean block = simonSaysService.onBlockInteract(pos, configManager.getConfig());
        block |= puzzleSolverService.onBlockInteract(pos, configManager.getConfig());
        return block;
    }

    /**
     * Called from the sound-packet mixin when the vanilla Etherwarp sound arrives. If the custom
     * Etherwarp sound is enabled, plays it (accurate timing) and returns true to cancel the vanilla one.
     */
    public boolean replaceEtherwarpSound() {
        var cfg = configManager.getConfig();
        if (!cfg.isEtherwarpSoundEnabled()) return false;
        de.horizon.feature.misc.CustomSoundPlayer.play(cfg.getEtherwarpSound());
        return true;
    }

    /** Left-click a waypoint block in edit mode → open its config screen. @return true if consumed. */
    public boolean onWaypointLeftClick(BlockPos pos) {
        if (!waypointService.isEditMode()) return false;
        var wp = waypointService.pick(pos);
        if (wp == null) return false;
        pendingScreen = new de.horizon.screen.WaypointScreen(null, waypointService, wp);
        return true;
    }

    public void onBlockUpdate(BlockPos pos, BlockState newState, BlockState oldState, Minecraft mc) {
        puzzleSolverService.onBlockChange(pos, mc);
        simonSaysService.onBlockUpdate(pos, newState);
        sharpShooterService.onBlockUpdate(pos, oldState, newState, configManager.getConfig());
        dragonService.onBlockUpdate(pos, newState);
        spiritBearService.onBlockUpdate(pos, newState, oldState);
    }

    public void onSimonSaysReset() {
        simonSaysService.onSectionReset(16);
    }

    public TerminalSolverService getTerminalSolverService() {
        return terminalSolverService;
    }

    /**
     * Draws the Experimentation Table helper overlay. Called from {@code HandledScreenMixin} at
     * {@code extractTooltip} HEAD so the re-shown Superpairs items land BEHIND the hover tooltip.
     */
    public void renderExperimentTableOverlay(AbstractContainerScreen<?> screen,
                                             net.minecraft.client.gui.GuiGraphicsExtractor context) {
        experimentTableSolver.render(screen, context, configManager.getConfig());
    }

    /** Croesus profit overlay — rendered before the tooltip so it never covers item tooltips. */
    public void renderCroesusOverlay(AbstractContainerScreen<?> screen,
                                     net.minecraft.client.gui.GuiGraphicsExtractor context) {
        chestProfitService.render(screen, context, configManager.getConfig());
        petHighlightService.render(screen, context, configManager.getConfig());
    }

    /**
     * Decorates an item tooltip (invoked from the Fabric {@code ItemTooltipCallback}). Applies,
     * in order: the maxed-enchant gradient (recolours existing lore, adds no lines) and one craft-value
     * price line. Shift switches craft components to Buy Order pricing; the separate Stack-Value toggle
     * additionally multiplies that line by the stack count while Shift is held.
     */
    public void decorateTooltip(
            net.minecraft.world.item.ItemStack stack, java.util.List<net.minecraft.network.chat.Component> lines) {
        var cfg = configManager.getConfig();
        if (stack == null || stack.isEmpty() || lines == null) return;

        if (cfg.isEnchantGradient()) {
            try {
                de.horizon.feature.skyblock.EnchantGradientRenderer.applyInPlace(stack, lines, cfg);
            } catch (Exception ignored) { }
        }

        boolean baz = cfg.isBazaarValueTooltip(), auc = cfg.isAuctionValueTooltip(), craft = cfg.isItemPriceTooltip();
        if (!(baz || auc || craft)) return;
        try {
            priceService.tick(); // ensure a fetch runs even before the first tick cycle
            String id = skyblockIdOf(stack);
            if (id != null) {
                id = id.replaceFirst("^STARRED_", "");
                // Market lines show for their own toggle OR the craft toggle (so one toggle gives all).
                if ((baz || craft) && priceService.isBazaarItem(id)) {
                    Long sell = priceService.getBazaarSell(id);
                    Long buy = priceService.getBazaarBuy(id);
                    if (buy != null) lines.add(priceLine("Bazaar Buy", buy));
                    if (sell != null) lines.add(priceLine("Bazaar Sell", sell));
                } else if ((auc || craft) && !priceService.isBazaarItem(id)) {
                    Long lb = priceService.getLowestBin(id);
                    Long avg = priceService.getAvgBin(id);
                    if (lb != null) lines.add(priceLine("Lowest BIN", lb));
                    if (avg != null) lines.add(priceLine("Avg BIN (3d)", avg));
                }
            }
            if (craft) {
                boolean shift = isShiftHeld();
                long value = craftValueService.craftValue(stack, shift);
                if (value > 0) {
                    if (shift && cfg.isStackValueOnShift() && stack.getCount() > 1) value *= stack.getCount();
                    lines.add(priceLine(shift ? "Craft Value (Buy Order)" : "Craft Value", value));
                }
            }
        } catch (Exception ignored) { }
    }

    private static net.minecraft.network.chat.Component priceLine(String label, long v) {
        return net.minecraft.network.chat.Component.literal("§7" + label + ": §6" + formatCoinsShort(v));
    }

    /** {@code /horizon price}: prints an itemised craft-value breakdown of the held item to chat. */
    private void printHeldItemPrice() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        java.util.function.Consumer<String> msg =
            s -> mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(s));
        net.minecraft.world.item.ItemStack stack = mc.player.getMainHandItem();
        if (stack == null || stack.isEmpty()) {
            msg.accept("§b[HRZN] §7Kein Item in der Hand.");
            return;
        }
        priceService.tick();
        var entries = craftValueService.breakdown(stack, false);
        msg.accept("§b§l[HRZN] §r§6Craft Breakdown: §f" + stack.getHoverName().getString());
        long total = 0L;
        for (var e : entries) {
            total += e.amount();
            msg.accept("§8• §7" + e.label() + ": §6" + coinsExact(e.amount()));
        }
        if (entries.isEmpty()) msg.accept("§7(Kein SkyBlock-Item oder Preise noch nicht geladen)");
        long buyOrderTotal = craftValueService.craftValue(stack, true);
        msg.accept("§8§m                              ");
        msg.accept("§7Total §f(Instabuy)§7: §e" + coinsExact(total));
        msg.accept("§7Total §f(Buy Order)§7: §e" + coinsExact(buyOrderTotal));
        String id = skyblockIdOf(stack);
        if (id != null) {
            id = id.replaceFirst("^STARRED_", "");
            if (priceService.isBazaarItem(id)) {
                Long buy = priceService.getBazaarBuy(id);
                if (buy != null) msg.accept("§7Markt §f(Bazaar Instabuy)§7: §6" + coinsExact(buy));
            } else {
                Long lb = priceService.getLowestBin(id);
                if (lb != null) msg.accept("§7Markt §f(Lowest BIN)§7: §6" + coinsExact(lb));
            }
        }
        if (!priceService.isLoaded()) msg.accept("§c(Preise laden noch — gleich erneut ausführen.)");
    }

    private static String coinsExact(long v) {
        return String.format(java.util.Locale.ROOT, "%,d", v);
    }

    private static boolean isShiftHeld() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().handle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
            || org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    private static String formatCoinsShort(long v) {
        if (v >= 1_000_000_000L) return String.format(java.util.Locale.ROOT, "%.2fb", v / 1_000_000_000.0);
        if (v >= 1_000_000L) return String.format(java.util.Locale.ROOT, "%.2fm", v / 1_000_000.0);
        if (v >= 1_000L) return String.format(java.util.Locale.ROOT, "%.1fk", v / 1_000.0);
        return String.format(java.util.Locale.ROOT, "%,d", v);
    }

    private static String skyblockIdOf(net.minecraft.world.item.ItemStack stack) {
        var cd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        var nbt = cd.copyTag();
        // In MC 26.1.2 the Hypixel SkyBlock id sits at the ROOT of custom data ({id:"ENDER_PEARL"});
        // older data nested it under ExtraAttributes. Check both.
        String id = nbt.getStringOr("id", "");
        if (id.isEmpty()) id = nbt.getCompoundOrEmpty("ExtraAttributes").getStringOr("id", "");
        return id.isEmpty() ? null : id;
    }

    /**
     * Replaces the displayed stack of a solved Experimentation Table slot (Superpairs reward /
     * Ultrasequencer number). Called from {@code HandledScreenMixin} for both item rendering and the
     * hover tooltip, so a remembered reward renders natively (no glass behind it) and tooltips work.
     */
    public net.minecraft.world.item.ItemStack modifyExperimentStack(
            AbstractContainerScreen<?> screen, net.minecraft.world.inventory.Slot slot,
            net.minecraft.world.item.ItemStack stack) {
        if (!configManager.getConfig().isExperimentSolverEnabled()) return stack;
        if (slot == null || slot.container instanceof net.minecraft.world.entity.player.Inventory) return stack;
        if (!experimentTableSolver.isActiveMenu(screen)) return stack;
        return experimentTableSolver.modifyDisplayStack(slot.index, stack);
    }

    /** Forwards an Experimentation Table slot click so the solver can advance its highlighted step. */
    public void onExperimentSlotClick(AbstractContainerScreen<?> screen, int slotId,
                                      net.minecraft.world.item.ItemStack stack, int button) {
        if (!configManager.getConfig().isExperimentSolverEnabled()) return;
        experimentTableSolver.onSlotClick(screen, slotId, stack, button);
    }

    /**
     * Whether the vanilla container should be fully hidden because the terminal overlay (or leap
     * menu) has replaced it. Shared by HandledScreenMixin (extractContents/extractRenderState) and
     * ContainerScreenMixin (extractBackground) so the two never disagree.
     */
    public boolean shouldHideVanillaContainer(AbstractContainerScreen<?> screen) {
        var cfg = configManager.getConfig();
        if (cfg.isTerminalSolverEnabled() && terminalSolverService.isActiveTerminal()) return true;
        if (cfg.isLeapMenuEnabled()) return de.horizon.feature.dungeon.LeapMenuOverlay.isLeapScreenTitle(screen);
        if (cfg.isStorageOverlayEnabled() && storageOverlayService.isStorageMenu(screen)
            && storageOverlayService.isOverviewOpen()) return true;
        return false;
    }

    /** True while the interactive storage-page overlay is active (relocates the real slots). */
    public boolean isStoragePageActive(AbstractContainerScreen<?> screen) {
        return configManager.getConfig().isStorageOverlayEnabled() && storageOverlayService.isStoragePage(screen);
    }

    /** Draws the storage overlay background + relocates the real slots (called before slots render). */
    public void renderStoragePageBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        storageOverlayService.relocateAndRenderBackground(screen, ctx, mouseX, mouseY);
    }


    /**
     * Called from the entity-interact mixin. Returns true when a right-click on an Arrow Align
     * frame should be cancelled (block-wrong-clicks), gated on the Goldor phase.
     */
    public boolean shouldBlockArrowInteract(net.minecraft.world.entity.Entity target) {
        if (target == null) return false;
        if (!dungeonStateService.isInDungeon()) return false;
        return arrowAlignService.shouldBlockInteract(target, configManager.getConfig());
    }

    /** True when an arrow-device frame's vanilla name label should be hidden (only our count shows). */
    public boolean shouldHideArrowFrameName(net.minecraft.world.entity.decoration.ItemFrame frame) {
        if (!configManager.getConfig().isArrowAlignEnabled() || !dungeonStateService.isInDungeon()) return false;
        return arrowAlignService.isDeviceFrame(frame);
    }

    /** True only for an orbiting Soulweaver cosmetic soul (never the static key/chest displays). */
    public boolean shouldHideSoulweaverSkull(net.minecraft.world.entity.Entity entity) {
        return configManager.getConfig().isSoulweaverSkullsHidden() && soulweaverService.isSoul(entity);
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

    private final java.util.Set<String> loggedRenderErrors = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Runs a world-render step in isolation; a failure is logged once and never
     *  cascades to the other render steps in the frame. */
    private void safeRender(String name, Runnable render) {
        try {
            render.run();
        } catch (RuntimeException | LinkageError e) {
            if (loggedRenderErrors.add(name)) {
                HorizonMod.LOGGER.error("Horizon render step '{}' failed (further errors suppressed)", name, e);
            }
        }
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
        // Hide Horizon HUD elements while the tab list is open so it stays readable.
        if (configManager.getConfig().isHideHudOnTab() && client.options.keyPlayerList.isDown()) {
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

            // Storage overlay: reset scroll/search when a fresh Storage menu/page opens
            if (configManager.getConfig().isStorageOverlayEnabled()) {
                if (storageOverlayService.isStorageMenu(handledScreen)) storageOverlayService.onStorageOpen();
                else if (storageOverlayService.isStoragePage(handledScreen)) storageOverlayService.onPageOpen();
            }
            // Reset the tooltip scale to the configured default whenever a screen opens.
            de.horizon.feature.misc.TooltipState.scale = configManager.getConfig().getTooltipScale();
            de.horizon.feature.misc.TooltipState.resetScroll();
            // Detect terminal screen opens
            if (configManager.getConfig().isTerminalSolverEnabled()
                || configManager.getConfig().isMelodyAnnounceEnabled()) {
                terminalSolverService.onScreenOpen(handledScreen);
            }
            // Experimentation Table (Superpairs) helper: reset memory on a fresh board
            if (configManager.getConfig().isExperimentSolverEnabled()) {
                experimentTableSolver.onScreenOpen(handledScreen);
            }
            // Terminal waypoints/titles: record position + show type title (independent of solver)
            if (configManager.getConfig().isTerminalWaypointsEnabled()
                || configManager.getConfig().isTerminalTitleEnabled()) {
                var termType = TerminalSolverService.detectType(handledScreen.getTitle().getString());
                terminalWaypointService.onTerminalOpen(termType, configManager.getConfig());
            }

            ScreenEvents.afterExtract(screen).register((currentScreen, context, mouseX, mouseY, delta) -> {
                if ("YOUTUBE_MUSIC".equals(configManager.getConfig().getActiveMusicService())) {
                    youtubeMusicInventoryOverlay.render(handledScreen, context, mouseX, mouseY);
                } else {
                    spotifyInventoryOverlay.render(handledScreen, context, mouseX, mouseY);
                }
                partyFinderOverlay.render(handledScreen, context);
                // Croesus profit renders in HandledScreenMixin (extractTooltip HEAD) so its highlights
                // + breakdown HUD sit BEHIND the item tooltips instead of covering them.
                if (configManager.getConfig().isStorageOverlayEnabled()) {
                    storageOverlayService.capture(handledScreen);
                    if (storageOverlayService.isStorageMenu(handledScreen)) {
                        storageOverlayService.render(handledScreen, context, mouseX, mouseY);
                    }
                    // Storage PAGE overlay renders in ContainerScreenMixin.extractBackground (before the
                    // real slots) so vanilla draws the relocated slots/cursor/tooltips natively.
                }
                // Leap Menu overlay
                if (leapMenuOverlay.isLeapScreen(handledScreen)) {
                    leapMenuOverlay.render(handledScreen, context, mouseX, mouseY, configManager.getConfig());
                }
                // Terminal solver rendering
                if (configManager.getConfig().isTerminalSolverEnabled()) {
                    terminalSolverService.onScreenTick(handledScreen);
                    terminalSolverService.render(handledScreen, context, configManager.getConfig());
                } else if (configManager.getConfig().isMelodyAnnounceEnabled()) {
                    terminalSolverService.onScreenTick(handledScreen); // compute melody state for the announce
                }
                // Melody progress announce (party chat), independent of the solver toggle.
                String melodyMsg = terminalSolverService.pollMelodyAnnounce(configManager.getConfig());
                if (melodyMsg != null && !melodyMsg.isBlank()) {
                    Minecraft mmc = Minecraft.getInstance();
                    if (mmc != null && mmc.player != null) mmc.player.connection.sendCommand("pc " + melodyMsg);
                }
                // Experimentation Table helper renders in HandledScreenMixin (extractTooltip HEAD)
                // so the re-shown Superpairs items stay behind the hover tooltip.
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
                // Storage overlay: consume clicks only when the overview is covering the menu.
                if (configManager.getConfig().isStorageOverlayEnabled()
                    && storageOverlayService.isStorageMenu(handledScreen)) {
                    if (storageOverlayService.onClick(handledScreen, click.x(), click.y())) return false;
                }
                // Storage page: real slots are relocated + handled natively by vanilla; we only
                // intercept clicks on the OTHER (cached) pages (navigate) or empty space (swallow).
                if (configManager.getConfig().isStorageOverlayEnabled()
                    && storageOverlayService.isStoragePage(handledScreen)) {
                    if (storageOverlayService.onPageClick(handledScreen, click.x(), click.y())) return false;
                }
                // Leap Menu: intercept clicks on the Spirit Leap screen
                if (leapMenuOverlay.isLeapScreen(handledScreen) && configManager.getConfig().isLeapMenuEnabled()) {
                    int slotIdx = leapMenuOverlay.getClickedSlot(handledScreen, (int) click.x(), (int) click.y(), configManager.getConfig());
                    if (slotIdx >= 0) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            mc.gameMode.handleContainerInput(
                                handledScreen.getMenu().containerId, slotIdx, 0, ContainerInput.PICKUP, mc.player);
                            // Announce who we're leaping to (configurable message).
                            if (configManager.getConfig().isLeapMenuAnnounce()) {
                                String target = leapMenuOverlay.getClickedPlayerName(handledScreen, (int) click.x(), (int) click.y());
                                String tmpl = configManager.getConfig().getLeapMenuMessage();
                                if (target != null && tmpl != null && !tmpl.isBlank()) {
                                    String msg = tmpl.replace("{playername}", target);
                                    mc.player.connection.sendCommand("pc " + msg);
                                }
                            }
                        }
                        return false; // cancel vanilla click
                    }
                }
                // Terminal solver: the overlay fully replaces the chest, so route every click on it
                // through the solver (valid clicks are forwarded to the server, all others swallowed).
                if (configManager.getConfig().isTerminalSolverEnabled()
                    && terminalSolverService.isActiveTerminal()) {
                    boolean isLeft = click.button() == 0;
                    if (terminalSolverService.onOverlayMouseClick(handledScreen, click.x(), click.y(), isLeft, configManager.getConfig())) {
                        terminalSolverService.onScreenTick(handledScreen);
                        de.horizon.feature.misc.CustomSoundPlayer.play(configManager.getConfig().getTerminalClickSound());
                        return false; // consume click (never let it reach the vanilla chest)
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
            // Storage overlay: scroll the combined view instead of the vanilla menu.
            ScreenMouseEvents.allowMouseScroll(screen).register((currentScreen, mx, my, horizontal, vertical) -> {
                if (configManager.getConfig().isStorageOverlayEnabled()) {
                    if (storageOverlayService.isStorageMenu(handledScreen)
                        && storageOverlayService.onScroll(handledScreen, vertical)) return false;
                    if (storageOverlayService.isStoragePage(handledScreen)
                        && storageOverlayService.onPageScroll(handledScreen, vertical)) return false;
                }
                // Scrollable tooltips: while a tooltip is showing, scroll pans it; Ctrl+scroll resizes.
                if (configManager.getConfig().isScrollableTooltips()
                    && de.horizon.feature.misc.TooltipState.isShowing()) {
                    Minecraft mc = Minecraft.getInstance();
                    boolean ctrl = mc != null && (org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().handle(),
                        org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                        || org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().handle(),
                        org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS);
                    if (ctrl) {
                        de.horizon.feature.misc.TooltipState.scale = Math.max(0.5f, Math.min(3.0f,
                            de.horizon.feature.misc.TooltipState.scale + (float) vertical * 0.1f));
                    } else {
                        de.horizon.feature.misc.TooltipState.scrollOffset = Math.max(0,
                            de.horizon.feature.misc.TooltipState.scrollOffset - (int) Math.round(vertical * 10));
                    }
                    return false;
                }
                return true;
            });
            // allowKeyPress fires before vanilla processing → can cancel hotbar-swap for wardrobe
            ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, input) -> {
                // Storage overlay: route typing into the search box.
                if (configManager.getConfig().isStorageOverlayEnabled()) {
                    if (storageOverlayService.isStorageMenu(handledScreen)
                        && storageOverlayService.onKey(input)) return false;
                    if (storageOverlayService.isStoragePage(handledScreen)
                        && storageOverlayService.onPageKey(input)) return false;
                }
                // Terminal solver: the drop key acts as a left click on the hovered overlay slot.
                if (configManager.getConfig().isTerminalSolverEnabled()
                    && terminalSolverService.isActiveTerminal()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.options.keyDrop.matches(input)) {
                        double mouseX = mc.mouseHandler.getScaledXPos(mc.getWindow());
                        double mouseY = mc.mouseHandler.getScaledYPos(mc.getWindow());
                        terminalSolverService.onOverlayMouseClick(handledScreen, mouseX, mouseY, true, configManager.getConfig());
                        terminalSolverService.onScreenTick(handledScreen);
                        return false; // block default drop behavior
                    }
                }
                if (wardrobeKeybindService.handleKeyPress(handledScreen, input.key(), configManager.getConfig())) {
                    return false; // cancel vanilla (prevents hotbar slot swap)
                }
                if (loadoutKeybindService.handleKeyPress(handledScreen, input.key(), configManager.getConfig())) {
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
