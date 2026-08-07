package dev.cammiescorner.cammiesminecarttweaks.data;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public class MTDamageTypes {
	public static final ResourceKey<DamageType> MINECART_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, MinecartTweaks.id("minecart"));
}
