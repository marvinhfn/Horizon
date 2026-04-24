package de.horizon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HorizonMod implements ModInitializer {
    public static final String MOD_ID = "horizon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = FabricLoader.getInstance()
        .getModContainer(MOD_ID)
        .map(container -> container.getMetadata().getVersion().getFriendlyString())
        .orElse("dev");

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Horizon core");
    }
}
