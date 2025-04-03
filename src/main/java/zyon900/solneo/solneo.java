package zyon900.solneo;

import zyon900.solneo.config.Config; // Import Config class
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(solneo.MOD_ID)
public class solneo {

    public static final String MOD_ID = "solneo"; // Updated Mod ID
    public static final Logger LOGGER = LoggerFactory.getLogger(solneo.class);

    public solneo(IEventBus modEventBus) {
        LOGGER.info("SoL: Valheim NeoForge initializing...");

        // Register setup methods
        modEventBus.addListener(this::onConstructMod);
        modEventBus.addListener(this::commonSetup);

        // Register DeferredRegisters if you add custom Items, Blocks, Effects etc.
        // Example: ModItems.ITEMS.register(modEventBus);

        // Capability events and other MOD bus events are typically registered
        // via @Mod.EventBusSubscriber on the handler classes themselves.

        // ClientSetup events are also handled by @Mod.EventBusSubscriber(value=Dist.CLIENT)
    }

    // Register configuration files during mod construction
    private void onConstructMod(final FMLConstructModEvent event) {
        LOGGER.debug("Registering Mod Configs");
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC, MOD_ID + "-common.toml");
        context.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC, MOD_ID + "-client.toml");
    }

    // Runs after registries are loaded but before world loading (single-threaded)
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.debug("Common Setup starting...");
        // Any setup logic that needs to run after registration but before full game load.
        // Example: Registering network packets, initializing complex data structures.

        // Parse the food config list here ONCE after config is loaded.
        Config.loadAndParseFoodConfigs(); // Call the parsing method
        LOGGER.info("Common setup finished.");
    }
}