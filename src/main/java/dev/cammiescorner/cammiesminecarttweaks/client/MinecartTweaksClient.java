package dev.cammiescorner.cammiesminecarttweaks.client;

import com.google.auto.service.AutoService;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.init.MTBlocks;
import dev.upcraft.sparkweave.api.entrypoint.ClientEntryPoint;
import dev.upcraft.sparkweave.api.platform.ModContainer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;

@AutoService(ClientEntryPoint.class)
public class MinecartTweaksClient implements ClientEntryPoint {

	@Override
	public void onInitializeClient(ModContainer mod) {
		BlockRenderLayerMap.INSTANCE.putBlock(MTBlocks.CROSSED_RAIL.get(), RenderType.cutout());

		// TODO render chain back to player if they're currently linking a minecart, maybe make the cart glow too

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if(entity instanceof MinecartFurnace && MinecartTweaksConfig.dontEatEnchantedItems) {
				ItemStack stack = player.getItemInHand(hand);
				if(stack.isEnchanted()) {
					return InteractionResult.CONSUME;
				}
			}

			return InteractionResult.PASS;
		});
	}
}
