package de.horizon.feature.chat;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ChatTabManager {
    /** Pixels the chat is lifted upward when the screen is focused, to make room for tab buttons. */
    public static final int TAB_BAR_LIFT = 14;

    private static final int MAX_HISTORY = 300;
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)\\u00a7[0-9a-fk-or]");

    private ChatTab activeTab = ChatTab.ALL;
    private boolean repopulating = false;
    private final List<StoredMessage> history = new ArrayList<>();

    public record StoredMessage(Component text, ChatTab category) {}

    public ChatTab getActiveTab() {
        return activeTab;
    }

    public boolean isRepopulating() {
        return repopulating;
    }

    public void setActiveTabAndRepopulate(ChatTab tab, HorizonConfig config) {
        this.activeTab = tab == null ? ChatTab.ALL : tab;
        repopulate(config);
    }

    public void repopulateAfterBridgeToggle(HorizonConfig config) {
        repopulate(config);
    }

    public void repopulateAfterGuildToggle(HorizonConfig config) {
        repopulate(config);
    }

    public void repopulateAfterSpamFilterChange(HorizonConfig config) {
        repopulate(config);
    }

    /**
     * Called from ChatComponentMixin when a new message arrives.
     * Classifies and stores the message, then returns whether it should be shown.
     */
    public boolean onMessageAdded(Component message, HorizonConfig config) {
        String raw = plain(message.getString());
        ChatTab category = classify(raw);

        history.add(new StoredMessage(message, category));
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }

        return shouldShow(raw, category, config);
    }

    private void repopulate(HorizonConfig config) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui == null) {
            return;
        }
        ChatComponent chatHud = mc.gui.getChat();
        repopulating = true;
        chatHud.clearMessages(false);
        for (StoredMessage msg : history) {
            if (shouldShow(plain(msg.text().getString()), msg.category(), config)) {
                chatHud.addClientSystemMessage(msg.text());
            }
        }
        repopulating = false;
    }

    private boolean shouldShow(String plainMsg, ChatTab category, HorizonConfig config) {
        if (config.isChatBridgeHidden() && isBridgeMessage(plainMsg, config.getChatBridgeBotName())) {
            return false;
        }
        if (config.isGuildChatHidden() && category == ChatTab.GUILD) {
            return false;
        }
        if (config.isHideGuildJoinLeaveMessages() && isGuildJoinLeaveMessage(plainMsg)) {
            return false;
        }
        if (config.isHideRadioSignalMessages() && isRadioSignalMessage(plainMsg)) {
            return false;
        }
        if (config.isHideSacksMessages() && plainMsg.toLowerCase(Locale.ROOT).startsWith("[sacks]")) {
            return false;
        }
        if (activeTab == ChatTab.ALL) {
            return true;
        }
        // Specific tabs (Party, Guild, DM) show only their own category.
        // Everything else — public chat, server messages, system messages — is All-only.
        return category == activeTab;
    }

    private boolean isGuildJoinLeaveMessage(String msg) {
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.startsWith("guild >") && (lower.contains(" joined") || lower.contains(" leaved") || lower.contains(" left") || lower.contains(" kicked"))) {
            return true;
        }
        return lower.contains("joined the guild")
            || lower.contains("left the guild")
            || lower.contains("was kicked from the guild");
    }

    private boolean isRadioSignalMessage(String msg) {
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("radio signal")
            || lower.contains("your radio is weak")
            || lower.contains("find another enjoyer to boost");
    }

    public ChatTab classify(String raw) {
        String msg = plain(raw);
        if (isPartyMessage(msg)) return ChatTab.PARTY;
        if (isGuildMessage(msg)) return ChatTab.GUILD;
        if (isDmMessage(msg)) return ChatTab.DM;
        return ChatTab.ALL;
    }

    private boolean isPartyMessage(String msg) {
        return msg.startsWith("Party >");
    }

    private boolean isGuildMessage(String msg) {
        return msg.startsWith("Guild >");
    }

    private boolean isDmMessage(String msg) {
        return msg.startsWith("From ") || msg.startsWith("To ");
    }

    private boolean isBridgeMessage(String msg, String botName) {
        if (!msg.startsWith("Guild >") || botName == null || botName.isBlank()) {
            return false;
        }
        String afterGuild = msg.substring("Guild >".length()).strip();
        // Strip optional leading rank like [VIP] or [MVP+]
        if (afterGuild.startsWith("[")) {
            int end = afterGuild.indexOf(']');
            if (end >= 0) {
                afterGuild = afterGuild.substring(end + 1).strip();
            }
        }
        String bot = botName.trim();
        if (!afterGuild.startsWith(bot)) {
            return false;
        }
        // Strip the bot name and any optional guild-rank suffix like [O], [CO], [GM]
        String afterBot = afterGuild.substring(bot.length()).strip();
        if (afterBot.startsWith("[")) {
            int end = afterBot.indexOf(']');
            if (end >= 0) {
                afterBot = afterBot.substring(end + 1).strip();
            }
        }
        // Expect ": <content>" after the bot identifier
        if (!afterBot.startsWith(":")) {
            return false;
        }
        String content = afterBot.substring(1).strip();
        // A bridge message relays a Discord user: "DiscordUser: actual message"
        // The Discord username must not contain spaces; bot responses like
        // "BersSungs Networth: ..." have a space before the colon and are not bridge messages.
        int colonPos = content.indexOf(':');
        if (colonPos <= 0) {
            return false;
        }
        String discordUser = content.substring(0, colonPos);
        return !discordUser.contains(" ");
    }

    private String plain(String raw) {
        return FORMATTING_CODES.matcher(raw == null ? "" : raw).replaceAll("").strip();
    }
}
