package dev.cammiescorner.cammiesminecarttweaks.init;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.datacomponent.ParentId;
import dev.upcraft.sparkweave.api.registry.RegistryHandler;
import dev.upcraft.sparkweave.api.registry.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public class MTDataComponents {
	public static final RegistryHandler<DataComponentType<?>> DATA_COMPONENTS = RegistryHandler.create(Registries.DATA_COMPONENT_TYPE, MinecartTweaks.MOD_ID);

	public static final RegistrySupplier<DataComponentType<ParentId>> PARENT_ID = DATA_COMPONENTS.register("parent_id", () -> DataComponentType.<ParentId>builder().persistent(ParentId.CODEC).networkSynchronized(ParentId.STREAM_CODEC).build());
}
