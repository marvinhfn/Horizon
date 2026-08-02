package de.horizon.feature.misc;

import de.horizon.config.DungeonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Plays a configurable {@link DungeonConfig.CustomSound} (sound id + volume + pitch) at the player. */
public final class CustomSoundPlayer {
    private CustomSoundPlayer() {}

    public static void play(DungeonConfig.CustomSound s) {
        if (s == null || !s.enabled || s.sound == null || s.sound.isBlank()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        Identifier id = Identifier.tryParse(s.sound.trim());
        if (id == null) return;
        SoundEvent ev = BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (ev == null) return;
        mc.player.playSound(ev, Math.max(0f, s.volume), Math.max(0.5f, Math.min(2f, s.pitch)));
    }
}
