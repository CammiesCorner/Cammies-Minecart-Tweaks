package dev.cammiescorner.cammiesminecarttweaks.init;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.cca.component.PoweredMinecartData;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;

public class MTComponents implements EntityComponentInitializer {

	public static final ComponentKey<PoweredMinecartData> POWERED_MINECART = ComponentRegistry.getOrCreate(MinecartTweaks.id("powered_minecart"), PoweredMinecartData.class);

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerFor(MinecartFurnace.class, POWERED_MINECART, PoweredMinecartData::new);
	}
}
