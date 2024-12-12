package dev.cammiescorner.cammiesminecarttweaks.client;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.common.compat.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.common.packets.SyncChainedMinecartPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

public class MinecartTweaksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SyncChainedMinecartPacket.ID, SyncChainedMinecartPacket::handle);
		BlockRenderLayerMap.INSTANCE.putBlock(MinecartTweaks.CROSSED_RAIL, RenderLayer.getCutout());

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack stack = player.getStackInHand(hand);

			if(world.isClient() && entity instanceof FurnaceMinecartEntity && MinecartTweaksConfig.dontEatEnchantedItems && stack.hasEnchantments())
				return ActionResult.CONSUME;

			return ActionResult.PASS;
		});
	}
}
