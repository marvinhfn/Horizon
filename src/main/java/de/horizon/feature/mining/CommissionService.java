package de.horizon.feature.mining;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the player's active commissions from the tab list (Dwarven Mines / Crystal Hollows / Glacite).
 *
 * <p>The tab list shows a "Commissions:" header followed by lines like "Titanium Miner: 45%" or
 * "Goblin Raid: DONE". We only scan when such a header is present, so the overlay stays empty
 * everywhere else. The vanilla player-list order isn't guaranteed, so we collect every commission-
 * shaped line rather than relying on positional ordering.
 */
public final class CommissionService {
    private static final Pattern STRIP = Pattern.compile("(?i)§[0-9a-fk-or]");
    // "<name>: 45%" / "<name>: 45.6%" / "<name>: DONE"
    private static final Pattern LINE = Pattern.compile("^(.{2,40}?):\\s*(DONE|\\d{1,3}(?:\\.\\d+)?%)$");

    public record Commission(String name, String value, boolean done) {}

    private final List<Commission> commissions = new ArrayList<>();
    private int tickCount = 0;

    public void tick(Minecraft mc) {
        if (++tickCount % 10 != 0) return; // twice per second is plenty
        if (mc == null || mc.player == null) return;
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) return;

        List<String> lines = new ArrayList<>();
        boolean sawHeader = false;
        for (PlayerInfo info : conn.getListedOnlinePlayers()) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;
            String text = STRIP.matcher(display.getString()).replaceAll("").strip();
            if (text.isEmpty()) continue;
            if (text.toLowerCase(Locale.ROOT).startsWith("commission")) { sawHeader = true; continue; }
            lines.add(text);
        }

        commissions.clear();
        if (!sawHeader) return;
        for (String text : lines) {
            Matcher m = LINE.matcher(text);
            if (!m.matches()) continue;
            String value = m.group(2);
            commissions.add(new Commission(m.group(1).strip(), value, value.equalsIgnoreCase("DONE")));
        }
    }

    public List<Commission> getCommissions() {
        return commissions;
    }

    public boolean hasCommissions() {
        return !commissions.isEmpty();
    }
}
