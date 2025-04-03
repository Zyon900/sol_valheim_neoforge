// Adjust the package to your new project structure
package com.zyon900.sol_valheim_neo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ValheimFoodData
{
    private static final int DEFAULT_MAX_SLOTS = 3;

    // --- EntityDataSerializer ---
    public static final EntityDataSerializer<ValheimFoodData> FOOD_DATA_SERIALIZER = new EntityDataSerializer<>(){
        @Override
        public @NotNull ValheimFoodData copy(ValheimFoodData value) {
            // Your existing deep copy logic
            var ret = new ValheimFoodData();
            ret.MaxItemSlots = value.MaxItemSlots;
            ret.ItemEntries = value.ItemEntries.stream().map(EatenFoodItem::new).collect(Collectors.toCollection(ArrayList::new));
            if (value.DrinkSlot != null)
                ret.DrinkSlot = new EatenFoodItem(value.DrinkSlot);
            ret.loadConfigValues();
            return ret;
        }

        // This provides a StreamCodec, which is the modern way to handle network sync for EntityData
        @Override
        public @NotNull StreamCodec<? super FriendlyByteBuf, ValheimFoodData> codec() {
            return ByteBufCodecs.TRUSTED_COMPOUND_TAG.map(
                    ValheimFoodData::read,      // CompoundTag -> ValheimFoodData
                    (foodData) -> foodData.save(new CompoundTag()) // ValheimFoodData -> CompoundTag
            );
        }
    };

    // --- Class Fields ---
    public List<EatenFoodItem> ItemEntries = new ArrayList<>();
    public EatenFoodItem DrinkSlot;
    public int MaxItemSlots = DEFAULT_MAX_SLOTS;

    // Constructor
    public ValheimFoodData() {
        loadConfigValues();
    }

    public void loadConfigValues() {
        // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
        // Ensure Config.COMMON and Config.COMMON.maxSlots are accessible static fields/objects
        if (Config.COMMON != null && Config.COMMON.maxSlots != null) {
            this.MaxItemSlots = Config.COMMON.maxSlots.get();
        } else {
            SoLValheimNeoforged.LOGGER.warn("Config not fully initialized when creating/loading ValheimFoodData. Using default max slots ({}).", DEFAULT_MAX_SLOTS);
            this.MaxItemSlots = DEFAULT_MAX_SLOTS;
        }
    }

    // --- Methods ---

    public void eatItem(Item food)
    {
        if (food == Items.ROTTEN_FLESH)
            return;

        // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
        // Ensure Config.getFoodConfig is an accessible static method
        Config.Common.FoodConfig config = Config.getFoodConfig(food);
        if (config == null)
            return;

        boolean isDrink = food.getDefaultInstance().getUseAnimation() == UseAnim.DRINK;
        if (isDrink) {
            if (DrinkSlot != null && !DrinkSlot.canEatEarly())
                return;

            if (DrinkSlot == null)
                DrinkSlot = new EatenFoodItem(food, config.getTime());
            else {
                DrinkSlot.ticksLeft = config.getTime();
                DrinkSlot.item = food;
            }
            return;
        }

        var existing = getEatenFood(food);
        if (existing != null)
        {
            if (!existing.canEatEarly())
                return;

            existing.ticksLeft = config.getTime();
            return;
        }

        if (ItemEntries.size() < this.MaxItemSlots)
        {
            ItemEntries.add(new EatenFoodItem(food, config.getTime()));
            return;
        }

        for (var item : ItemEntries)
        {
            if (item.canEatEarly())
            {
                item.ticksLeft = config.getTime();
                item.item = food;
                return;
            }
        }
    }

    public boolean canEat(Item food)
    {
        if (food == Items.ROTTEN_FLESH)
            return true;

        // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
        Config.Common.FoodConfig config = Config.getFoodConfig(food);
        if (config == null)
            return false;

        boolean isDrink = food.getDefaultInstance().getUseAnimation() == UseAnim.DRINK;
        if (isDrink)
            return DrinkSlot == null || DrinkSlot.canEatEarly();

        var existing = getEatenFood(food);
        if (existing != null)
            return existing.canEatEarly();

        if (ItemEntries.size() < this.MaxItemSlots)
            return true;

        return ItemEntries.stream().anyMatch(EatenFoodItem::canEatEarly);
    }

    public EatenFoodItem getEatenFood(Item food) {
        // No config access here
        return ItemEntries.stream()
                .filter((item) -> item.item == food)
                .findFirst()
                .orElse(null);
    }

    public void clear() {
        // No config access here
        ItemEntries.clear();
        DrinkSlot = null;
    }

    public void tick() {
        // No config access here
        var iterator = ItemEntries.iterator();
        while (iterator.hasNext()) {
            var item = iterator.next();
            item.ticksLeft--;
            if (item.ticksLeft <= 0) {
                iterator.remove();
            }
        }

        if (DrinkSlot != null) {
            DrinkSlot.ticksLeft--;
            if (DrinkSlot.ticksLeft <= 0)
                DrinkSlot = null;
        }

        ItemEntries.sort(Comparator.comparingInt(a -> a.ticksLeft));
    }

    public float getTotalFoodNutrition()
    {
        float nutrition = 0f;
        for (var item : ItemEntries)
        {
            // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
            Config.Common.FoodConfig food = Config.getFoodConfig(item.item);
            if (food != null)
            {
                nutrition += food.getHearts();
            }
        }

        float drinkBonus = 0f;
        if (DrinkSlot != null)
        {
            // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
            Config.Common.FoodConfig food = Config.getFoodConfig(DrinkSlot.item);
            if (food != null)
            {
                nutrition += food.getHearts();
            }

            // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
            if (Config.COMMON != null && Config.COMMON.drinkSlotFoodEffectivenessBonus != null) {
                drinkBonus = Config.COMMON.drinkSlotFoodEffectivenessBonus.get().floatValue();
            } else {
                SoLValheimNeoforged.LOGGER.warn("Drink bonus config not ready during nutrition calculation.");
            }
            nutrition = nutrition * (1.0f + drinkBonus);
        }

        return nutrition;
    }


    public float getRegenSpeed()
    {
        float regen = 0.25f;
        for (var item : ItemEntries)
        {
            // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
            Config.Common.FoodConfig food = Config.getFoodConfig(item.item);
            if (food != null)
            {
                regen += food.getHealthRegen();
            }
        }

        float drinkBonus = 0f;
        if (DrinkSlot != null)
        {
            // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
            Config.Common.FoodConfig food = Config.getFoodConfig(DrinkSlot.item);
            if (food != null)
            {
                regen += food.getHealthRegen();
            }

            // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
            if (Config.COMMON != null && Config.COMMON.drinkSlotFoodEffectivenessBonus != null) {
                drinkBonus = Config.COMMON.drinkSlotFoodEffectivenessBonus.get().floatValue();
            } else {
                SoLValheimNeoforged.LOGGER.warn("Drink bonus config not ready during regen calculation.");
            }
            regen = regen * (1.0f + drinkBonus);
        }

        return regen;
    }


    // --- NBT Serialization ---

    public CompoundTag save(CompoundTag tag) {
        // No direct config access here, uses instance field this.MaxItemSlots
        tag.putInt("max_slots", this.MaxItemSlots);
        tag.putInt("count", ItemEntries.size());
        int savedCount = 0;
        for (EatenFoodItem foodItem : ItemEntries) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(foodItem.item);
            if (!itemId.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
                tag.putString("id" + savedCount, itemId.toString());
                tag.putInt("ticks" + savedCount, foodItem.ticksLeft);
                savedCount++;
            } else {
                SoLValheimNeoforged.LOGGER.warn("Attempted to save unregistered or AIR item in food slot: {}", foodItem.item);
            }
        }
        tag.putInt("count", savedCount);

        if (DrinkSlot != null)
        {
            ResourceLocation drinkId = BuiltInRegistries.ITEM.getKey(DrinkSlot.item);
            if (!drinkId.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
                tag.putString("drink", drinkId.toString());
                tag.putInt("drinkticks", DrinkSlot.ticksLeft);
            } else {
                SoLValheimNeoforged.LOGGER.warn("Attempted to save unregistered or AIR item in drink slot: {}", DrinkSlot.item);
            }
        } else {
            tag.remove("drink");
            tag.remove("drinkticks");
        }

        return tag;
    }

    public static ValheimFoodData read(CompoundTag tag) {
        // No direct config access here during read, uses constructor/loadConfigValues
        var instance = new ValheimFoodData();

        if (tag.contains("max_slots", CompoundTag.TAG_INT)) {
            instance.MaxItemSlots = tag.getInt("max_slots");
        }

        int size = tag.getInt("count");
        for (int i = 0; i < size; i++)
        {
            String idStr = tag.getString("id" + i);
            int ticks = tag.getInt("ticks" + i);

            if (!idStr.isEmpty()) {
                ResourceLocation itemId = ResourceLocation.tryParse(idStr);
                if (itemId != null) {
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item != Items.AIR) {
                        instance.ItemEntries.add(new EatenFoodItem(item, ticks));
                    } else {
                        SoLValheimNeoforged.LOGGER.warn("Failed to load item with ID '{}' from save data (Item not found).", idStr);
                    }
                } else {
                    SoLValheimNeoforged.LOGGER.warn("Failed to parse item ID '{}' from save data.", idStr);
                }
            }
        }

        if (tag.contains("drink", CompoundTag.TAG_STRING)) {
            String drinkStr = tag.getString("drink");
            int drinkTicks = tag.getInt("drinkticks");

            if (!drinkStr.isEmpty()) {
                ResourceLocation drinkId = ResourceLocation.tryParse(drinkStr);
                if (drinkId != null) {
                    Item drinkItem = BuiltInRegistries.ITEM.get(drinkId);
                    if (drinkItem != Items.AIR) {
                        instance.DrinkSlot = new EatenFoodItem(drinkItem, drinkTicks);
                    } else {
                        SoLValheimNeoforged.LOGGER.warn("Failed to load drink item with ID '{}' from save data (Item not found).", drinkStr);
                    }
                } else {
                    SoLValheimNeoforged.LOGGER.warn("Failed to parse drink item ID '{}' from save data.", drinkStr);
                }
            }
        }

        instance.ItemEntries.sort(Comparator.comparingInt(a -> a.ticksLeft));
        return instance;
    }


    // --- Inner Class: EatenFoodItem ---
    public static class EatenFoodItem {
        public Item item;
        public int ticksLeft;

        public boolean canEatEarly() {
            if (ticksLeft < 1200)
                return true;

            // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
            Config.Common.FoodConfig config = Config.getFoodConfig(item);
            if (config == null || config.getTime() <= 0)
                return false;

            float eatAgainPercentage = 0.25f;
            // *** CHANGE HERE: Use 'Config' instead of 'SoLValheimNeoforgedConfig' ***
            eatAgainPercentage = Config.COMMON.eatAgainPercentage.get().floatValue();
            return ((float) this.ticksLeft / config.getTime()) < eatAgainPercentage;
        }

        public EatenFoodItem(Item item, int ticksLeft) {
            this.item = item;
            this.ticksLeft = ticksLeft;
        }

        public EatenFoodItem(EatenFoodItem eaten) {
            this.item = eaten.item;
            this.ticksLeft = eaten.ticksLeft;
        }
    }
}