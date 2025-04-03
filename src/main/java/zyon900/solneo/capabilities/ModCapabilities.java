package zyon900.solneo.capabilities;

import zyon900.solneo.data.ValheimFoodData;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.jetbrains.annotations.Nullable;

public class ModCapabilities {

    // --- Store the EntityCapability handle for our data type on Entities ---
    // The context is likely Void for player data not dependent on direction.
    // Initialize as null. It will be populated during the RegisterCapabilitiesEvent.
    @Nullable
    public static EntityCapability<ValheimFoodData, @Nullable Void> VALHEIM_FOOD = null;

    public static boolean isFoodCapabilityRegistered() {
        return VALHEIM_FOOD != null;
    }
}
