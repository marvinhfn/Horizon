package de.horizon.feature.chat;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.misc.PingService;
import de.horizon.feature.misc.TpsTracker;
import de.horizon.spotify.SpotifyService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles Hypixel chat commands triggered with a !-prefix in party, guild, or private chat.
 * <p>
 * Supported commands: !warp !inv !kick !promote !demote !transfer !coords !here
 *                     !fps !ping !tps !time !item !cf !8ball !dice
 */
public final class ChatCommandService {
    private static final Pattern PARTY_MSG  = Pattern.compile(
        "^Party >.+?(?:\\[.+?])?\\s*(\\w+)\\s*:\\s*!(.+)$");
    private static final Pattern GUILD_MSG  = Pattern.compile(
        "^Guild >.+?(?:\\[.+?])?\\s*(\\w+)\\s*:\\s*!(.+)$");
    private static final Pattern PRIVATE_MSG = Pattern.compile(
        "^From .+?(?:\\[.+?])?\\s*(\\w+)\\s*:\\s*!(.+)$");
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    private final PingService pingService;
    private final TpsTracker tpsTracker;
    private final SpotifyService spotifyService;

    public ChatCommandService(PingService pingService, TpsTracker tpsTracker, SpotifyService spotifyService) {
        this.pingService = pingService;
        this.tpsTracker = tpsTracker;
        this.spotifyService = spotifyService;
    }

    /**
     * Called for every received chat message. Returns a command string to execute
     * (without leading slash) if a command should be run, or {@code null} otherwise.
     */
    public String handleMessage(String rawMessage, HorizonConfig config) {
        if (!config.isChatCommandsEnabled()) return null;

        String plain = FORMATTING.matcher(rawMessage).replaceAll("").strip();
        String sender = null;
        String commandRaw = null;
        String channel = null;

        Matcher pm = PARTY_MSG.matcher(plain);
        if (pm.matches() && config.isChatCommandsParty()) {
            sender = pm.group(1);
            commandRaw = pm.group(2).trim();
            channel = "p";
        }

        if (sender == null) {
            Matcher gm = GUILD_MSG.matcher(plain);
            if (gm.matches() && config.isChatCommandsGuild()) {
                sender = gm.group(1);
                commandRaw = gm.group(2).trim();
                channel = "gc";
            }
        }

        if (sender == null) {
            Matcher msg = PRIVATE_MSG.matcher(plain);
            if (msg.matches() && config.isChatCommandsPrivate()) {
                sender = msg.group(1);
                commandRaw = msg.group(2).trim();
                channel = "msg " + sender;
            }
        }

        if (sender == null || commandRaw == null || channel == null) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return null;

        String[] parts = commandRaw.split("\\s+", 2);
        String cmdRaw = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1] : "";

        // Resolve aliases to canonical command names
        String cmd = switch (cmdRaw) {
            case "w"  -> "warp";
            case "k"  -> "kick";
            case "pt" -> "transfer";
            default   -> cmdRaw;
        };

        if (!config.isChatCommandEnabled(cmd)) return null;

        String selfName = mc.player.getName().getString();

        return switch (cmd) {
            // ── Party management ────────────────────────────────────────────
            case "warp"     -> "p warp";
            case "inv", "invite" -> {
                if (!isSelf(sender, selfName) && !arg.isBlank()) {
                    String t = fuzzyResolve(mc, arg); yield "p invite " + (t != null ? t : arg);
                } else if (!isSelf(sender, selfName)) yield "p invite " + sender;
                else yield null;
            }
            case "kick" -> {
                if (arg.isBlank()) yield null;
                String t = fuzzyResolve(mc, arg); yield "p kick " + (t != null ? t : arg);
            }
            case "promote" -> {
                if (arg.isBlank()) yield null;
                String t = fuzzyResolve(mc, arg); yield "p promote " + (t != null ? t : arg);
            }
            case "demote" -> {
                if (arg.isBlank()) yield null;
                String t = fuzzyResolve(mc, arg); yield "p demote " + (t != null ? t : arg);
            }
            case "transfer" -> {
                if (arg.isBlank()) yield null;
                String t = fuzzyResolve(mc, arg); yield "p transfer " + (t != null ? t : arg);
            }

            // ── Info sharing ────────────────────────────────────────────────
            case "coords" -> {
                var pos = mc.player.blockPosition();
                yield channel + " Coords: " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
            }
            case "here" -> {
                String dim = mc.level != null
                    ? mc.level.dimension().identifier().getPath()
                    : "unknown";
                yield channel + " I'm in: " + dim;
            }
            case "fps" -> {
                int fps = mc.getFps();
                yield channel + " FPS: " + fps;
            }
            case "ping" -> {
                int ping = pingService.getPing(mc);
                yield channel + " Ping: " + ping + "ms";
            }
            case "tps" -> {
                double tps = tpsTracker.getLastKnownTps();
                yield channel + " TPS: " + String.format(Locale.ROOT, "%.1f", tps);
            }
            case "time" -> {
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                yield channel + " Time: " + time;
            }
            case "item" -> {
                var held = mc.player.getMainHandItem();
                if (held.isEmpty()) yield channel + " I'm holding nothing.";
                yield channel + " I'm holding: " + held.getHoverName().getString();
            }

            // ── Fun ─────────────────────────────────────────────────────────
            case "cf" -> {
                boolean heads = Math.random() < 0.5;
                yield channel + " " + (heads ? "Heads!" : "Tails!");
            }
            case "dice" -> {
                int sides = arg.isBlank() ? 6 : parseIntOrDefault(arg, 6);
                int roll = (int) (Math.random() * sides) + 1;
                yield channel + " 🎲 " + roll + " (1-" + sides + ")";
            }
            case "8ball" -> {
                String[] answers = {
                    "It is certain.", "Without a doubt.", "Yes, definitely.", "Most likely.",
                    "Signs point to yes.", "Ask again later.", "Cannot predict now.",
                    "Don't count on it.", "My reply is no.", "Very doubtful."
                };
                yield channel + " 🎱 " + answers[(int) (Math.random() * answers.length)];
            }

            case "song" -> {
                if (spotifyService == null) yield null;
                var state = spotifyService.getPlaybackState();
                if (!state.connected() || !state.playing() || state.trackName().isBlank())
                    yield channel + " Nothing playing.";
                yield channel + " Now playing: " + state.trackName() + " by " + state.artistName();
            }

            default -> null;
        };
    }

    private static boolean isSelf(String sender, String selfName) {
        return sender.equalsIgnoreCase(selfName);
    }

    /**
     * Finds the first tablist player whose name starts with {@code prefix} (case-insensitive).
     * Returns the full name if found, or {@code null} if not.
     */
    private static String fuzzyResolve(Minecraft mc, String prefix) {
        if (mc.player == null || mc.player.connection == null) return null;
        String lower = prefix.toLowerCase(Locale.ROOT);
        Collection<PlayerInfo> players = mc.player.connection.getOnlinePlayers();
        for (PlayerInfo info : players) {
            String name = info.getProfile().name();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(lower)) return name;
        }
        return null;
    }

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
