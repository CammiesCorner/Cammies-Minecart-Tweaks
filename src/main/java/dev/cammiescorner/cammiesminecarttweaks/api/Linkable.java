package dev.cammiescorner.cammiesminecarttweaks.api;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface injected onto {@link AbstractMinecart} to facilitate linking carts together.
 */
public interface Linkable {
	default @Nullable AbstractMinecart getLinkedParent() {
		return null;
	}
	default void setLinkedParent(@Nullable AbstractMinecart parent) {}

	default @Nullable AbstractMinecart getLinkedChild() {
		return null;
	}
	default void setLinkedChild(@Nullable AbstractMinecart child) {}

	default void setLinkedParentClient(int id) {}
	default void setLinkedChildClient(int id) {}

	/**
	 * @deprecated use {@link #asAbstractMinecart()}
	 */
	@Deprecated(forRemoval = true)
	default AbstractMinecart asAbstractMinecartEntity() { return asAbstractMinecart(); }

	default AbstractMinecart asAbstractMinecart() { return (AbstractMinecart) this; }

	static void setParentChild(@NotNull Linkable parent, @NotNull Linkable child) {
		unsetParentChild(parent, parent.getLinkedChild());
		unsetParentChild(child, child.getLinkedParent());
		parent.setLinkedChild(child.asAbstractMinecart());
		child.setLinkedParent(parent.asAbstractMinecart());
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
