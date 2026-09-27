package de.horizon.screen.render;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public final class Fonts {
    public static final Identifier INTER_ID = Identifier.fromNamespaceAndPath("horizon", "inter");
    public static final FontDescription INTER = new FontDescription.Resource(INTER_ID);
    private Fonts() {}
    public static MutableComponent of(String s) {
        return Component.literal(s).withStyle(st -> st.withFont(INTER));
    }
}
