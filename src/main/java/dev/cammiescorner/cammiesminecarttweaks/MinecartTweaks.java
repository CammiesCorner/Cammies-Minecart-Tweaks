package dev.cammiescorner.cammiesminecarttweaks;

import com.google.auto.service.AutoService;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import dev.cammiescorner.cammiesminecarttweaks.init.MTBlocks;
import dev.cammiescorner.cammiesminecarttweaks.init.MTDataComponents;
import dev.cammiescorner.cammiesminecarttweaks.util.MinecartHelper;
import dev.upcraft.sparkweave.api.entrypoint.MainEntryPoint;
import dev.upcraft.sparkweave.api.item.CreativeTabHelper;
import dev.upcraft.sparkweave.api.platform.ModContainer;
import dev.upcraft.sparkweave.api.platform.services.RegistryService;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.CreativeModeTabs;

@AutoService(MainEntryPoint.class)
public class MinecartTweaks implements MainEntryPoint {
	public static final String MOD_ID = "minecarttweaks";
	private static final Configurator CONFIGURATOR = new Configurator(MOD_ID);

	@Override
	public void onInitialize(ModContainer mod) {
		CONFIGURATOR.register(MinecartTweaksConfig.class);

		var registryService = RegistryService.get();
		MTBlocks.BLOCKS.accept(registryService);
		MTDataComponents.DATA_COMPONENTS.accept(registryService);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> CreativeTabHelper.addRegistryEntries(entries.getContext(), entries, MTBlocks.BLOCKS));

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if(entity instanceof AbstractMinecart minecart) {
				var heldItem = player.getItemInHand(hand);

				if(MinecartHelper.mayAttemptLinking(player, level, hand, minecart, heldItem)) {
					if(level instanceof ServerLevel serverLevel) {
						// only check serverside config
						if(!MinecartTweaksConfig.canLinkMinecarts) {
							// TODO error message
							player.displayClientMessage(Component.literal("linking disabled in server config").withStyle(ChatFormatting.RED), true);
							return InteractionResult.FAIL;
						}

						MinecartHelper.tryLinkMinecart(player, serverLevel, hand, minecart, heldItem);
					}

					return InteractionResult.SUCCESS;
				}

				if(player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel && MinecartHelper.tryUpgradeMinecart(serverPlayer, serverLevel, hand, minecart, heldItem)) {
					player.swing(hand, true);
					return InteractionResult.SUCCESS;
				}
			}

			return InteractionResult.PASS;
		});
	}

	public static ResourceLocation id(String name) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
	}
}
