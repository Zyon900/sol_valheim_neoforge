package zyon900.solneo.event;

import net.neoforged.fml.common.EventBusSubscriber;
import zyon900.solneo.solneo;
import zyon900.solneo.capabilities.ModCapabilities;
// No provider import needed here if relying on Attacher
import zyon900.solneo.data.ValheimFoodData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable; // For Void context

@EventBusSubscriber(modid = solneo.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CapabilityRegistry {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        solneo.LOGGER.debug("Registering capabilities...");

        // --- STEP 1: Create the EntityCapability Handle ---
        // Use the static factory method you found
        ModCapabilities.VALHEIM_FOOD = EntityCapability.create(
                ResourceLocation.fromNamespaceAndPath(solneo.MOD_ID, "valheim_food_data"), // Unique Name
                ValheimFoodData.class,              // Data Type
                Void.class                          // Context Type (@Nullable Void might also work/be needed)
        );

        // Check if handle creation succeeded
        if (!ModCapabilities.isFoodCapabilityRegistered()) {
            solneo.LOGGER.error("Failed to create VALHEIM_FOOD EntityCapability handle!");
            return; // Stop registration if handle creation failed
        }
        solneo.LOGGER.debug("Created VALHEIM_FOOD EntityCapability handle.");

        // --- STEP 2: Register the Handle against the Entity Type ---
        // This links the Capability Type (handle) to the Entity Type (Player)
        // The third argument (ICapabilityProvider) seems optional or handled differently now.
        // We rely on CapabilityAttacher to provide the actual instance per player.
        // Pass null or a lambda returning null, as the main provider instance comes later.
        event.registerEntity(
                ModCapabilities.VALHEIM_FOOD,   // The handle we just created
                EntityType.PLAYER,              // Target entity type
                // Provider argument: Pass null, relying on AttachCapabilitiesEvent
                (player, context) -> null
                // OR: (player, context) -> null
        );
        solneo.LOGGER.debug("Registered VALHEIM_FOOD handle for Player entities.");
    }
}