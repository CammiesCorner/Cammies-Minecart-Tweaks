package dev.cammiescorner.cammiesminecarttweaks.cca.component;

import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.init.MTComponents;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.jspecify.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.Optional;
import java.util.UUID;

public class LinkableData implements Component, AutoSyncedComponent {
	protected final AbstractMinecart owner;
	@Nullable
	protected UUID parentRef;
	@Nullable
	protected UUID childRef;
	public LinkableData(AbstractMinecart owner) {
		this.owner = owner;
	}

	@Override
	public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		if (tag.hasUUID("parent")) {
			parentRef = tag.getUUID("parent");
		} else {
			parentRef = null;
		}

		if (tag.hasUUID("child")) {
			childRef = tag.getUUID("child");
		} else {
			childRef = null;
		}
	}

	@Override
	public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		if (parentRef != null) {
			tag.putUUID("parent", parentRef);
		}

		if (childRef != null) {
			tag.putUUID("child", childRef);
		}
	}

	public @Nullable UUID getParentRef() {
		return parentRef;
	}

	public @Nullable Linkable getParent() {
		if (parentRef == null || parentRef.equals(Util.NIL_UUID)) {
			return null;
		}

		return ((ServerLevel) owner.level()).getEntity(parentRef) instanceof Linkable linkable ? linkable : null;
	}

	public void setParentRef(@Nullable UUID parentRef) {
		this.parentRef = parentRef;
	}

	public @Nullable UUID getChildRef() {
		return childRef;
	}

	public @Nullable Linkable getChild() {
		if (childRef == null || childRef.equals(Util.NIL_UUID)) {
			return null;
		}

		return ((ServerLevel) owner.level()).getEntity(childRef) instanceof Linkable linkable ? linkable : null;
	}

	public void setChildRef(@Nullable UUID childRef) {
		this.childRef = childRef;
	}

	@Override
	public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
		buf.writeOptional(Optional.ofNullable(getParent()).filter(Entity.class::isInstance).map(Entity.class::cast).map(Entity::getId), ByteBufCodecs.VAR_INT);
		buf.writeOptional(Optional.ofNullable(getChild()).filter(Entity.class::isInstance).map(Entity.class::cast).map(Entity::getId), ByteBufCodecs.VAR_INT);
	}

	public void sync() {
		MTComponents.LINKABLE.sync(owner);
	}

	public static class Client extends LinkableData {
		@Nullable
		private Integer parentId;
		@Nullable
		private Integer childId;

		public Client(AbstractMinecart owner) {
			super(owner);
		}

		@Override
		public @Nullable Linkable getParent() {
			if(parentId == null) {
				return null;
			}

			return owner.level().getEntity(parentId) instanceof Linkable linkable ? linkable : null;
		}

		@Override
		public @Nullable Linkable getChild() {
			if(childId == null) {
				return null;
			}

			return owner.level().getEntity(childId) instanceof Linkable linkable ? linkable : null;
		}

		@Override
		public @Nullable UUID getParentRef() {
			return getParent() instanceof Entity entity ? entity.getUUID() : null;
		}

		@Override
		public @Nullable UUID getChildRef() {
			return getChild() instanceof Entity entity ? entity.getUUID() : null;
		}

		@Override
		public void applySyncPacket(RegistryFriendlyByteBuf buf) {
			parentId = buf.readOptional(ByteBufCodecs.VAR_INT).orElse(null);
			childId = buf.readOptional(ByteBufCodecs.VAR_INT).orElse(null);
		}
	}
}
