package dev.cammiescorner.cammiesminecarttweaks.client;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.common.compat.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.common.packets.SyncChainedMinecartPacket;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.block.extensions.api.client.BlockRenderLayerMap;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

public class MinecartTweaksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(ModContainer mod) {
		ClientPlayNetworking.registerGlobalReceiver(SyncChainedMinecartPacket.ID, SyncChainedMinecartPacket::handle);
		BlockRenderLayerMap.put(RenderLayer.getCutout(), MinecartTweaks.CROSSED_RAIL);

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack stack = player.getStackInHand(hand);

			if(world.isClient() && entity instanceof FurnaceMinecartEntity && MinecartTweaksConfig.dontEatEnchantedItems && stack.hasEnchantments())
				return ActionResult.CONSUME;

			return ActionResult.PASS;
		});
	}
}
