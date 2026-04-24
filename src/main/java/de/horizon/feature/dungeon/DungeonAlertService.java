package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.dungeon.room.DetectedDungeonRoom;
import de.horizon.feature.dungeon.room.DungeonRoomDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DungeonAlertService {
    private static final Set<String> RARE_ROOM_NAMES = Set.of("trinity", "tomioka", "duncan");

    private final Set<String> alertedRooms = new HashSet<>();

    public void tick(MinecraftClient client, HorizonConfig config, DungeonStateService dungeonState, DungeonRoomDetector roomDetector) {
        if (client == null || client.player == null || client.world == null || config == null || dungeonState == null || roomDetector == null) {
            alertedRooms.clear();
            return;
        }
        if (!dungeonState.isInDungeon()) {
            alertedRooms.clear();
            return;
        }
        if (!config.isDungeonRareRoomAlertsEnabled()) {
            return;
        }

        DetectedDungeonRoom room = roomDetector.currentRoom().orElse(null);
        if (room == null || room.confidence() < 80 || !isRareAlertRoom(room.name())) {
            return;
        }

        String roomKey = normalized(room.name()) + "@" + room.origin().getX() + ":" + room.origin().getZ();
        if (!alertedRooms.add(roomKey)) {
            return;
        }

        Text alert = Text.literal("Rare Room entdeckt: " + room.name()).formatted(Formatting.LIGHT_PURPLE);
        client.inGameHud.setOverlayMessage(alert, false);
        client.player.sendMessage(alert, false);
        client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.85F, 1.15F);
    }

    private boolean isRareAlertRoom(String roomName) {
        return RARE_ROOM_NAMES.contains(normalized(roomName));
    }

    private String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }
}
