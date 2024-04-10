package dev.cammiescorner.cammiesminecarttweaks;

import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.common.blocks.CrossedRailBlock;
import dev.cammiescorner.cammiesminecarttweaks.common.compat.MinecartTweaksConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Holder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

import java.util.HashSet;
import java.util.Set;

public class MinecartTweaks implements ModInitializer {
	public static final String MOD_ID = "minecarttweaks";
	public static final Block CROSSED_RAIL = new CrossedRailBlock();

	@Override
	public void onInitialize(ModContainer mod) {
		MidnightConfig.init(MinecartTweaks.MOD_ID, MinecartTweaksConfig.class);

		Registry.register(Registries.BLOCK, id("crossed_rail"), CROSSED_RAIL);
		Registry.register(Registries.ITEM, id("crossed_rail"), new BlockItem(CROSSED_RAIL, new Item.Settings()));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE_BLOCKS).register(entries -> entries.addItem(CROSSED_RAIL));

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if(entity instanceof MinecartEntity ridableCart && ridableCart.getMinecartType() == AbstractMinecartEntity.Type.RIDEABLE) {
				AbstractMinecartEntity parent = ((Linkable) ridableCart).getLinkedParent();
				AbstractMinecartEntity child = ((Linkable) ridableCart).getLinkedChild();
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

				if(type != AbstractMinecartEntity.Type.RIDEABLE) {
					AbstractMinecartEntity minecart = AbstractMinecartEntity.create(world, ridableCart.getX(), ridableCart.getY(), ridableCart.getZ(), type);
					world.spawnEntity(minecart);

					if(parent != null) {
						Linkable.unsetParentChild((Linkable) parent, (Linkable) ridableCart);
						Linkable.setParentChild((Linkable) parent, (Linkable) minecart);
					}
					if(child != null) {
						Linkable.unsetParentChild((Linkable) ridableCart, (Linkable) child);
						Linkable.setParentChild((Linkable) minecart, (Linkable) child);
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
						NbtCompound nbt = stack.getOrCreateNbt();

						if(nbt.contains("ParentEntity") && !cart.getUuid().equals(nbt.getUuid("ParentEntity"))) {
							if(server.getEntity(nbt.getUuid("ParentEntity")) instanceof AbstractMinecartEntity parent) {
								Set<Linkable> train = new HashSet<>();
								train.add((Linkable) parent);

								AbstractMinecartEntity nextParent;
								while((nextParent = ((Linkable) parent).getLinkedParent()) instanceof Linkable && !train.contains(nextParent)) {
									train.add((Linkable) nextParent);
								}

								if(train.contains(cart) || ((Linkable) parent).getLinkedChild() != null) {
									player.sendMessage(Text.translatable(MinecartTweaks.MOD_ID + ".cant_link_to_engine").formatted(Formatting.RED), true);
								}
								else {
									if(((Linkable) cart).getLinkedParent() != null) {
										Linkable.unsetParentChild((Linkable) cart, (Linkable) ((Linkable) cart).getLinkedParent());
									}

									Linkable.setParentChild((Linkable) parent, (Linkable) cart);
								}
							}
							else {
								nbt.remove("ParentEntity");

								if(nbt.isEmpty())
									stack.setNbt(null);
							}

							world.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.NEUTRAL, 1F, 1F);

							if(!player.isCreative())
								stack.decrement(1);

							nbt.remove("ParentEntity");

							if(nbt.isEmpty())
								stack.setNbt(null);
						}
						else {
							nbt.putUuid("ParentEntity", cart.getUuid());
							world.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.NEUTRAL, 1F, 1F);
						}
					}

					return ActionResult.success(true);
				}
			}

			return ActionResult.PASS;
		});
	}

	public static Identifier id(String name) {
		return new Identifier(MOD_ID, name);
	}

	public static DamageSource minecart(Entity entity) {
		return new DamageSource(new Holder.Direct<>(new DamageType(MOD_ID + ".minecart", 1)), entity);
	}
}
