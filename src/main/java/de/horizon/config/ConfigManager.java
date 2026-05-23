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

    private final Path baseDir = FabricLoader.getInstance().getConfigDir().resolve("horizon");

    private HudConfig hud = new HudConfig();
    private DungeonConfig dungeon = new DungeonConfig();
    private SpotifyConfig spotify = new SpotifyConfig();
    private YoutubeConfig youtube = new YoutubeConfig();
    private ChatConfig chat = new ChatConfig();
    private MiscConfig misc = new MiscConfig();
    private AntiSpamConfig antiSpam = new AntiSpamConfig();
    private ParticleConfig particle = new ParticleConfig();
    private ScoreboardConfig scoreboard = new ScoreboardConfig();

    private HorizonConfig config = build();

    public HorizonConfig getConfig() {
        return config;
    }

    public void load() {
        hud = loadSub("hud.json", HudConfig.class, new HudConfig());
        dungeon = loadSub("dungeon.json", DungeonConfig.class, new DungeonConfig());
        spotify = loadSub("music/spotify.json", SpotifyConfig.class, new SpotifyConfig());
        youtube = loadSub("music/youtube.json", YoutubeConfig.class, new YoutubeConfig());
        chat = loadSub("chat.json", ChatConfig.class, new ChatConfig());
        misc = loadSub("misc.json", MiscConfig.class, new MiscConfig());
        antiSpam = loadSub("anti_spam.json", AntiSpamConfig.class, new AntiSpamConfig());
        particle = loadSub("particle.json", ParticleConfig.class, new ParticleConfig());
        scoreboard = loadSub("scoreboard.json", ScoreboardConfig.class, new ScoreboardConfig());
        scoreboard.ensureIslandDefaults();
        config = build();
        save();
    }

    public void save() {
        saveSub("hud.json", config.hud);
        saveSub("dungeon.json", config.dungeon);
        saveSub("music/spotify.json", config.spotify);
        saveSub("music/youtube.json", config.youtube);
        saveSub("chat.json", config.chat);
        saveSub("misc.json", config.misc);
        saveSub("anti_spam.json", config.antiSpam);
        saveSub("particle.json", config.particle);
        saveSub("scoreboard.json", config.scoreboard);
    }

    public HudPosition getOrCreatePosition(String id, int defaultX, int defaultY) {
        return config.getHudPositions().computeIfAbsent(id, ignored -> new HudPosition(defaultX, defaultY));
    }

    public void resetPosition(String id, int defaultX, int defaultY) {
        config.getHudPositions().put(id, new HudPosition(defaultX, defaultY, 1.0D));
        save();
    }

    private HorizonConfig build() {
        return new HorizonConfig(hud, dungeon, spotify, youtube, chat, misc, antiSpam, particle, scoreboard);
    }

    private <T> T loadSub(String filename, Class<T> clazz, T defaultValue) {
        Path path = baseDir.resolve(filename);
        if (!Files.exists(path)) {
            return defaultValue;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            T loaded = GSON.fromJson(reader, clazz);
            return loaded != null ? loaded : defaultValue;
        } catch (IOException | RuntimeException exception) {
            HorizonMod.LOGGER.error("Failed to load config/{}, using defaults", filename, exception);
            return defaultValue;
        }
    }

    private void saveSub(String filename, Object obj) {
        Path path = baseDir.resolve(filename);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException exception) {
            HorizonMod.LOGGER.error("Failed to save config/{}", filename, exception);
        }
    }
}
