package com.zyon900.sol_valheim_neo;// Or your chosen package

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair; // Required for the standard pattern

import java.util.Arrays;
import java.util.List;

public class Config {

    // --- Holder for Common Config Spec and its Values ---
    // We define the spec and its holder together using this Pair pattern
    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        // Create a builder
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // Build the common config category and its values
        COMMON = new Common(builder);

        // Finalize the spec for the COMMON type
        COMMON_SPEC = builder.build();
    }

    public static Common.FoodConfig getFoodConfig(Item item) {
        return null;
    }

    // --- Common Config Category Definition ---
    public static class Common {

        public static class FoodConfig {
            public float getHearts() {
                return 0f;
            }
            public float getHealthRegen() {
                return 0f;
            }

            public int getTime() {
                return 0;
            }
            // fields like time, hearts, healthRegen
            // constructor
        }

        // --- General Settings ---
        public final ModConfigSpec.IntValue maxSlots;
        public final ModConfigSpec.DoubleValue eatAgainPercentage;
        public final ModConfigSpec.DoubleValue drinkSlotFoodEffectivenessBonus;
        public final ModConfigSpec.BooleanValue enableDebugMessages; // Example boolean

        // --- Food Specific Configs (Example using a list of strings) ---
        // Format example: "minecraft:apple|200|1.0|0.1" -> item_id|duration_ticks|hearts|regen
        // You will need to parse this list later.
        public final ModConfigSpec.ConfigValue<List<? extends String>> foodProperties;

        // --- Constructor for the Common category ---
        Common(ModConfigSpec.Builder builder) {
            // Group settings under a comment/category in the TOML file
            builder.comment("Common configuration settings for SoL: Valheim Neoforged").push("common");

            // --- Define individual values ---
            maxSlots = builder
                    .comment("Maximum number of distinct food items a player can have active (excluding drink).")
                    .translation("config.solvalheimneoforged.maxSlots") // For localization
                    .defineInRange("maxSlots", 3, 1, 10); // name, default, min, max

            eatAgainPercentage = builder
                    .comment("Percentage of food duration remaining at which the player can eat the same food again (e.g., 0.25 means usable when <25% duration left).")
                    .translation("config.solvalheimneoforged.eatAgainPercentage")
                    .defineInRange("eatAgainPercentage", 0.25, 0.0, 1.0); // name, default, min, max

            drinkSlotFoodEffectivenessBonus = builder
                    .comment("Multiplier bonus applied to total hearts/regen when a drink is active (e.g., 0.1 = +10% bonus). Set to 0 to disable.")
                    .translation("config.solvalheimneoforged.drinkSlotFoodEffectivenessBonus")
                    .defineInRange("drinkSlotFoodEffectivenessBonus", 0.1, 0.0, 5.0); // name, default, min, max

            enableDebugMessages = builder
                    .comment("Enable extra debug messages in the log.")
                    .translation("config.solvalheimneoforged.enableDebugMessages")
                    .define("enableDebugMessages", false); // name, default

            // Example for more complex list config
            foodProperties = builder
                    .comment("Define properties for food items.",
                            "Format: \"item_registry_name|duration_ticks|hearts_restored|health_regen_per_second\"",
                            "Example: \"minecraft:cooked_beef|1800|8.0|0.5\"")
                    .translation("config.solvalheimneoforged.foodProperties")
                    // Define a list of strings, providing default values.
                    .defineListAllowEmpty( // Allows the list to be empty
                            List.of("foodProperties"), // Path within the config section
                            () -> Arrays.asList( // Default values
                                    "minecraft:apple|600|4.0|0.1",
                                    "minecraft:cooked_beef|1800|8.0|0.5",
                                    "minecraft:golden_apple|2400|4.0|1.0",
                                    "minecraft:poisonous_potato|300|2.0|-0.2" // Example negative regen
                            ),
                            obj -> obj instanceof String s && s.contains("|") // Validator: Check if it's a String containing '|'
                    );


            // Pop the category stack
            builder.pop();
        }
    }
}