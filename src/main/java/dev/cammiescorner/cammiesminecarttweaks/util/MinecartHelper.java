package dev.cammiescorner.cammiesminecarttweaks.util;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.Set;

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
}
