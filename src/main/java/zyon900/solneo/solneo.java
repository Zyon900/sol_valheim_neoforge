package zyon900.solneo;

import zyon900.solneo.config.Config;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(solneo.MOD_ID)
public class solneo {

    public static final String MOD_ID = "solneo";
    public static final Logger LOGGER = LoggerFactory.getLogger(solneo.class);

    public solneo(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("SoL: Valheim NeoForge initializing...");

        // Register setup methods (using the modEventBus parameter)
        modEventBus.addListener(this::commonSetup);
        // Client setup listener remains handled by @Mod.EventBusSubscriber

        // --- CONFIG REGISTRATION (Using ModContainer) ---
        LOGGER.debug("Registering Mod Configs via ModContainer");
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC, MOD_ID + "-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC, MOD_ID + "-client.toml");
    }

    // Common setup method remains the same
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.debug("Common Setup starting...");
        Config.loadAndParseFoodConfigs();
        LOGGER.info("Common setup finished.");
    }
}