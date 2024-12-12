package dev.cammiescorner.cammiesminecarttweaks.api;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface injected onto AbstractMinecartEntity to facilitate linking carts together.
 */
public interface Linkable {
	default @Nullable AbstractMinecartEntity getLinkedParent() {
		return null;
	}
	default void setLinkedParent(@Nullable AbstractMinecartEntity parent) {}

	default @Nullable AbstractMinecartEntity getLinkedChild() {
		return null;
	}
	default void setLinkedChild(@Nullable AbstractMinecartEntity child) {}

	default void setLinkedParentClient(int id) {}
	default void setLinkedChildClient(int id) {}

	default AbstractMinecartEntity asAbstractMinecartEntity() { return (AbstractMinecartEntity) this; }

	static void setParentChild(@NotNull Linkable parent, @NotNull Linkable child) {
		unsetParentChild(parent, parent.getLinkedChild());
		unsetParentChild(child, child.getLinkedParent());
		parent.setLinkedChild(child.asAbstractMinecartEntity());
		child.setLinkedParent(parent.asAbstractMinecartEntity());
	}

	static void unsetParentChild(@Nullable Linkable parent, @Nullable Linkable child) {
		if (parent != null) {
			parent.setLinkedChild(null);
		}
		if (child != null) {
			child.setLinkedParent(null);
		}
	}
}
