package de.horizon.config;

public final class DisplayConfig {
    boolean pillarboxEnabled = false;
    AnimationConfig animation = new AnimationConfig();
    boolean fireOverlayDisabled = false;
    float hurtCamIntensity = 1.0f;
    boolean frontCamDisabled = false;      // F5 skips the front-facing third-person view
    boolean soulweaverSkullsHidden = false; // hide the Soulweaver Gloves orbiting skulls
}
