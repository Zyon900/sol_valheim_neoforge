package zyon900.solneo.capabilities;

import zyon900.solneo.data.ValheimFoodData;
import zyon900.solneo.solneo; // For logger
import net.minecraft.core.HolderLookup; // Import Provider
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable; // The interface itself
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability; // Import for return type annotation

import java.util.stream.Collectors; // Ensure Collectors is imported

// Implement ICapabilityProvider<Player, Void, ValheimFoodData>
// Implement INBTSerializable<CompoundTag>
public class ValheimFoodProvider implements ICapabilityProvider<Player, Void, ValheimFoodData>, INBTSerializable<CompoundTag> {

    private final ValheimFoodData foodData = new ValheimFoodData();

    @Override
    @Nullable
    public ValheimFoodData getCapability(@NotNull Player player, @Nullable Void context) {
        // This implementation is correct based on the generic interface definition
        return this.foodData;
    }

    // --- Correct serializeNBT Signature ---
    @Override
    @UnknownNullability // Match annotation from interface if needed/desired
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        // Call the save method on the data object.
        // Currently, ValheimFoodData.save doesn't need the provider, but we pass an empty tag.
        return foodData.save(new CompoundTag());
        // If ValheimFoodData.save is updated later to need the provider:
        // return foodData.save(provider, new CompoundTag());
    }

    // --- Correct deserializeNBT Signature ---
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        // Call the static read method on the data class.
        // Currently, ValheimFoodData.read doesn't need the provider.
        ValheimFoodData loadedData = ValheimFoodData.read(nbt);
        // If ValheimFoodData.read is updated later to need the provider:
        // ValheimFoodData loadedData = ValheimFoodData.read(provider, nbt);

        // --- Perform DEEP COPY when updating the existing instance ---
        // List copy (assumes EatenFoodItem copy constructor is sufficient)
        this.foodData.ItemEntries = loadedData.ItemEntries.stream()
                .map(ValheimFoodData.EatenFoodItem::new)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        // Drink slot copy
        this.foodData.DrinkSlot = (loadedData.DrinkSlot == null) ? null : new ValheimFoodData.EatenFoodItem(loadedData.DrinkSlot);
        // Simple field copy
        this.foodData.MaxItemSlots = loadedData.MaxItemSlots;
        // Re-apply config
        this.foodData.loadConfigValues();
    }
}