package dev.cammiescorner.cammiesminecarttweaks.api;

import dev.cammiescorner.cammiesminecarttweaks.cca.component.LinkableData;
import dev.cammiescorner.cammiesminecarttweaks.init.MTComponents;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.jspecify.annotations.Nullable;

/**
 * Interface injected onto {@link AbstractMinecart} to facilitate linking carts together.
 */
public interface Linkable {

	@Nullable Linkable getLinkedParent();
	void setLinkedParent(@Nullable Linkable parent);

	@Nullable Linkable getLinkedChild();
	void setLinkedChild(@Nullable Linkable child);

	static void setParentChild(@Nullable Linkable parent, @Nullable Linkable child) {
		if(child != null) {
			var prevParent = child.getLinkedParent();
			if(prevParent != null) {
				prevParent.setLinkedChild(null);
				MTComponents.LINKABLE.maybeGet(prevParent).ifPresent(LinkableData::sync);
			}

			child.setLinkedParent(parent);
		}

		if(parent != null) {
			var prevChild = parent.getLinkedChild();
			if(prevChild != null) {
				prevChild.setLinkedParent(null);
				MTComponents.LINKABLE.maybeGet(prevChild).ifPresent(LinkableData::sync);
			}

			parent.setLinkedChild(child);
		}

		MTComponents.LINKABLE.maybeGet(parent).ifPresent(LinkableData::sync);
		MTComponents.LINKABLE.maybeGet(child).ifPresent(LinkableData::sync);
	}
}
