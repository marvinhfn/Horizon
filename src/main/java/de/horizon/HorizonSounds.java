package de.horizon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;

public final class HorizonSounds {
    public static final SoundEvent FISHING_ALERT_CUSTOM =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("horizon", "fishing_alert_custom"));
    public static final SoundEvent FISHING_ALERT_MR =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("horizon", "fishing_alert_mr"));

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                Identifier.fromNamespaceAndPath("horizon", "fishing_alert_custom"),
                FISHING_ALERT_CUSTOM);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                Identifier.fromNamespaceAndPath("horizon", "fishing_alert_mr"),
                FISHING_ALERT_MR);
    }

    private HorizonSounds() {}
}
