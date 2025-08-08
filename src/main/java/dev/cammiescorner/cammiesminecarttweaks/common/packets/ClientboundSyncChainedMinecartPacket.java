package dev.cammiescorner.cammiesminecarttweaks.common.packets;

import commonnetwork.networking.data.PacketContext;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.Nullable;

public record ClientboundSyncChainedMinecartPacket(int parentId, int childId) implements CustomPayload {
	public static final CustomPayload.Id<ClientboundSyncChainedMinecartPacket> TYPE = new CustomPayload.Id<>(MinecartTweaks.id("sync_chained_minecart"));
	public static final PacketCodec<? extends PacketByteBuf, ClientboundSyncChainedMinecartPacket> CODEC = PacketCodec.of((packet, buffer) -> {
		buffer.writeVarInt(packet.parentId);
		buffer.writeVarInt(packet.childId);
	}, buffer -> {
		int parentId = buffer.readVarInt();
		int childId = buffer.readVarInt();

		return new ClientboundSyncChainedMinecartPacket(parentId, childId);
	});

	public static void handle(PacketContext<ClientboundSyncChainedMinecartPacket> context) {
		ClientWorld world = MinecraftClient.getInstance().world;
		int parentId = context.message().parentId();
		int childId = context.message().childId();

		if(world != null) {
			@Nullable Entity parentEntity = world.getEntityById(parentId);
			@Nullable Entity childEntity = world.getEntityById(childId);

			if(parentEntity instanceof Linkable linkable)
				linkable.setLinkedChildClient(childId);
			if(childEntity instanceof Linkable linkable)
				linkable.setLinkedParentClient(parentId);
		}
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return null;
	}
}
