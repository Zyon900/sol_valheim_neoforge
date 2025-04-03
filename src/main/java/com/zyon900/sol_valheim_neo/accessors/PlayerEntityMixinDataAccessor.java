package com.zyon900.sol_valheim_neo.accessors;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.zyon900.sol_valheim_neo.ValheimFoodData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

public interface PlayerEntityMixinDataAccessor
{
    ValheimFoodData sol_valheim$getFoodData();
}
