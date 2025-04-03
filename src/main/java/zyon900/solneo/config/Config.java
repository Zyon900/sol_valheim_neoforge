package zyon900.solneo.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import zyon900.solneo.solneo; // Import main mod class for logger

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    // --- Config Specs and Holders ---
    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        final Pair<Common, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = commonPair.getRight();
        COMMON = commonPair.getLeft();

        final Pair<Client, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    // --- Parsed Food Config Data ---
    // This map will hold the parsed data from the foodProperties list config
    private static Map<ResourceLocation, FoodConfig> parsedFoodConfigs = Collections.emptyMap();

    // Call this method during FMLCommonSetupEvent AFTER config is loaded
    public static void loadAndParseFoodConfigs() {
        parsedFoodConfigs = parseFoodPropertiesList(COMMON.foodProperties.get());
        solneo.LOGGER.info("Parsed {} food config entries.", parsedFoodConfigs.size());
    }

    // Parses the List<String> from the config into the Map
    private static Map<ResourceLocation, FoodConfig> parseFoodPropertiesList(List<? extends String> list) {
        Map<ResourceLocation, FoodConfig> map = new HashMap<>();
        if (list == null) return map; // Handle null case

        for (String entry : list) {
            try {
                String[] parts = entry.split("\\|"); // Split by pipe
                if (parts.length >= 4) { // Need at least ID, duration, hearts, regen
                    ResourceLocation itemId = ResourceLocation.tryParse(parts[0].trim());
                    int duration = Integer.parseInt(parts[1].trim());
                    float hearts = Float.parseFloat(parts[2].trim());
                    float regen = Float.parseFloat(parts[3].trim());

                    List<MobEffectConfig> effects = new ArrayList<>();
                    // Optional: Parse extra effects if included (e.g., part 4 onwards)
                    // Example format: "minecraft:haste|1.0|0" for effect ID, duration multiplier, amplifier
                    if (parts.length > 4) {
                        for (int i = 4; i < parts.length; i++) {
                            String[] effectParts = parts[i].trim().split(":");
                            if (effectParts.length == 3) {
                                effects.add(new MobEffectConfig(effectParts[0], Float.parseFloat(effectParts[1]), Integer.parseInt(effectParts[2])));
                            } else {
                                solneo.LOGGER.warn("Invalid mob effect format in food config entry part: {}", parts[i]);
                            }
                        }
                    }


                    if (itemId != null) {
                        map.put(itemId, new FoodConfig(itemId, duration, hearts, regen, effects));
                    } else {
                        solneo.LOGGER.warn("Invalid ResourceLocation in food config: {}", parts[0]);
                    }
                } else {
                    solneo.LOGGER.warn("Invalid format in food config entry (expected at least 4 parts): {}", entry);
                }
            } catch (Exception e) {
                solneo.LOGGER.error("Error parsing food config entry: '{}'", entry, e);
            }
        }
        return map;
    }

    /**
     * Gets the parsed FoodConfig for the given item.
     * Returns null if the item is not considered food by this mod's logic
     * (doesn't have food component, isn't cake, isn't a drink)
     * or if no specific config is found.
     */
    public static FoodConfig getFoodConfig(Item item) {
        boolean isDrink = item.getDefaultInstance().getUseAnimation() == UseAnim.DRINK;

        // Check if the item has the FOOD data component OR is Cake OR is considered a drink
        boolean consideredFood = item.components().has(DataComponents.FOOD) || item == Items.CAKE || isDrink;

        if (!consideredFood) {
            // If it's not food, cake, or a drink, return null
            return null;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        // Return the parsed config if found, otherwise null
        return parsedFoodConfigs.get(itemId); // parsedFoodConfigs is populated by loadAndParseFoodConfigs
    }


    // --- Common Config Definition ---
    public static class Common {
        public final ModConfigSpec.IntValue defaultTimer;
        public final ModConfigSpec.IntValue regenDelay;
        public final ModConfigSpec.IntValue respawnGracePeriod;
        public final ModConfigSpec.IntValue startingHealth; // Note: Applied via attribute modification now
        public final ModConfigSpec.IntValue maxSlots;
        public final ModConfigSpec.DoubleValue regenSpeedModifier; // Now used as multiplier/divisor in tick logic
        public final ModConfigSpec.DoubleValue speedBoost;
        public final ModConfigSpec.DoubleValue eatAgainPercentage;
        public final ModConfigSpec.DoubleValue drinkSlotFoodEffectivenessBonus;
        public final ModConfigSpec.BooleanValue passTicksDuringNight;
        public final ModConfigSpec.ConfigValue<List<? extends String>> foodProperties;

        Common(ModConfigSpec.Builder builder) {
            builder.comment("Common configuration settings").push("common");

            defaultTimer = builder
                    .comment("Base time in seconds food should last. Actual duration calculated using nutrition/saturation.")
                    .defineInRange("defaultTimer", 180, 10, 7200);

            regenDelay = builder
                    .comment("Time in ticks that regeneration should wait after taking damage.")
                    .defineInRange("regenDelay", 20 * 10, 0, 1200); // 10 seconds default

            respawnGracePeriod = builder
                    .comment("Time in seconds after spawning/respawning before sprinting requires food.")
                    .defineInRange("respawnGracePeriod", 60 * 5, 0, 3600); // 5 minutes default

            startingHealth = builder
                    .comment("Base number of hearts (health points / 2) the player starts with before food bonuses.")
                    .defineInRange("startingHealth", 3, 1, 20); // Default 3 hearts (6 health)

            maxSlots = builder
                    .comment("Maximum number of distinct food items a player can have active (excluding drink).")
                    .defineInRange("maxSlots", 3, 1, 10);

            regenSpeedModifier = builder
                    .comment("General modifier for health regen effectiveness. Lower value = faster regen intervals. 1.0 = default.")
                    .defineInRange("regenSpeedModifier", 1.0, 0.1, 10.0);

            speedBoost = builder
                    .comment("Movement speed multiplier applied when player health (considering food) is at or above 20 (10 hearts). 0 = disable.")
                    .defineInRange("speedBoost", 0.1, 0.0, 1.0); // Changed from 0.20 to 0.1 for +10%

            eatAgainPercentage = builder
                    .comment("Percentage of food duration remaining when player can eat the same food again (e.g., 0.25 means usable when <25% duration left).")
                    .defineInRange("eatAgainPercentage", 0.20, 0.0, 1.0); // Changed from 0.2F

            drinkSlotFoodEffectivenessBonus = builder
                    .comment("Multiplier bonus applied to total hearts/regen calculation when a drink is active (e.g., 0.1 = +10% bonus). Set to 0 to disable.")
                    .defineInRange("drinkSlotFoodEffectivenessBonus", 0.10, 0.0, 5.0);

            passTicksDuringNight = builder
                    .comment("Simulate food ticking down when players sleep through the night.")
                    .define("passTicksDuringNight", true);

            foodProperties = builder
                    .comment("Define properties for food items.",
                            "Format: \"item_registry_name|duration_ticks|hearts_restored|health_regen_points_per_tick\"",
                            "Example: \"minecraft:cooked_beef|1800|8.0|0.025\" (0.025 * 20 ticks = 0.5/sec)",
                            "Optionally add mob effects after regen: |effect_id:duration_mult:amplifier|...",
                            "Example: \"minecraft:golden_apple|2400|4.0|0.05|minecraft:regeneration:1.0:1|minecraft:absorption:1.0:0\""
                    )
                    .defineListAllowEmpty(
                            List.of("foodProperties"),
                            () -> Arrays.asList( // Provide sensible defaults
                                    "minecraft:apple|600|4|0.005", // 4 hearts, 0.1 regen/sec
                                    "minecraft:bread|1200|5|0.005",
                                    "minecraft:cooked_porkchop|1800|8|0.01",
                                    "minecraft:cooked_beef|1800|8|0.01",
                                    "minecraft:cooked_chicken|1400|6|0.0075",
                                    "minecraft:cooked_cod|1200|5|0.005",
                                    "minecraft:cooked_salmon|1400|6|0.0075",
                                    "minecraft:cooked_mutton|1400|6|0.0075",
                                    "minecraft:cooked_rabbit|1200|5|0.005",
                                    "minecraft:baked_potato|1200|5|0.005",
                                    "minecraft:carrot|800|3|0.0025",
                                    "minecraft:beetroot|400|1|0.001",
                                    "minecraft:beetroot_soup|1400|6|0.0075",
                                    "minecraft:pumpkin_pie|1800|8|0.01",
                                    "minecraft:mushroom_stew|1400|6|0.0075",
                                    "minecraft:rabbit_stew|2000|10|0.0125",
                                    "minecraft:suspicious_stew|800|6|0.0075", // Effects handled by vanilla
                                    "minecraft:cake|1|14|0.0175", // Duration handled by block logic / per slice
                                    "minecraft:cookie|400|2|0.001",
                                    "minecraft:melon_slice|600|2|0.001",
                                    "minecraft:dried_kelp|200|1|0.0",
                                    "minecraft:honey_bottle|600|6|0.001", // Drink
                                    "minecraft:milk_bucket|1|0|0.0", // Drink, clears effects
                                    "minecraft:potion|600|0|0.0", // Drink base for potions
                                    "minecraft:golden_carrot|1600|6|0.0075",
                                    "minecraft:golden_apple|2400|4|0.05|minecraft:regeneration:1.0:1|minecraft:absorption:1.0:0", // Higher regen + effects
                                    "minecraft:enchanted_golden_apple|3600|4|0.1|minecraft:regeneration:1.0:4|minecraft:absorption:1.0:3|minecraft:resistance:1.0:0|minecraft:fire_resistance:1.0:0" // Even higher regen + effects
                            ),
                            obj -> obj instanceof String s && s.contains("|") && s.split("\\|").length >= 4 // Basic validation
                    );

            builder.pop();
        }
    }

    // --- Client Config Definition ---
    public static class Client {
        public final ModConfigSpec.BooleanValue useLargeIcons;

        Client(ModConfigSpec.Builder builder) {
            builder.comment("Client-side visual settings").push("client");

            useLargeIcons = builder
                    .comment("Enlarge the currently eaten food icons on the HUD.")
                    .define("useLargeIcons", true);

            builder.pop();
        }
    }

    // --- Helper Structures for Parsed Food Data ---
    public static class FoodConfig {
        public final ResourceLocation id;
        public final int timeTicks; // Duration in ticks
        public final float hearts; // Health points (2 = 1 heart)
        public final float healthRegenPerTick; // Direct regen value per tick
        public final List<MobEffectConfig> extraEffects;

        // Private constructor, use parsing method
        private FoodConfig(ResourceLocation id, int time, float hearts, float healthRegen, List<MobEffectConfig> effects) {
            this.id = id;
            this.timeTicks = Math.max(1, time); // Ensure time is at least 1 tick
            this.hearts = Math.max(0, hearts); // Ensure hearts is non-negative
            this.healthRegenPerTick = healthRegen;
            this.extraEffects = Collections.unmodifiableList(effects != null ? effects : new ArrayList<>());
        }

        public int getTime() { return timeTicks; }
        public float getHearts() { return hearts; }
        public float getHealthRegenPerTick() { return healthRegenPerTick; }
        public List<MobEffectConfig> getExtraEffects() { return extraEffects; }

        // Helper to calculate regen per second for display/tooltips if needed
        public float getHealthRegenPerSecond() { return healthRegenPerTick * 20.0f; }
    }

    public static class MobEffectConfig {
        public final String effectId; // String ID from config
        public final float durationMultiplier; // Multiplier for total food duration (0.0 to 1.0)
        public final int amplifier; // Effect amplifier (0 = Level I)
        private MobEffect cachedEffect = null; // Cache looked-up effect

        private MobEffectConfig(String id, float durationMult, int amp) {
            this.effectId = id;
            this.durationMultiplier = Mth.clamp(durationMult, 0.0f, 1.0f);
            this.amplifier = Math.max(0, amp);
        }

        // Lazily get and cache the MobEffect instance
        public MobEffect getEffect() {
            if (cachedEffect == null) {
                ResourceLocation effectRL = ResourceLocation.tryParse(effectId);
                if (effectRL != null) {
                    cachedEffect = BuiltInRegistries.MOB_EFFECT.get(effectRL);
                    if (cachedEffect == null) {
                        solneo.LOGGER.warn("Could not find mob effect with ID: {}", effectId);
                    }
                } else {
                    solneo.LOGGER.warn("Invalid mob effect ResourceLocation: {}", effectId);
                }
            }
            return cachedEffect;
        }

        public int getAmplifier() { return amplifier; }

        // Calculate actual duration in ticks based on food duration
        public int getDurationTicks(int foodDurationTicks) {
            return (int) (foodDurationTicks * durationMultiplier);
        }
    }
}