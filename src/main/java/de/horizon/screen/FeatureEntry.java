package de.horizon.screen;

import java.util.Locale;

public record FeatureEntry(String titleDe, String titleEn, String location,
                           HorizonConfigScreen.Tab tab, HorizonConfigScreen.DungeonSection section,
                           String keywordsDe, String keywordsEn) {
    public String title(boolean german) { return german ? titleDe : titleEn; }
    public boolean matches(String query, boolean german) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return false;
        String hay = (title(german) + " " + location + " " + keywordsDe + " " + keywordsEn).toLowerCase(Locale.ROOT);
        for (String term : q.split("\\s+")) if (!hay.contains(term)) return false;
        return true;
    }
}
