package dev.cammiescorner.cammiesminecarttweaks.util.ext;

import net.minecraft.world.entity.vehicle.AbstractMinecart;

public interface MinecartItemExt {

	default AbstractMinecart.Type minecarttweaks$getType() {
		throw new AssertionError("Implemented in mixin");
	}
}
