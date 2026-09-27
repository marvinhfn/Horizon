package de.horizon.screen.render;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public final class Fonts {
    public static final Identifier INTER_ID = Identifier.fromNamespaceAndPath("horizon", "inter");
    public static final FontDescription INTER = new FontDescription.Resource(INTER_ID);
    // Smaller (vanilla-sized) Inter variant for in-world HUDs, which render at the
    // user's normal GUI scale (no hi-DPI magnification like the menus get).
    public static final Identifier INTER_HUD_ID = Identifier.fromNamespaceAndPath("horizon", "inter_hud");
    public static final FontDescription INTER_HUD = new FontDescription.Resource(INTER_HUD_ID);
    private Fonts() {}
    public static MutableComponent of(String s) {
        return Component.literal(s).withStyle(st -> st.withFont(INTER));
    }
    public static MutableComponent hud(String s) {
        return Component.literal(s).withStyle(st -> st.withFont(INTER_HUD));
    }
}
