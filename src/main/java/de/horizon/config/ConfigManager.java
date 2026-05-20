package de.horizon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.horizon.HorizonMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path = FabricLoader.getInstance().getConfigDir().resolve("horizon.json");
    private HorizonConfig config = new HorizonConfig();

    public HorizonConfig getConfig() {
        return config;
    }

    public void load() {
        if (!Files.exists(path)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            HorizonConfig loaded = GSON.fromJson(reader, HorizonConfig.class);
            config = loaded != null ? loaded : new HorizonConfig();
        } catch (IOException | RuntimeException exception) {
            HorizonMod.LOGGER.error("Failed to load config from {}, resetting to defaults", path, exception);
            config = new HorizonConfig();
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            HorizonMod.LOGGER.error("Failed to save config to {}", path, exception);
        }
    }

    public HudPosition getOrCreatePosition(String id, int defaultX, int defaultY) {
        return config.getHudPositions().computeIfAbsent(id, ignored -> new HudPosition(defaultX, defaultY));
    }

    public void resetPosition(String id, int defaultX, int defaultY) {
        config.getHudPositions().put(id, new HudPosition(defaultX, defaultY, 1.0D));
        save();
    }
}
