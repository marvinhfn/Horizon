package de.horizon.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HudRegistry {
    private final List<HudElement> elements = new ArrayList<>();

    public void register(HudElement element) {
        elements.add(element);
    }

    public List<HudElement> getElements() {
        return Collections.unmodifiableList(elements);
    }
}
