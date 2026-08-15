package dev.cammiescorner.cammiesminecarttweaks.init;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.cca.component.LinkableData;
import dev.cammiescorner.cammiesminecarttweaks.cca.component.PoweredMinecartData;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;

public class MTComponents implements EntityComponentInitializer {

	public static final ComponentKey<PoweredMinecartData> POWERED_MINECART = ComponentRegistry.getOrCreate(MinecartTweaks.id("powered_minecart"), PoweredMinecartData.class);
	public static final ComponentKey<LinkableData> LINKABLE = ComponentRegistry.getOrCreate(MinecartTweaks.id("linkable"), LinkableData.class);

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerFor(MinecartFurnace.class, POWERED_MINECART, PoweredMinecartData::new);
		registry.beginRegistration(AbstractMinecart.class, LINKABLE)
			.end(minecart -> {
				if(minecart.level().isClientSide()) {
					return new LinkableData.Client(minecart);
				}

				return new LinkableData(minecart);
			});
	}
}
