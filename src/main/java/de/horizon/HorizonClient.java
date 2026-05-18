package de.horizon;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.horizon.api.HorizonApiClient;
import de.horizon.api.auth.HorizonApiAuthService;
import de.horizon.api.profile.HorizonProfileGateway;
import de.horizon.config.ConfigManager;
import de.horizon.feature.chat.ChatTabManager;
import de.horizon.feature.chat.SpamHider;
import de.horizon.feature.dungeon.DungeonAlertService;
import de.horizon.feature.dungeon.DungeonStateService;
import de.horizon.feature.dungeon.DungeonSolverOverlay;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import de.horizon.feature.misc.PingService;
import de.horizon.feature.misc.SystemStatsService;
import de.horizon.feature.misc.TpsTracker;
import de.horizon.feature.particle.ParticleFilterService;
import de.horizon.feature.revive.ReviveTracker;
import de.horizon.hypixel.HypixelProfileService;
import de.horizon.hypixel.PartyFinderOverlay;
import de.horizon.hypixel.HypixelSidebarOverlay;
import de.horizon.hud.HudElement;
import de.horizon.hud.HudRegistry;
import de.horizon.hud.PerformanceHudElement;
import de.horizon.hud.RevivalStatusHudElement;
import de.horizon.hud.SystemStatsHudElement;
import de.horizon.hud.TimeHudElement;
import de.horizon.screen.HorizonConfigScreen;
import de.horizon.screen.PlayerProfileScreen;
import de.horizon.spotify.SpotifyInventoryOverlay;
import de.horizon.spotify.SpotifyService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HorizonClient implements ClientModInitializer {
    private static HorizonClient instance;
    private static final KeyBinding.Category HORIZON_CATEGORY = KeyBinding.Category.create(Identifier.of("horizon", "controls"));

    private final ConfigManager configManager = new ConfigManager();
    private final SpamHider spamHider = new SpamHider();
    private final ChatTabManager chatTabManager = new ChatTabManager();
    private final ReviveTracker reviveTracker = new ReviveTracker();
    private final DungeonAlertService dungeonAlertService = new DungeonAlertService();
    private final DungeonStateService dungeonStateService = new DungeonStateService();
    private final DungeonRoomDetector dungeonRoomDetector = new DungeonRoomDetector();
    private final DungeonSolverOverlay dungeonSolverOverlay = new DungeonSolverOverlay();
    private final HudRegistry hudRegistry = new HudRegistry();
    private final ParticleFilterService particleFilterService = new ParticleFilterService(configManager);
    private final TpsTracker tpsTracker = new TpsTracker();
    private final PingService pingService = new PingService();
    private final SystemStatsService systemStatsService = new SystemStatsService();
    private final SpotifyService spotifyService = new SpotifyService(configManager);
    private final SpotifyInventoryOverlay spotifyInventoryOverlay = new SpotifyInventoryOverlay(spotifyService);
    private final HypixelProfileService hypixelProfileService = new HypixelProfileService(configManager);
    private final HorizonApiAuthService horizonApiAuthService = new HorizonApiAuthService(configManager);
    private final HorizonApiClient horizonApiClient = new HorizonApiClient(configManager, horizonApiAuthService);
    private final HorizonProfileGateway horizonProfileGateway = new HorizonProfileGateway(horizonApiClient);
    private final PartyFinderOverlay partyFinderOverlay = new PartyFinderOverlay(hypixelProfileService);
    private final HypixelSidebarOverlay hypixelSidebarOverlay = new HypixelSidebarOverlay();
    private KeyBinding openConfigKeyBinding;
    private Screen pendingScreen;

    public static HorizonClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        HorizonMod.LOGGER.info("Initializing Horizon client");

        configManager.load();
        hudRegistry.register(new RevivalStatusHudElement(configManager, reviveTracker, dungeonStateService));
        hudRegistry.register(new TimeHudElement());
        hudRegistry.register(new PerformanceHudElement());
        hudRegistry.register(new SystemStatsHudElement());
        openConfigKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.horizon.open_config",
            InputUtil.Type.KEYSYM,
            InputUtil.GLFW_KEY_H,
            HORIZON_CATEGORY
        ));

        HudRenderCallback.EVENT.register(this::renderHud);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> dungeonSolverOverlay.renderWorld(context));
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String raw = message.getString();
            dungeonStateService.handleChatMessage(raw);
            dungeonRoomDetector.handleChatMessage(raw);
            dungeonSolverOverlay.handleChatMessage(raw);
            reviveTracker.handleChatMessage(raw, configManager.getConfig());
            return !spamHider.shouldHide(raw, configManager.getConfig())
                && !chatTabManager.shouldHide(raw, configManager.getConfig());
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String raw = message.getString();
            dungeonStateService.handleChatMessage(raw);
            dungeonRoomDetector.handleChatMessage(raw);
            dungeonSolverOverlay.handleChatMessage(raw);
            reviveTracker.handleChatMessage(raw, configManager.getConfig());
            return !spamHider.shouldHide(raw, configManager.getConfig())
                && !chatTabManager.shouldHide(raw, configManager.getConfig());
        });
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> !executeLocalCommand(command, MinecraftClient.getInstance() == null ? null : MinecraftClient.getInstance().currentScreen));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("horizon")
                .executes(context -> {
                    openConfigScreen(null);
                    return 1;
                }))
        );
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("hv")
                .executes(context -> openProfileScreen(""))
                .then(ClientCommandManager.argument("player", StringArgumentType.greedyString())
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
        return false;
    }

    private void onClientTick(MinecraftClient client) {
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
        dungeonSolverOverlay.tick(client, configManager.getConfig(), dungeonStateService, dungeonRoomDetector);
        pingService.tick(client);
        reviveTracker.tick();
        while (openConfigKeyBinding != null && openConfigKeyBinding.wasPressed()) {
            HorizonMod.LOGGER.info("Opening Horizon config through keybind");
            openConfigScreen(client.currentScreen);
        }
    }

    public void openConfigScreen(net.minecraft.client.gui.screen.Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
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

    public DungeonRoomDetector getDungeonRoomDetector() {
        return dungeonRoomDetector;
    }

    public ChatTabManager getChatTabManager() {
        return chatTabManager;
    }

    public DungeonSolverOverlay getDungeonSolverOverlay() {
        return dungeonSolverOverlay;
    }

    public SystemStatsService getSystemStatsService() {
        return systemStatsService;
    }

    private int openProfileScreen(String player) {
        return openProfileScreen(player, MinecraftClient.getInstance() == null ? null : MinecraftClient.getInstance().currentScreen);
    }

    private int openProfileScreen(String player, Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return 0;
        }

        String target = player == null || player.isBlank() ? client.player.getName().getString() : player.trim();
        pendingScreen = new PlayerProfileScreen(normalizeCommandParent(parent), target, horizonProfileGateway);
        return 1;
    }

    private Screen normalizeCommandParent(Screen parent) {
        return parent instanceof ChatScreen ? null : parent;
    }

    private void renderHud(DrawContext drawContext, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options.hudHidden || client.player == null) {
            return;
        }

        for (HudElement element : hudRegistry.getElements()) {
            if (!element.isEnabled(configManager.getConfig())) {
                continue;
            }
            element.render(drawContext, client, configManager.getOrCreatePosition(element.id(), element.defaultX(), element.defaultY()), false);
        }
        dungeonSolverOverlay.renderHudOverlay(drawContext, client, configManager.getConfig());
        hypixelSidebarOverlay.render(drawContext, client);
    }

    private void registerScreenHooks() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof HandledScreen<?> handledScreen)) {
                return;
            }

            ScreenEvents.afterRender(screen).register((currentScreen, context, mouseX, mouseY, delta) ->
            {
                spotifyInventoryOverlay.render(handledScreen, context, mouseX, mouseY);
                partyFinderOverlay.render(handledScreen, context);
                dungeonSolverOverlay.render(handledScreen, context, configManager.getConfig(), dungeonStateService, dungeonRoomDetector);
            }
            );
            ScreenMouseEvents.afterMouseClick(screen).register((currentScreen, click, doubled) ->
                spotifyInventoryOverlay.mouseClicked(click.x(), click.y(), click.button())
            );
            ScreenMouseEvents.afterMouseDrag(screen).register((currentScreen, click, deltaX, deltaY, cancelled) ->
                spotifyInventoryOverlay.mouseDragged(click.x(), click.y(), click.button())
            );
            ScreenMouseEvents.afterMouseRelease(screen).register((currentScreen, click, cancelled) ->
                spotifyInventoryOverlay.mouseReleased(click.x(), click.y(), click.button())
            );
            net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen).register((currentScreen, input) ->
                spotifyInventoryOverlay.keyPressed(input.key())
            );
        });
    }
}
