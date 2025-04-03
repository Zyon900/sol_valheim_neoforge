package zyon900.solneo.data;

import zyon900.solneo.config.Config; // Import new Config class
import zyon900.solneo.solneo; // Import main mod class
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ValheimFoodData {

    private static final int DEFAULT_MAX_SLOTS = 3;

    // --- EntityDataSerializer ---
    public static final EntityDataSerializer<ValheimFoodData> FOOD_DATA_SERIALIZER = new EntityDataSerializer<>() {
        @Override // copy IS required
        public ValheimFoodData copy(ValheimFoodData value) {
            var ret = new ValheimFoodData();
            ret.MaxItemSlots = value.MaxItemSlots;
            // Ensure EatenFoodItem copy constructor does a proper deep copy if needed
            ret.ItemEntries = value.ItemEntries.stream().map(EatenFoodItem::new).collect(Collectors.toCollection(ArrayList::new));
            if (value.DrinkSlot != null)
                ret.DrinkSlot = new EatenFoodItem(value.DrinkSlot);
            ret.loadConfigValues(); // Load config for the copy
            return ret;
        }

        @Override // streamCodec IS required
        public StreamCodec<? super FriendlyByteBuf, ValheimFoodData> codec() {
            return ByteBufCodecs.TRUSTED_COMPOUND_TAG.map(
                    ValheimFoodData::read,      // Method reference to static read(CompoundTag)
                    (foodData) -> foodData.save(new CompoundTag()) // Lambda calling instance save(CompoundTag)
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

    // Load config values (call this when instance created or config reloaded)
    public void loadConfigValues() {
        if (Config.COMMON != null && Config.COMMON.maxSlots != null) {
            try {
                this.MaxItemSlots = Config.COMMON.maxSlots.get();
            } catch (Exception e) { // Catch potential issues during early startup
                solneo.LOGGER.error("Failed to get maxSlots config value, using default.", e);
                this.MaxItemSlots = DEFAULT_MAX_SLOTS;
            }
        } else {
            solneo.LOGGER.warn("Config.COMMON.maxSlots not available, using default max slots ({}).", DEFAULT_MAX_SLOTS);
            this.MaxItemSlots = DEFAULT_MAX_SLOTS;
        }
    }

    // --- Methods ---
    public void eatItem(Item food) {
        if (food == Items.ROTTEN_FLESH) { // Special case: Clear food
            clear();
            solneo.LOGGER.debug("Player ate rotten flesh, clearing food data.");
            return;
        }

        // Use new Config class to get food properties
        Config.FoodConfig foodConfig = Config.getFoodConfig(food);
        if (foodConfig == null) {
            solneo.LOGGER.debug("Tried to eat item with no food config: {}", food);
            return; // Item isn't configured as food by this mod
        }

        boolean isDrink = food.getDefaultInstance().getUseAnimation() == UseAnim.DRINK;

        if (isDrink) {
            if (DrinkSlot != null && !DrinkSlot.canEatEarly()) {
                solneo.LOGGER.debug("Cannot drink {}, drink slot busy.", food);
                return;
            }
            if (DrinkSlot == null) {
                DrinkSlot = new EatenFoodItem(food, foodConfig.getTime());
                solneo.LOGGER.debug("Added {} to drink slot.", food);
            } else {
                DrinkSlot.item = food;
                DrinkSlot.ticksLeft = foodConfig.getTime();
                solneo.LOGGER.debug("Refreshed {} in drink slot.", food);
            }
            return;
        }

        // Handle non-drinks
        EatenFoodItem existing = getEatenFood(food);
        if (existing != null) {
            if (!existing.canEatEarly()) {
                solneo.LOGGER.debug("Cannot eat {}, already active and not ready.", food);
                return;
            }
            existing.ticksLeft = foodConfig.getTime();
            solneo.LOGGER.debug("Refreshed existing food {}.", food);
            return;
        }

        if (ItemEntries.size() < this.MaxItemSlots) {
            ItemEntries.add(new EatenFoodItem(food, foodConfig.getTime()));
            solneo.LOGGER.debug("Added new food {}. Slots: {}/{}", food, ItemEntries.size(), this.MaxItemSlots);
            sortFood(); // Keep sorted
            return;
        }

        // Try to replace an item that can be eaten early
        for (int i = 0; i < ItemEntries.size(); i++) {
            EatenFoodItem item = ItemEntries.get(i);
            if (item.canEatEarly()) {
                solneo.LOGGER.debug("Replacing food {} with {}.", item.item, food);
                item.item = food;
                item.ticksLeft = foodConfig.getTime();
                sortFood(); // Keep sorted
                return;
            }
        }

        solneo.LOGGER.debug("Cannot eat {}, all {} slots full and none ready.", food, this.MaxItemSlots);
    }

    public boolean canEat(Item food) {
        if (food == Items.ROTTEN_FLESH) return true; // Always allow rotten flesh

        // Use new Config class to get food properties
        Config.FoodConfig foodConfig = Config.getFoodConfig(food);
        if (foodConfig == null) {
            return false; // Cannot eat if not configured by this mod
        }

        boolean isDrink = food.getDefaultInstance().getUseAnimation() == UseAnim.DRINK;
        if (isDrink) {
            return DrinkSlot == null || DrinkSlot.canEatEarly();
        }

        EatenFoodItem existing = getEatenFood(food);
        if (existing != null) {
            return existing.canEatEarly(); // Can re-eat if ready
        }

        if (ItemEntries.size() < this.MaxItemSlots) {
            return true; // Can eat if slot available
        }

        // Can eat if any existing item can be replaced
        return ItemEntries.stream().anyMatch(EatenFoodItem::canEatEarly);
    }

    public EatenFoodItem getEatenFood(Item food) {
        for (EatenFoodItem item : ItemEntries) {
            if (item.item == food) {
                return item;
            }
        }
        return null;
    }

    public void clear() {
        ItemEntries.clear();
        DrinkSlot = null;
    }

    public void tick() {
        boolean changed = false;
        // Tick down regular food items
        var iterator = ItemEntries.iterator();
        while (iterator.hasNext()) {
            var item = iterator.next();
            item.ticksLeft--;
            if (item.ticksLeft <= 0) {
                iterator.remove();
                changed = true;
            }
        }

        // Tick down drink slot
        if (DrinkSlot != null) {
            DrinkSlot.ticksLeft--;
            if (DrinkSlot.ticksLeft <= 0) {
                DrinkSlot = null;
                changed = true;
            }
        }

        if (changed) {
            sortFood(); // Sort if items were removed
        }
    }

    private void sortFood() {
        ItemEntries.sort(Comparator.comparingInt(a -> a.ticksLeft));
    }

    // Calculates total 'hearts' value based on active foods and drink bonus
    public float getTotalHealthBonus() {
        float totalHearts = 0f;
        for (EatenFoodItem item : ItemEntries) {
            Config.FoodConfig config = Config.getFoodConfig(item.item);
            if (config != null) {
                totalHearts += config.getHearts();
            }
        }

        float drinkBonusMult = 1.0f;
        if (DrinkSlot != null) {
            Config.FoodConfig drinkConfig = Config.getFoodConfig(DrinkSlot.item);
            if (drinkConfig != null) {
                totalHearts += drinkConfig.getHearts();
            }
            if (Config.COMMON != null && Config.COMMON.drinkSlotFoodEffectivenessBonus != null) {
                try {
                    drinkBonusMult += Config.COMMON.drinkSlotFoodEffectivenessBonus.get().floatValue();
                } catch (Exception e) {solneo.LOGGER.warn("Failed to get drink bonus config");}
            }
        }

        return totalHearts * drinkBonusMult;
    }

    // Calculates total regen per tick based on active foods and drink bonus
    public float getRegenPerTick() {
        float totalRegen = 0f; // Start with 0 base regen from food
        for (EatenFoodItem item : ItemEntries) {
            Config.FoodConfig config = Config.getFoodConfig(item.item);
            if (config != null) {
                totalRegen += config.getHealthRegenPerTick();
            }
        }

        float drinkBonusMult = 1.0f;
        if (DrinkSlot != null) {
            Config.FoodConfig drinkConfig = Config.getFoodConfig(DrinkSlot.item);
            if (drinkConfig != null) {
                totalRegen += drinkConfig.getHealthRegenPerTick();
            }
            if (Config.COMMON != null && Config.COMMON.drinkSlotFoodEffectivenessBonus != null) {
                try {
                    drinkBonusMult += Config.COMMON.drinkSlotFoodEffectivenessBonus.get().floatValue();
                } catch (Exception e) {solneo.LOGGER.warn("Failed to get drink bonus config");}
            }
        }
        // Apply bonus multiplier to the SUM of regen values
        return totalRegen * drinkBonusMult;
    }


    // --- NBT Serialization ---
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("MaxSlots", this.MaxItemSlots); // Save potentially configured value
        tag.putInt("Count", ItemEntries.size());
        int savedCount = 0;
        for (int i = 0; i < ItemEntries.size(); i++) {
            EatenFoodItem foodItem = ItemEntries.get(i);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(foodItem.item);
            if (itemId != null && !itemId.equals(BuiltInRegistries.ITEM.getDefaultKey())) { // Check item is valid
                tag.putString("ID" + savedCount, itemId.toString());
                tag.putInt("Ticks" + savedCount, foodItem.ticksLeft);
                savedCount++;
            } else {
                solneo.LOGGER.warn("Attempted to save unregistered or AIR item in food slot: {}", foodItem.item);
            }
        }
        tag.putInt("Count", savedCount); // Save actual number written

        if (DrinkSlot != null) {
            ResourceLocation drinkId = BuiltInRegistries.ITEM.getKey(DrinkSlot.item);
            if (drinkId != null && !drinkId.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
                tag.putString("DrinkID", drinkId.toString());
                tag.putInt("DrinkTicks", DrinkSlot.ticksLeft);
            } else {
                solneo.LOGGER.warn("Attempted to save unregistered or AIR item in drink slot: {}", DrinkSlot.item);
            }
        } else {
            // Clean up old tags if drink slot is empty now
            tag.remove("DrinkID");
            tag.remove("DrinkTicks");
        }
        return tag;
    }

    // Static method used by serializer and capability loading
    public static ValheimFoodData read(CompoundTag tag) {
        ValheimFoodData instance = new ValheimFoodData(); // Creates instance with defaults, constructor loads config

        // Load max slots if saved, potentially overriding current config (design choice)
        if (tag.contains("MaxSlots", CompoundTag.TAG_INT)) {
            instance.MaxItemSlots = tag.getInt("MaxSlots");
        } // Otherwise, value from loadConfigValues() remains.

        instance.ItemEntries.clear(); // Clear default list
        int size = tag.getInt("Count");
        for (int i = 0; i < size; i++) {
            String idStr = tag.getString("ID" + i);
            int ticks = tag.getInt("Ticks" + i);

            if (!idStr.isEmpty()) {
                ResourceLocation itemId = ResourceLocation.tryParse(idStr);
                if (itemId != null) {
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item != Items.AIR) {
                        instance.ItemEntries.add(new EatenFoodItem(item, ticks));
                    } else {
                        solneo.LOGGER.warn("Failed to load item with ID '{}' from save data (Item not found).", idStr);
                    }
                } else {
                    solneo.LOGGER.warn("Failed to parse item ID '{}' from save data.", idStr);
                }
            }
        }

        instance.DrinkSlot = null; // Clear default
        if (tag.contains("DrinkID", CompoundTag.TAG_STRING)) {
            String drinkStr = tag.getString("DrinkID");
            int drinkTicks = tag.getInt("DrinkTicks");
            if (!drinkStr.isEmpty()) {
                ResourceLocation drinkId = ResourceLocation.tryParse(drinkStr);
                if (drinkId != null) {
                    Item drinkItem = BuiltInRegistries.ITEM.get(drinkId);
                    if (drinkItem != Items.AIR) {
                        instance.DrinkSlot = new EatenFoodItem(drinkItem, drinkTicks);
                    } else {
                        solneo.LOGGER.warn("Failed to load drink item with ID '{}' from save data (Item not found).", drinkStr);
                    }
                } else {
                    solneo.LOGGER.warn("Failed to parse drink item ID '{}' from save data.", drinkStr);
                }
            }
        }

        instance.sortFood(); // Sort after loading
        return instance;
    }

    // --- Inner Class: EatenFoodItem ---
    public static class EatenFoodItem {
        public Item item;
        public int ticksLeft;

        public boolean canEatEarly() {
            if (ticksLeft < 1200) // Always allow if less than 1 min left
                return true;

            Config.FoodConfig config = Config.getFoodConfig(item);
            if (config == null || config.getTime() <= 0)
                return false; // Cannot eat early if not configured or time is zero/negative

            float eatAgainPercentage = 0.2f; // Default if config fails
            if (Config.COMMON != null && Config.COMMON.eatAgainPercentage != null) {
                try {
                    eatAgainPercentage = Config.COMMON.eatAgainPercentage.get().floatValue();
                } catch(Exception e) { solneo.LOGGER.warn("Failed to get eatAgainPercentage config"); }
            }
            return ((float) this.ticksLeft / config.getTime()) < eatAgainPercentage;
        }

        public EatenFoodItem(Item item, int ticksLeft) {
            this.item = item;
            this.ticksLeft = ticksLeft;
        }

        // Copy Constructor
        public EatenFoodItem(EatenFoodItem eaten) {
            this.item = eaten.item; // Item is typically singleton, direct assignment okay
            this.ticksLeft = eaten.ticksLeft;
        }
    }
}