package de.horizon.hud;

import de.horizon.HorizonClient;
import de.horizon.config.HorizonConfig;

public final class HudStyle {
    private static final int DEFAULT_ACCENT_RGB = 0x75E7CA;
    private static final int TEXT = 0xFFEAF3FF;
    private static final int MUTED = 0xFF9FB0C7;
    private static final int WARNING = 0xFFFFD27C;
    private static final int BACKDROP = 0xD0080F18;
    private static final int PANEL = 0xE0121822;
    private static final int PANEL_ALT = 0xE0182230;
    private static final int ACTION = 0xFF131B25;
    private static final int ACTION_HOVER = 0xFF1A2532;

    private HudStyle() {
    }

    public static int accent() {
        HorizonClient client = HorizonClient.getInstance();
        return client == null ? argb(DEFAULT_ACCENT_RGB) : accent(client.getConfigManager().getConfig());
    }

    public static int accent(HorizonConfig config) {
        return argb(parseAccentRgb(config == null ? null : config.getHudAccentColor()));
    }

    public static int text() {
        return TEXT;
    }

    public static int muted() {
        return MUTED;
    }

    public static int warning() {
        return WARNING;
    }

    public static int backdrop() {
        return BACKDROP;
    }

    public static int panel() {
        return PANEL;
    }

    public static int panelAlt() {
        return PANEL_ALT;
    }

    public static int action() {
        return ACTION;
    }

    public static int actionHover() {
        return ACTION_HOVER;
    }

    public static int border() {
        return mix(accent(), 0xFF2E3B4D, 0.42F);
    }

    public static int selected() {
        return mix(accent(), 0xFFFFFFFF, 0.25F);
    }

    public static int badgeFill() {
        return withAlpha(accent(), 0x38);
    }

    public static int readyText() {
        return accent();
    }

    public static String sanitizeHex(String value) {
        if (value == null) {
            return "#75E7CA";
        }
        String trimmed = value.trim().toUpperCase();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        StringBuilder builder = new StringBuilder(6);
        for (int index = 0; index < trimmed.length() && builder.length() < 6; index++) {
            char current = trimmed.charAt(index);
            if ((current >= '0' && current <= '9') || (current >= 'A' && current <= 'F')) {
                builder.append(current);
            }
        }
        if (builder.isEmpty()) {
            return "#";
        }
        return "#" + builder;
    }

    public static boolean isCompleteHex(String value) {
        return value != null && value.matches("#[0-9A-Fa-f]{6}");
    }

    private static int parseAccentRgb(String value) {
        if (value == null) {
            return DEFAULT_ACCENT_RGB;
        }
        String sanitized = sanitizeHex(value);
        if (!isCompleteHex(sanitized)) {
            return DEFAULT_ACCENT_RGB;
        }
        try {
            return Integer.parseInt(sanitized.substring(1), 16);
        } catch (NumberFormatException ignored) {
            return DEFAULT_ACCENT_RGB;
        }
    }

    private static int argb(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static int mix(int colorA, int colorB, float ratioB) {
        float ratioA = 1.0F - ratioB;
        int a = Math.round(((colorA >>> 24) & 0xFF) * ratioA + ((colorB >>> 24) & 0xFF) * ratioB);
        int r = Math.round(((colorA >>> 16) & 0xFF) * ratioA + ((colorB >>> 16) & 0xFF) * ratioB);
        int g = Math.round(((colorA >>> 8) & 0xFF) * ratioA + ((colorB >>> 8) & 0xFF) * ratioB);
        int b = Math.round((colorA & 0xFF) * ratioA + (colorB & 0xFF) * ratioB);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
