package de.horizon.feature.dungeon;

import de.horizon.HorizonMod;
import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class MimicService {
    private static final Pattern FORMATTING_CODES = Pattern.compile("(?i)§[0-9a-fk-or]");

    private static final Set<String> MIMIC_CHAT = Set.of(
        "mimic dead!", "mimic dead", "mimic killed!", "mimic killed",
        "child destroyed!", "mimic obliterated!", "mimic exorcised!",
        "mimic destroyed!", "mimic annhilated!", "breefing killed",
        "breefing dead"
    );

    private static final Set<String> PRINCE_CHAT = Set.of(
        "prince dead", "prince dead!", "prince killed", "prince killed!",
        "prince slain", "a prince falls. +1 bonus score"
    );

    private boolean mimicKilled;
    private boolean mimicAnnounced;
    private boolean princeKilled;
    private boolean princeAnnounced;

    public void onBabyZombieDeath(DungeonStateService state, HorizonConfig config) {
        if (mimicKilled) return;
        if (!config.isMimicDetectionEnabled()) return;
        // Mimics only exist on floors 6+ and never inside the boss room. Gating here
        // (mirrors the reference score mods) prevents a stray baby zombie on a lower
        // floor from ever flagging a mimic kill and inflating the bonus score.
        if (state.getCurrentFloor() <= 5) return;
        if (state.isInBoss()) return;

        HorizonMod.LOGGER.info("[MimicService] Baby zombie death detected, marking mimic as killed");
        mimicKilled = true;
        if (!mimicAnnounced && config.isMimicMessageEnabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.connection.sendCommand("pc Mimic killed!");
                mimicAnnounced = true;
            }
        }
    }

    public void handleChatMessage(String raw, HorizonConfig config) {
        String plain = FORMATTING_CODES.matcher(raw).replaceAll("");
        String lower = plain.toLowerCase(Locale.ROOT).strip();

        if (!mimicKilled) {
            for (String msg : MIMIC_CHAT) {
                if (lower.contains(msg)) {
                    mimicKilled = true;
                    mimicAnnounced = true;
                    break;
                }
            }
        }

        if (!princeKilled) {
            for (String msg : PRINCE_CHAT) {
                if (lower.contains(msg)) {
                    princeKilled = true;
                    if (!princeAnnounced && config.isPrinceMessageEnabled()) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null && mc.player != null) {
                            mc.player.connection.sendCommand("pc Prince killed!");
                            princeAnnounced = true;
                        }
                    }
                    break;
                }
            }
        }
    }

    public boolean isMimicKilled() {
        return mimicKilled;
    }

    public boolean isPrinceKilled() {
        return princeKilled;
    }

    public void reset() {
        mimicKilled = false;
        mimicAnnounced = false;
        princeKilled = false;
        princeAnnounced = false;
    }
}
