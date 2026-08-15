package dev.cammiescorner.cammiesminecarttweaks.ext;

import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.util.MinecartPhysicsAccess;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

public interface AbstractMinecartExt extends Linkable, MinecartPhysicsAccess {

	@Override
	default @Nullable Linkable getLinkedParent() {
		throw new AssertionError("Implemented in Mixin");
	}

	@Override
	default void setLinkedParent(@Nullable Linkable parent) {
		throw new AssertionError("Implemented in Mixin");
	}

	@Override
	default @Nullable Linkable getLinkedChild() {
		throw new AssertionError("Implemented in Mixin");
	}

	@Override
	default void setLinkedChild(@Nullable Linkable child) {
		throw new AssertionError("Implemented in Mixin");
	}

	@ApiStatus.Internal
	@Override
	default boolean isSelfMovingOnRail() {
		throw new AssertionError("Implemented in Mixin");
	}
}
