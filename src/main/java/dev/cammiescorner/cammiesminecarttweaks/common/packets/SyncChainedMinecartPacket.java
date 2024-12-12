package dev.cammiescorner.cammiesminecarttweaks.common.packets;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class SyncChainedMinecartPacket {
	public static final Identifier ID = MinecartTweaks.id("sync_chained_minecart");

	public static void send(@Nullable Entity parent, @Nullable Entity child, ServerPlayerEntity... players) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(parent != null);

		if(parent != null)
			buf.writeInt(parent.getId());

		buf.writeBoolean(child != null);
		if (child != null)
			buf.writeInt(child.getId());

		for (var player : players) {
			ServerPlayNetworking.send(player, ID, buf);
		}
	}

	@Environment(EnvType.CLIENT)
	public static void handle(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
		boolean parentExists = buf.readBoolean();
		int parentId = parentExists ? buf.readInt() : -1;

		boolean childExists = buf.readBoolean();
		int childId = childExists ? buf.readInt() : -1;

		client.submit(() -> {
			if(client.world != null) {
				ClientWorld world = client.world;

				@Nullable Entity parentEntity = world.getEntityById(parentId);
				@Nullable Entity childEntity = world.getEntityById(childId);

				if (parentEntity instanceof Linkable linkable) {
					linkable.setLinkedChildClient(childId);
				}

				if (childEntity instanceof Linkable linkable) {
					linkable.setLinkedParentClient(parentId);
				}
			}
		});
	}
}
