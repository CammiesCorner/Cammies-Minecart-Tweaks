package dev.cammiescorner.cammiesminecarttweaks.util.ext;

public interface MinecartPhysicsAccess {
	default boolean isSelfMovingOnRail() {
		throw new AssertionError("Implemented in Mixin");
	}
}
