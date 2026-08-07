package dev.cammiescorner.cammiesminecarttweaks.init;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.util.XtraCodecs;
import dev.upcraft.sparkweave.api.registry.RegistryHandler;
import dev.upcraft.sparkweave.api.registry.RegistrySupplier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import java.util.UUID;

public class MTDataComponents {

	public static final RegistryHandler<DataComponentType<?>> DATA_COMPONENTS = RegistryHandler.create(Registries.DATA_COMPONENT_TYPE, MinecartTweaks.MOD_ID);
	public static final RegistrySupplier<DataComponentType<UUID>> PARENT_ID = DATA_COMPONENTS.register("parent_id", () -> DataComponentType.<UUID>builder().persistent(XtraCodecs.UUID_CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());
}
