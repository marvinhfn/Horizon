package de.horizon;

public final class Lang {
    public enum Language { DE, EN }

    private static Language current = Language.DE;

    private Lang() {}

    public static void set(Language lang) {
        current = lang != null ? lang : Language.DE;
    }

    public static Language get() {
        return current;
    }

    public static String t(String de, String en) {
        return current == Language.EN ? en : de;
    }
}
