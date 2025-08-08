package dev.cammiescorner.cammiesminecarttweaks.common.utils;

import com.google.common.collect.Maps;
import dev.cammiescorner.cammiesminecarttweaks.mixin.EntityShapeContextAccessor;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Map;
import java.util.Set;

import static net.minecraft.util.math.Direction.*;

public class MinecartHelper {
	public static final VoxelShape WALL_SHAPE = VoxelShapes.cuboid(0.48, 0.5, 0.48, 0.52, 1.2, 0.52);
	public static final Map<Set<VoxelShape>, VoxelShape> WALL_SHAPES_UNION = new Object2ReferenceOpenHashMap<>();
	public static final Map<Direction, VoxelShape> DIRECTION_2_SHAPE = Util.make(Maps.newEnumMap(Direction.class), map -> {
		map.put(EAST, getShapeForDirection(EAST));
		map.put(WEST, getShapeForDirection(WEST));
		map.put(NORTH, getShapeForDirection(NORTH));
		map.put(SOUTH, getShapeForDirection(SOUTH));
	});
	public static final Map<RailShape, Set<Direction>> DERAIL_FIX_WALLS = Util.make(Maps.newEnumMap(RailShape.class), map -> {
		map.put(RailShape.NORTH_WEST, new ObjectArraySet<>(new Direction[]{SOUTH, EAST}));
		map.put(RailShape.NORTH_EAST, new ObjectArraySet<>(new Direction[]{SOUTH, WEST}));
		map.put(RailShape.SOUTH_EAST, new ObjectArraySet<>(new Direction[]{NORTH, WEST}));
		map.put(RailShape.SOUTH_WEST, new ObjectArraySet<>(new Direction[]{NORTH, EAST}));

		map.put(RailShape.NORTH_SOUTH, new ObjectArraySet<>(new Direction[]{WEST, EAST}));
		map.put(RailShape.EAST_WEST, new ObjectArraySet<>(new Direction[]{NORTH, SOUTH}));
		map.put(RailShape.ASCENDING_EAST, new ObjectArraySet<>(new Direction[]{NORTH, SOUTH}));
		map.put(RailShape.ASCENDING_WEST, new ObjectArraySet<>(new Direction[]{NORTH, SOUTH}));
		map.put(RailShape.ASCENDING_NORTH, new ObjectArraySet<>(new Direction[]{WEST, EAST}));
		map.put(RailShape.ASCENDING_SOUTH, new ObjectArraySet<>(new Direction[]{WEST, EAST}));
	});


	private static VoxelShape getShapeForDirection(Direction direction) {
		return WALL_SHAPE.offset(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
	}

	private static VoxelShape getUnionShape(Set<VoxelShape> shapes) {
		VoxelShape totalShape = VoxelShapes.empty();
		for (VoxelShape shape : shapes) {
			totalShape = totalShape.isEmpty() ? shape : VoxelShapes.union(totalShape, shape);
		}
		return totalShape;
	}

	public static VoxelShape getCollisionShape(VoxelShape railCollisionShape, RailShape railShape, BlockPos pos, ShapeContext context) {
		if (context instanceof EntityShapeContextAccessor entityContext) {
			Entity entity = entityContext.getEntity();

			if(entity instanceof AbstractMinecartEntity cart) {
				if(cart.isSelfMovingOnRail()) {
					Set<Direction> derailFixWalls = DERAIL_FIX_WALLS.get(railShape);

					Set<VoxelShape> selectedWalls = new ObjectArraySet<>();
					Box offsetCartBox = cart.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ());
					for(Direction direction : derailFixWalls) {
						VoxelShape wallShape = DIRECTION_2_SHAPE.get(direction);
						Direction.Axis axis = direction.getAxis();
						Direction.AxisDirection axisDirection = direction.getDirection();
						double distanceToHitbox = axisDirection == Direction.AxisDirection.NEGATIVE ? offsetCartBox.getMin(axis) - (wallShape.getMax(axis)) : wallShape.getMin(axis) - offsetCartBox.getMax(axis);

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
}
