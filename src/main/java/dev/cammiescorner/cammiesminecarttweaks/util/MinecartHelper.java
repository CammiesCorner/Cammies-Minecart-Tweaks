package dev.cammiescorner.cammiesminecarttweaks.util;

import com.google.common.collect.Maps;
import dev.cammiescorner.cammiesminecarttweaks.api.InWorldMinecartCraftingEvent;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.init.MTDataComponents;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class MinecartHelper {
	public static final VoxelShape WALL_SHAPE = Shapes.box(0.48, 0.5, 0.48, 0.52, 1.2, 0.52);
	public static final Map<Set<VoxelShape>, VoxelShape> WALL_SHAPES_UNION = new Object2ReferenceOpenHashMap<>();
	public static final Map<Direction, VoxelShape> DIRECTION_2_SHAPE = Util.make(Maps.newEnumMap(Direction.class), map -> Direction.Plane.HORIZONTAL.forEach(direction -> map.put(direction, getShapeForDirection(direction))));
	public static final Map<RailShape, Set<Direction>> DERAIL_FIX_WALLS = Util.make(Maps.newEnumMap(RailShape.class), map -> {
		map.put(RailShape.NORTH_WEST, new ObjectArraySet<>(new Direction[]{Direction.SOUTH, Direction.EAST}));
		map.put(RailShape.NORTH_EAST, new ObjectArraySet<>(new Direction[]{Direction.SOUTH, Direction.WEST}));
		map.put(RailShape.SOUTH_EAST, new ObjectArraySet<>(new Direction[]{Direction.NORTH, Direction.WEST}));
		map.put(RailShape.SOUTH_WEST, new ObjectArraySet<>(new Direction[]{Direction.NORTH, Direction.EAST}));

		map.put(RailShape.NORTH_SOUTH, new ObjectArraySet<>(new Direction[]{Direction.WEST, Direction.EAST}));
		map.put(RailShape.EAST_WEST, new ObjectArraySet<>(new Direction[]{Direction.NORTH, Direction.SOUTH}));
		map.put(RailShape.ASCENDING_EAST, new ObjectArraySet<>(new Direction[]{Direction.NORTH, Direction.SOUTH}));
		map.put(RailShape.ASCENDING_WEST, new ObjectArraySet<>(new Direction[]{Direction.NORTH, Direction.SOUTH}));
		map.put(RailShape.ASCENDING_NORTH, new ObjectArraySet<>(new Direction[]{Direction.WEST, Direction.EAST}));
		map.put(RailShape.ASCENDING_SOUTH, new ObjectArraySet<>(new Direction[]{Direction.WEST, Direction.EAST}));
	});


	private static VoxelShape getShapeForDirection(Direction direction) {
		return WALL_SHAPE.move(direction.getStepX(), direction.getStepY(), direction.getStepZ());
	}

	private static VoxelShape getUnionShape(Set<VoxelShape> shapes) {
		VoxelShape totalShape = Shapes.empty();
		for (VoxelShape shape : shapes) {
			totalShape = totalShape.isEmpty() ? shape : Shapes.or(totalShape, shape);
		}
		return totalShape;
	}

	public static VoxelShape getCollisionShape(VoxelShape railCollisionShape, RailShape railShape, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext) {
			Entity entity = entityContext.getEntity();

			if(entity instanceof AbstractMinecart cart) {
				if(cart.isSelfMovingOnRail()) {
					Set<Direction> derailFixWalls = DERAIL_FIX_WALLS.get(railShape);

					Set<VoxelShape> selectedWalls = new ObjectArraySet<>();
					var offsetCartBox = cart.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ());
					for(Direction direction : derailFixWalls) {
						VoxelShape wallShape = DIRECTION_2_SHAPE.get(direction);
						Direction.Axis axis = direction.getAxis();
						Direction.AxisDirection axisDirection = direction.getAxisDirection();
						double distanceToHitbox = axisDirection == Direction.AxisDirection.NEGATIVE ? offsetCartBox.min(axis) - (wallShape.max(axis)) : wallShape.min(axis) - offsetCartBox.max(axis);

						if(distanceToHitbox > 0)
							selectedWalls.add(wallShape);
					}

					if(!selectedWalls.isEmpty()) {
						if(!railCollisionShape.isEmpty())
							selectedWalls.add(railCollisionShape);

						return WALL_SHAPES_UNION.computeIfAbsent(selectedWalls, MinecartHelper::getUnionShape);
					}
				}
			}
		}

		return railCollisionShape;
	}

	public static boolean tryUpgradeMinecart(ServerPlayer player, ServerLevel level, InteractionHand hand, AbstractMinecart originalMinecart, ItemStack heldItem) {
		if(!originalMinecart.isAlive()) {
			return false;
		}

		var originalStack = originalMinecart.getPickResult();
		var craftingInput = CraftingInput.of(2, 1, List.of(heldItem, originalStack));

		var resultStack = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput, level).map(recipe -> recipe.value().assemble(craftingInput, level.registryAccess())).orElse(ItemStack.EMPTY);

		Entity entity;
		if(resultStack.getItem() instanceof MinecartItem minecartItem) {
			entity = AbstractMinecart.createMinecart(level, originalMinecart.getX(), originalMinecart.getY(), originalMinecart.getZ(), minecartItem.minecarttweaks$getType(), resultStack, player);
		} else {
			entity = null;
		}

		var ctx = new InWorldMinecartCraftingEvent.Context() {
			private Entity resultEntity = entity;

			@Override
			public ServerPlayer getPlayer() {
				return player;
			}

			@Override
			public InteractionHand getHand() {
				return hand;
			}

			@Override
			public ItemStack getItemStack() {
				return heldItem;
			}

			@Override
			public ServerLevel getLevel() {
				return level;
			}

			@Override
			public AbstractMinecart getOriginalEntity() {
				return originalMinecart;
			}

			@Override
			public @Nullable Entity getResultEntity() {
				return resultEntity;
			}

			@Override
			public void setResultEntity(@Nullable Entity entity) {
				resultEntity = entity;
			}
		};
		if(InWorldMinecartCraftingEvent.EVENT.invoker().tryUpgradeMinecart(ctx)) {
			var resultEntity = ctx.getResultEntity();
			if(resultEntity == null) {
				return false;
			}

			if(originalMinecart.isVehicle()) {
				originalMinecart.ejectPassengers();
			}

			resultEntity.copyPosition(originalMinecart);

			var parent = originalMinecart.getLinkedParent();
			var child = originalMinecart.getLinkedChild();

			if(parent != null) {
				Linkable.unsetParentChild(parent, originalMinecart);
			}
			if(child != null) {
				Linkable.unsetParentChild(originalMinecart, child);
			}

			originalMinecart.remove(Entity.RemovalReason.DISCARDED);
			level.addFreshEntity(resultEntity);

			if(resultEntity instanceof Linkable linkableEntity) {
				if(parent != null) {
					Linkable.setParentChild(parent, linkableEntity);
				}
				if(child != null) {
					Linkable.setParentChild(linkableEntity, child);
				}
			}
			else {
				// not linkable, drop connection chains

				var count = 0;
				if(parent != null) {
					count++;
				}
				if(child != null) {
					count++;
				}

				var stack = new ItemStack(Items.CHAIN, count);
				resultEntity.spawnAtLocation(stack, resultEntity.getBbHeight());
			}

			if(!player.isCreative()) {
				heldItem.shrink(1);
			}
			player.setItemInHand(hand, heldItem);
			return true;
		}

		return false;
	}

	public static boolean tryLinkMinecart(Player player, ServerLevel serverLevel, InteractionHand hand, AbstractMinecart minecart, ItemStack heldItem) {
		if (!heldItem.is(Items.CHAIN)) {
			return false;
		}

		UUID uuid = heldItem.get(MTDataComponents.PARENT_ID.get());
		if(uuid != null) {
			heldItem.remove(MTDataComponents.PARENT_ID.get());

			if (!minecart.getUUID().equals(uuid) && serverLevel.getEntity(uuid) instanceof Linkable parent) {
				if (parent.getLinkedChild() != null) {
					// TODO better error message: parent cart already has a linked child
					player.displayClientMessage(Component.translatable("minecarttweaks.cant_link_to_engine").withStyle(ChatFormatting.RED), true);
					minecart.playSound(SoundEvents.CHAIN_BREAK);
					player.setItemInHand(hand, heldItem);
					return true;
				}

				// check for circles; we need only check the parents since the source cart already cannot have a child
				Set<Linkable> train = new HashSet<>();
				train.add(minecart);
				train.add(parent);

				Linkable tmp;
				while ((tmp = parent.getLinkedParent()) != null) {
					if (!train.add(tmp)) {
						// TODO better error message: cart already in train
						player.displayClientMessage(Component.translatable("minecarttweaks.cant_link_to_engine").withStyle(ChatFormatting.RED), true);
						minecart.playSound(SoundEvents.CHAIN_BREAK);
						player.setItemInHand(hand, heldItem);
						return true;
					}
				}

				Linkable oldParent = minecart.getLinkedParent();
				if (oldParent != null) {
					Linkable.unsetParentChild(oldParent, minecart);
				}

				Linkable.setParentChild(parent, minecart);

				if (!player.isCreative()) {
					heldItem.shrink(1);
				}
				minecart.playSound(SoundEvents.CHAIN_PLACE);
			} else {
				minecart.playSound(SoundEvents.CHAIN_BREAK);
			}
		}

		return true;
	}

	public static boolean shouldApplyBrakes(AbstractMinecart minecart, Level level, BlockPos pos, BlockState blockState) {
		return blockState.is(Blocks.POWERED_RAIL) && !blockState.getValue(PoweredRailBlock.POWERED);
	}
}
