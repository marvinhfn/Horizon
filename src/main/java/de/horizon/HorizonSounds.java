package de.horizon;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class HorizonSounds {
    public static final SoundEvent FISHING_ALERT_CUSTOM =
            SoundEvent.of(Identifier.of("horizon", "fishing_alert_custom"));
    public static final SoundEvent FISHING_ALERT_MR =
            SoundEvent.of(Identifier.of("horizon", "fishing_alert_mr"));

    public static void register() {
        Registry.register(Registries.SOUND_EVENT,
                Identifier.of("horizon", "fishing_alert_custom"),
                FISHING_ALERT_CUSTOM);
        Registry.register(Registries.SOUND_EVENT,
                Identifier.of("horizon", "fishing_alert_mr"),
                FISHING_ALERT_MR);
    }

    private HorizonSounds() {}
}
