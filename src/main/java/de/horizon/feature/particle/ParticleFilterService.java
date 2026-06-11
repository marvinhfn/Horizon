package de.horizon.feature.particle;

import de.horizon.config.ConfigManager;
import de.horizon.config.HorizonConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ParticleFilterService {
    private final ConfigManager configManager;

    public ParticleFilterService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean isAllowed(ParticleOptions effect) {
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(effect.getType());
        if (id == null) {
            return true;
        }
        return configManager.getConfig().getParticleStates().getOrDefault(id.toString(), true);
    }

    public List<String> particleIds() {
        List<String> ids = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            ids.add(id.toString());
        }
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    public String displayName(String particleId) {
        String path = particleId.contains(":") ? particleId.substring(particleId.indexOf(':') + 1) : particleId;
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            if (part.isEmpty()) {
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    public boolean isEnabled(String particleId) {
        return configManager.getConfig().getParticleStates().getOrDefault(particleId, true);
    }

    public void toggle(String particleId) {
        HorizonConfig config = configManager.getConfig();
        config.getParticleStates().put(particleId, !isEnabled(particleId));
        configManager.save();
    }
}
