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
 * Party management: !warp/!w !inv/!invite/!kidnap !kick/!k !promote !demote !transfer
 *                   !pt/!ptme !ai/!allinvite !f/!f&lt;n&gt; !m/!m&lt;n&gt;
 * Info sharing:     !coords/!cords !here !fps !ping !tps !time !item !song
 * Fun:              !cf !dice !8ball !gay
 * <p>
 * Every command that writes to chat prefixes its message with {@code [HRZN]}.
 */
public final class ChatCommandService {
    private static final Pattern PARTY_MSG  = Pattern.compile(
        "^Party >.+?(?:\\[.+?])?\\s*(\\w+)\\s*:\\s*!(.+)$");
    private static final Pattern GUILD_MSG  = Pattern.compile(
        "^Guild >.+?(?:\\[.+?])?\\s*(\\w+)\\s*:\\s*!(.+)$");
    private static final Pattern PRIVATE_MSG = Pattern.compile(
        "^From .+?(?:\\[.+?])?\\s*(\\w+)\\s*:\\s*!(.+)$");
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");
    private static final Pattern FLOOR_INLINE = Pattern.compile("^([fm])(\\d)$"); // !f7 / !m3

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
            channel = "pc"; // party CHAT (/pc); /p <text> is party INVITE, not chat
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

        // Inline floor form (!f7 / !m3) → canonical cmd + floor arg.
        String cmd;
        Matcher fm = FLOOR_INLINE.matcher(cmdRaw);
        if (fm.matches()) {
            cmd = fm.group(1);
            arg = fm.group(2);
        } else {
            cmd = switch (cmdRaw) {
                case "w"         -> "warp";
                case "k"         -> "kick";
                case "cords"     -> "coords";
                case "ptme"      -> "pt";
                case "allinvite" -> "ai";
                case "invite", "kidnap" -> "inv";
                default          -> cmdRaw;
            };
        }

        if (!config.isChatCommandEnabled(cmd)) return null;

        String selfName = mc.player.getName().getString();

        return switch (cmd) {
            // ── Party management ────────────────────────────────────────────
            case "warp"     -> "p warp";
            case "inv" -> {
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
            case "pt"  -> "p transfer " + sender;   // hand leadership to whoever asked
            case "ai"  -> "p settings allinvite";   // toggle all-invite
            case "f"   -> joinInstance(arg, false);  // !f<n>: catacombs floor n (0 = entrance)
            case "m"   -> joinInstance(arg, true);   // !m<n>: master catacombs floor n

            // ── Info sharing (chat output → [HRZN] prefix) ──────────────────
            case "coords" -> {
                var pos = mc.player.blockPosition();
                yield chat(channel, "Coords: " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            }
            case "here" -> {
                String dim = mc.level != null ? mc.level.dimension().identifier().getPath() : "unknown";
                yield chat(channel, "I'm in: " + dim);
            }
            case "fps"  -> chat(channel, "FPS: " + mc.getFps());
            case "ping" -> chat(channel, "Ping: " + pingService.getPing(mc) + "ms");
            case "tps"  -> chat(channel, "TPS: " + String.format(Locale.ROOT, "%.1f", tpsTracker.getLastKnownTps()));
            case "time" -> chat(channel, "Time: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            case "item" -> {
                var held = mc.player.getMainHandItem();
                yield chat(channel, held.isEmpty() ? "I'm holding nothing." : "I'm holding: " + held.getHoverName().getString());
            }

            // ── Fun ─────────────────────────────────────────────────────────
            case "cf" -> chat(channel, Math.random() < 0.5 ? "Heads!" : "Tails!");
            case "dice" -> {
                int sides = arg.isBlank() ? 6 : parseIntOrDefault(arg, 6);
                yield chat(channel, "🎲 " + ((int) (Math.random() * sides) + 1) + " (1-" + sides + ")");
            }
            case "8ball" -> {
                String[] answers = {
                    "It is certain.", "Without a doubt.", "Yes, definitely.", "Most likely.",
                    "Signs point to yes.", "Ask again later.", "Cannot predict now.",
                    "Don't count on it.", "My reply is no.", "Very doubtful."
                };
                yield chat(channel, "🎱 " + answers[(int) (Math.random() * answers.length)]);
            }
            case "gay" -> {
                String target = arg.isBlank() ? sender : arg;
                String t = fuzzyResolve(mc, target); if (t != null) target = t;
                yield chat(channel, target + " is " + (int) (Math.random() * 101) + "% gay");
            }

            // ── Spotify (fetched async, dispatched when the API responds) ────
            case "song" -> {
                if (spotifyService == null) yield null;
                final String ch = channel;
                spotifyService.fetchNowPlayingAsync(state -> {
                    String out;
                    if (!state.connected()) out = "Spotify not connected.";
                    else if (!state.playing() || state.trackName().isBlank()) out = "Nothing playing.";
                    else out = "Now playing: " + state.trackName() + " by " + state.artistName();
                    sendCommand(chat(ch, out));
                });
                yield null; // dispatched asynchronously above
            }

            default -> null;
        };
    }

    /** Prefixes a chat message with {@code [HRZN]} in the given channel (e.g. {@code p}/{@code gc}). */
    private static String chat(String channel, String text) {
        return channel + " [HRZN] " + text;
    }

    /** {@code /joininstance} target for catacombs (or master catacombs) floor {@code arg}. */
    private static String joinInstance(String arg, boolean master) {
        int n = parseIntOrDefault(arg.trim(), -1);
        if (!master && n == 0) return "joininstance CATACOMBS_ENTRANCE";
        String word = floorWord(n);
        if (word == null) return null;
        return "joininstance " + (master ? "MASTER_CATACOMBS_FLOOR_" : "CATACOMBS_FLOOR_") + word;
    }

    private static String floorWord(int n) {
        return switch (n) {
            case 1 -> "ONE"; case 2 -> "TWO"; case 3 -> "THREE"; case 4 -> "FOUR";
            case 5 -> "FIVE"; case 6 -> "SIX"; case 7 -> "SEVEN"; default -> null;
        };
    }

    /** Sends a Hypixel command on the client thread (used by async command results like !song). */
    private static void sendCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.player != null && mc.player.connection != null) mc.player.connection.sendCommand(command);
        });
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
