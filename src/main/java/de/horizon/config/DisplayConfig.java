package de.horizon.config;

public final class DisplayConfig {
    boolean pillarboxEnabled = false;
    AnimationConfig animation = new AnimationConfig();
    boolean fireOverlayDisabled = false;
    float hurtCamIntensity = 1.0f;
    boolean frontCamDisabled = false;      // F5 skips the front-facing third-person view
    boolean soulweaverSkullsHidden = false; // hide the Soulweaver Gloves orbiting skulls
    boolean experimentSolverEnabled = true; // Experimentation Table (Superpairs) helper
    boolean croesusProfitEnabled = true;    // Dungeon/Croesus chest profit calculator
    boolean storageOverlayEnabled = true;   // Combined Ender Chest/Backpack storage overlay
    boolean hideHudOnTab = true;            // hide Horizon HUD elements while the tab list is open
    boolean hideStatusEffects = false;      // hide vanilla potion-effect icons (HUD + inventory)
    boolean bazaarValueTooltip = false;     // append Bazaar buy/sell to item tooltips
    boolean auctionValueTooltip = false;    // append Lowest/Avg BIN to item tooltips
    boolean itemPriceTooltip = false;       // extra craft-value line; instabuy, shift -> buy-order
    boolean stackValueOnShift = false;      // shift multiplies the shown price by the stack count
    boolean enchantGradient = false;        // animated gradient over maxed-enchant lore text
    int enchantGradientMode = 0;            // 0 = HUD accent, 1 = custom stops, 2 = rainbow
    int enchantGradientColorA = 0xFF75E7CA; // custom gradient stop A
    int enchantGradientColorB = 0xFF3AA0FF; // custom gradient stop B
    boolean scrollableTooltips = false;     // scroll long tooltips; ctrl-scroll to resize
    float tooltipScale = 1.0f;              // default tooltip scale
    boolean petHighlight = true;            // highlight the summoned pet in the Pets menu
}
