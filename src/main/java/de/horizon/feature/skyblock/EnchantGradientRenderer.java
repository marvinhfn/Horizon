package de.horizon.feature.skyblock;

import de.horizon.config.HorizonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Recolors the lore text of <b>maxed</b> enchantments (per {@link EnchantData}) with an animated
 * diagonal gradient sweep. The gradient is positional across the whole enchant block — its colour at a
 * character is a function of that character's pixel x plus line y, offset by time — so it flows
 * diagonally over all maxed enchants rather than per-enchant. Non-maxed enchant text is untouched and
 * no tooltip lines are added or removed.
 */
public final class EnchantGradientRenderer {
    private EnchantGradientRenderer() {}

    private static final double SPAN = 140.0;   // pixels per gradient period
    private static final double SPEED = 0.35;   // periods per second (animation)
    private static final double LINE_H = 10.0;  // approx tooltip line height for the diagonal

    /** Recolours maxed-enchant runs in the given (mutable) tooltip line list, in place. */
    public static void applyInPlace(ItemStack stack, List<Component> lines, HorizonConfig config) {
        if (stack == null || lines == null || lines.isEmpty()) return;
        Set<String> tokens = maxedTokens(stack);
        if (tokens.isEmpty()) return;

        int[] stops = resolveStops(config);
        int mode = config.getEnchantGradientMode();
        double time = System.currentTimeMillis() / 1000.0;
        Font font = Minecraft.getInstance().font;

        for (int li = 1; li < lines.size(); li++) { // skip the item name (line 0)
            Flat flat = flatten(lines.get(li));
            boolean[] maxed = markMaxed(flat.text, tokens);
            if (maxed == null) continue; // no maxed token on this line
            lines.set(li, recolorLine(flat, maxed, font, li, stops, mode, time));
        }
    }

    /** Clean (no §) character stream + the resolved Style per character. */
    private record Flat(String text, List<Style> styles) {}

