package dev.cammiescorner.cammiesminecarttweaks;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import commonnetwork.api.Network;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.init.MTBlocks;
import dev.cammiescorner.cammiesminecarttweaks.init.MTDataComponents;
import dev.cammiescorner.cammiesminecarttweaks.packets.ClientboundSyncChainedMinecartPacket;
import dev.upcraft.sparkweave.api.item.CreativeTabHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MinecartTweaks implements ModInitializer {
	public static final String MOD_ID = "minecarttweaks";
	private static final Configurator CONFIGURATOR = new Configurator(MOD_ID);

	@Override
	public void onInitialize() {
		CONFIGURATOR.register(MinecartTweaksConfig.class);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> CreativeTabHelper.addRegistryEntries(entries.getContext(), entries, MTBlocks.BLOCKS));

		Network.registerPacket(ClientboundSyncChainedMinecartPacket.TYPE, ClientboundSyncChainedMinecartPacket.class, ClientboundSyncChainedMinecartPacket.CODEC, ClientboundSyncChainedMinecartPacket::handle);

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if(entity instanceof Minecart ridableCart && ridableCart.getMinecartType() == AbstractMinecart.Type.RIDEABLE) {
				var parent = ridableCart.getLinkedParent();
				var child = ridableCart.getLinkedChild();
				var stack = player.getItemInHand(hand);
				var item = stack.getItem();
				var type = AbstractMinecart.Type.RIDEABLE;

				if(item == Items.FURNACE)
					type = AbstractMinecart.Type.FURNACE;
				if(item == Items.CHEST)
					type = AbstractMinecart.Type.CHEST;
				if(item == Items.TNT)
					type = AbstractMinecart.Type.TNT;
				if(item == Items.HOPPER)
					type = AbstractMinecart.Type.HOPPER;

				if(type != AbstractMinecart.Type.RIDEABLE && level instanceof ServerLevel serverLevel) {
					var minecart = AbstractMinecart.createMinecart(serverLevel, ridableCart.getX(), ridableCart.getY(), ridableCart.getZ(), type, stack, player);
					minecart.copyPosition(ridableCart);
					level.addFreshEntity(minecart);

					if(parent != null) {
						Linkable.unsetParentChild(parent, ridableCart);
						Linkable.setParentChild(parent, minecart);
					}
					if(child != null) {
						Linkable.unsetParentChild(ridableCart, child);
						Linkable.setParentChild(minecart, child);
					}

					ridableCart.remove(Entity.RemovalReason.DISCARDED);

					if(!player.isCreative())
						stack.shrink(1);

					return InteractionResult.sidedSuccess(level.isClientSide());
				}
			}

			return InteractionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if(entity instanceof AbstractMinecart cart && MinecartTweaksConfig.canLinkMinecarts) {
				ItemStack stack = player.getItemInHand(hand);

				if(player.isShiftKeyDown() && stack.is(Items.CHAIN)) {
					if(level instanceof ServerLevel serverLevel) {
						UUID uuid = stack.get(MTDataComponents.PARENT_ID.get());

						if(uuid != null && !cart.getUUID().equals(uuid)) {
							if(serverLevel.getEntity(uuid) instanceof AbstractMinecart parent) {
								Set<Linkable> train = new HashSet<>();
								train.add(parent);

								AbstractMinecart nextParent;
								while((nextParent = parent.getLinkedParent()) != null && !train.contains(nextParent)) {
									train.add(nextParent);
								}

								if(train.contains(cart) || parent.getLinkedChild() != null) {
									player.displayClientMessage(Component.translatable("minecarttweaks.cant_link_to_engine").withStyle(ChatFormatting.RED), true);
								}
								else {
									if(cart.getLinkedParent() != null)
										Linkable.unsetParentChild(cart, cart.getLinkedParent());

									Linkable.setParentChild(parent, cart);
								}
							}
							else {
								stack.remove(MTDataComponents.PARENT_ID.get());
							}

							level.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1f, 1f);

							if(!player.isCreative()) {
								stack.shrink(1);
							}

							stack.remove(MTDataComponents.PARENT_ID.get());
						}
						else {
							stack.set(MTDataComponents.PARENT_ID.get(), cart.getUUID());
							level.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 1f, 1f);
						}
					}

					return InteractionResult.sidedSuccess(level.isClientSide());
				}
			}

			return InteractionResult.PASS;
		});
	}

	public static ResourceLocation id(String name) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
	}
}
