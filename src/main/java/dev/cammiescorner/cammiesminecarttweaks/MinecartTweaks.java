package dev.cammiescorner.cammiesminecarttweaks;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import commonnetwork.api.Network;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.common.blocks.CrossedRailBlock;
import dev.cammiescorner.cammiesminecarttweaks.common.packets.ClientboundSyncChainedMinecartPacket;
import dev.cammiescorner.cammiesminecarttweaks.common.utils.XtraCodecs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MinecartTweaks implements ModInitializer {
	public static final String MOD_ID = "minecarttweaks";
	public static final Block CROSSED_RAIL = new CrossedRailBlock(AbstractBlock.Settings.copy(Blocks.RAIL));
	public static final ComponentType<UUID> PARENT_ID = ComponentType.<UUID>builder().codec(XtraCodecs.UUID_CODEC).packetCodec(XtraCodecs.UUID_PACKET_CODEC).build();
	public static final RegistryKey<DamageType> MINECART_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id("minecart"));
	public static final Configurator CONFIGURATOR = new Configurator(MOD_ID);

	@Override
	public void onInitialize() {
		CONFIGURATOR.register(MinecartTweaksConfig.class);

		Registry.register(Registries.BLOCK, id("crossed_rail"), CROSSED_RAIL);
		Registry.register(Registries.ITEM, id("crossed_rail"), new BlockItem(CROSSED_RAIL, new Item.Settings()));
		Registry.register(Registries.DATA_COMPONENT_TYPE, id("parent_id"), PARENT_ID);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> entries.add(CROSSED_RAIL));

		Network.registerPacket(ClientboundSyncChainedMinecartPacket.TYPE, ClientboundSyncChainedMinecartPacket.class, ClientboundSyncChainedMinecartPacket.CODEC, ClientboundSyncChainedMinecartPacket::handle);

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if(entity instanceof MinecartEntity ridableCart && ridableCart.getMinecartType() == AbstractMinecartEntity.Type.RIDEABLE) {
				AbstractMinecartEntity parent = ridableCart.getLinkedParent();
				AbstractMinecartEntity child = ridableCart.getLinkedChild();
				ItemStack stack = player.getStackInHand(hand);
				Item item = stack.getItem();
				AbstractMinecartEntity.Type type = AbstractMinecartEntity.Type.RIDEABLE;

				if(item == Items.FURNACE)
					type = AbstractMinecartEntity.Type.FURNACE;
				if(item == Items.CHEST)
					type = AbstractMinecartEntity.Type.CHEST;
				if(item == Items.TNT)
					type = AbstractMinecartEntity.Type.TNT;
				if(item == Items.HOPPER)
					type = AbstractMinecartEntity.Type.HOPPER;

				if(type != AbstractMinecartEntity.Type.RIDEABLE && world instanceof ServerWorld serverWorld) {
					AbstractMinecartEntity minecart = AbstractMinecartEntity.create(serverWorld, ridableCart.getX(), ridableCart.getY(), ridableCart.getZ(), type, stack, player);
					minecart.copyPositionAndRotation(ridableCart);
					world.spawnEntity(minecart);

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
						stack.decrement(1);

					return ActionResult.success(world.isClient());
				}
			}

			return ActionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if(entity instanceof AbstractMinecartEntity cart && MinecartTweaksConfig.canLinkMinecarts) {
				ItemStack stack = player.getStackInHand(hand);

				if(player.isSneaking() && stack.isOf(Items.CHAIN)) {
					if(world instanceof ServerWorld server) {
						UUID uuid = stack.get(PARENT_ID);

						if(uuid != null && !cart.getUuid().equals(uuid)) {
							if(server.getEntity(uuid) instanceof AbstractMinecartEntity parent) {
								Set<Linkable> train = new HashSet<>();
								train.add(parent);

								AbstractMinecartEntity nextParent;
								while((nextParent = parent.getLinkedParent()) != null && !train.contains(nextParent)) {
									train.add(nextParent);
								}

								if(train.contains(cart) || parent.getLinkedChild() != null) {
									player.sendMessage(Text.translatable(MinecartTweaks.MOD_ID + ".cant_link_to_engine").formatted(Formatting.RED), true);
								}
								else {
									if(cart.getLinkedParent() != null)
										Linkable.unsetParentChild(cart, cart.getLinkedParent());

									Linkable.setParentChild(parent, cart);
								}
							}
							else {
								stack.remove(PARENT_ID);
							}

							world.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.NEUTRAL, 1f, 1f);

							if(!player.isCreative())
								stack.decrement(1);

							stack.remove(PARENT_ID);
						}
						else {
							stack.set(PARENT_ID, cart.getUuid());
							world.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.NEUTRAL, 1f, 1f);
						}
					}

					return ActionResult.success(true);
				}
			}

			return ActionResult.PASS;
		});
	}

	public static Identifier id(String name) {
		return Identifier.of(MOD_ID, name);
	}
}
