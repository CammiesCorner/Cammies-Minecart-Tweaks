package dev.cammiescorner.cammiesminecarttweaks.packets;

import commonnetwork.networking.data.PacketContext;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

public record ClientboundSyncChainedMinecartPacket(int parentId, int childId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClientboundSyncChainedMinecartPacket> TYPE = new CustomPacketPayload.Type<>(MinecartTweaks.id("sync_chained_minecart"));
	public static final StreamCodec<? extends RegistryFriendlyByteBuf, ClientboundSyncChainedMinecartPacket> CODEC = StreamCodec.composite(
		ByteBufCodecs.INT,
		ClientboundSyncChainedMinecartPacket::parentId,
		ByteBufCodecs.INT,
		ClientboundSyncChainedMinecartPacket::childId,
		ClientboundSyncChainedMinecartPacket::new
	);

	public static void handle(PacketContext<ClientboundSyncChainedMinecartPacket> context) {
		var level = Minecraft.getInstance().level;
		int parentId = context.message().parentId();
		int childId = context.message().childId();

		if(level != null) {
			@Nullable var parentEntity = level.getEntity(parentId);
			@Nullable var childEntity = level.getEntity(childId);

			if(parentEntity instanceof Linkable linkable)
				linkable.setLinkedChildClient(childId);
			if(childEntity instanceof Linkable linkable)
				linkable.setLinkedParentClient(parentId);
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