    /**
     * Flattens a lore line to visible characters with each character's effective Style, parsing inline
     * legacy §-codes into Style and dropping the § markers. This is essential: Hypixel enchant lore
     * carries colour/bold as inline §-codes (ultimate enchants are §d§l), and those codes override any
     * Style colour at render time — so to recolour we must strip them and express formatting as Style.
     */
    private static Flat flatten(Component line) {
        StringBuilder text = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        line.visit((base, content) -> {
            Style cur = base;
            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if (ch == '§' && i + 1 < content.length()) {
                    net.minecraft.ChatFormatting cf = net.minecraft.ChatFormatting.getByCode(Character.toLowerCase(content.charAt(++i)));
                    if (cf == net.minecraft.ChatFormatting.RESET) cur = base;
                    else if (cf != null && cf.isColor()) cur = base.withColor(TextColor.fromRgb(cf.getColor()));
                    else if (cf != null) cur = cur.applyFormat(cf);
                    continue;
                }
                text.append(ch);
                styles.add(cur);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return new Flat(text.toString(), styles);
    }

    /** Build the set of "<name> <roman>" lore tokens for every maxed enchant on the item. */
    private static Set<String> maxedTokens(ItemStack stack) {
        EnchantData.load();
        Set<String> tokens = new HashSet<>();
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return tokens;
        // 26.1.2 flattens ExtraAttributes into the custom_data root (see ItemCraftValueService).
        CompoundTag root = cd.copyTag();
        CompoundTag ea = root.getCompoundOrEmpty("ExtraAttributes");
        if (ea.isEmpty()) ea = root;
        CompoundTag ench = ea.getCompoundOrEmpty("enchantments");
        for (String id : ench.keySet()) {
            int lvl = ench.getIntOr(id, 0);
            if (lvl > 0 && EnchantData.isMaxed(id, lvl)) {
                String token = EnchantData.loreToken(id, lvl);
                if (token != null) tokens.add(token);
            }
        }
        return tokens;
    }

    /** Marks char indices covered by any maxed token, or null if the line has none. */
    private static boolean[] markMaxed(String plain, Set<String> tokens) {
        boolean[] maxed = null;
        for (String token : tokens) {
            int from = 0, idx;
            while ((idx = plain.indexOf(token, from)) >= 0) {
                if (maxed == null) maxed = new boolean[plain.length()];
                for (int i = idx; i < idx + token.length(); i++) maxed[i] = true;
                from = idx + token.length();
            }
        }
        return maxed;
    }

    /** Rebuilds a line from its flatten, replacing the colour of maxed-span characters with the gradient. */
    private static Component recolorLine(Flat flat, boolean[] maxed, Font font, int lineIndex,
                                         int[] stops, int mode, double time) {
        String plain = flat.text;
        List<Style> styles = flat.styles;
        MutableComponent result = Component.empty();
        double px = 0.0;
        double py = lineIndex * LINE_H;
        StringBuilder run = new StringBuilder();
        Style runStyle = null;
        for (int i = 0; i < plain.length(); i++) {
            char ch = plain.charAt(i);
            Style base = i < styles.size() ? styles.get(i) : Style.EMPTY;
            boolean isMax = i < maxed.length && maxed[i];
            Style style = base;
            if (isMax) {
                int color = gradientColor(px + py, time, stops, mode);
                style = base.withColor(TextColor.fromRgb(color));
            }
            // A maxed char's colour is unique, so its style differs and naturally starts a new run;
            // consecutive non-maxed chars sharing a style merge into one run.
            if (runStyle == null || !runStyle.equals(style)) {
                flush(result, run, runStyle);
                runStyle = style;
            }
            run.append(ch);
            px += charWidth(font, ch);
        }
        flush(result, run, runStyle);
        return result;
    }

    private static void flush(MutableComponent result, StringBuilder run, Style style) {
        if (run.length() == 0) return;
        result.append(Component.literal(run.toString()).setStyle(style == null ? Style.EMPTY : style));
        run.setLength(0);
    }

    private static double charWidth(Font font, char ch) {
        return font.width(String.valueOf(ch));
    }

    /** Gradient colour (0xRRGGBB) for a diagonal position, animated over time. */
    private static int gradientColor(double pos, double time, int[] stops, int mode) {
        double phase = pos / SPAN - time * SPEED;
        double f = phase - Math.floor(phase); // frac -> [0,1)
        if (mode == 2) { // rainbow: hue sweep
            return Color.HSBtoRGB((float) f, 1f, 1f) & 0xFFFFFF;
        }
        // 2-stop: triangle wave for a seamless back-and-forth blend.
        double tt = f * 2.0;
        if (tt > 1.0) tt = 2.0 - tt;
        return lerp(stops[0], stops[1], tt);
    }

    private static int lerp(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Two RGB stops for the current mode: HUD accent (+ hue-shifted twin), custom, or unused (rainbow). */
    private static int[] resolveStops(HorizonConfig config) {
        int mode = config.getEnchantGradientMode();
        if (mode == 1) {
            return new int[]{ config.getEnchantGradientColorA() & 0xFFFFFF, config.getEnchantGradientColorB() & 0xFFFFFF };
        }
        // HUD accent -> [accent, hue-shifted brighter twin]
        int accent = parseHex(config.getHudAccentColor());
        float[] hsb = new float[3];
        Color.RGBtoHSB((accent >> 16) & 0xFF, (accent >> 8) & 0xFF, accent & 0xFF, hsb);
        int twin = Color.HSBtoRGB((hsb[0] + 0.12f) % 1f, hsb[1], Math.min(1f, hsb[2] + 0.15f)) & 0xFFFFFF;
        return new int[]{ accent, twin };
    }

    private static int parseHex(String hex) {
        if (hex == null) return 0x75E7CA;
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try { return (int) (Long.parseLong(h, 16) & 0xFFFFFF); } catch (NumberFormatException e) { return 0x75E7CA; }
    }
}
