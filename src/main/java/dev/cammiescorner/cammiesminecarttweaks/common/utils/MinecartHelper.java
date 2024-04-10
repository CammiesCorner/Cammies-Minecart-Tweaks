package dev.cammiescorner.cammiesminecarttweaks.common.utils;

import com.mojang.datafixers.util.Pair;
import dev.cammiescorner.cammiesminecarttweaks.common.blocks.CrossedRailBlock;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MinecartHelper {
	public static boolean shouldSlowDown(AbstractMinecartEntity minecart, World world) {
		boolean slowEm = false;

		if(minecart != null) {
			int velocity = MathHelper.ceil(minecart.getVelocity().horizontalLength());
			Direction direction = Direction.getFacing(minecart.getVelocity().getX(), 0, minecart.getVelocity().getZ());
			BlockPos minecartPos = minecart.getBlockPos();
			Vec3i pain = new Vec3i(minecartPos.getX(), 0, minecartPos.getZ());
			BlockPos.Mutable pos = new BlockPos.Mutable();
			List<Vec3i> poses = new ArrayList<>();

			poses.add(minecartPos);

			for(int i = 0; i < poses.size(); i++) {
				pos.set(poses.get(i));
				int distance = pain.getManhattanDistance(new Vec3i(pos.getX(), 0, pos.getZ()));

				if(distance > velocity)
					break;

				if(world.getBlockState(pos.down()).isIn(BlockTags.RAILS))
					pos.move(0, -1, 0);

				BlockState state = world.getBlockState(pos);

				if(state.isIn(BlockTags.RAILS) && state.getBlock() instanceof AbstractRailBlock rails) {
					RailShape shape = state.get(rails.getShapeProperty());

					if(rails instanceof CrossedRailBlock && minecart.getVelocity().horizontalLength() > 0) {
						if(shape == RailShape.NORTH_SOUTH && (direction == Direction.EAST || direction == Direction.WEST)) {
							world.setBlockState(pos, state.with(rails.getShapeProperty(), RailShape.EAST_WEST));
							break;
						}

						if(shape == RailShape.EAST_WEST && (direction == Direction.NORTH || direction == Direction.SOUTH)) {
							world.setBlockState(pos, state.with(rails.getShapeProperty(), RailShape.NORTH_SOUTH));
							break;
						}
					}

					if((shape != RailShape.NORTH_SOUTH && shape != RailShape.EAST_WEST)) {
						slowEm = true;
						break;
					}

					Pair<Vec3i, Vec3i> pair = AbstractMinecartEntity.getAdjacentRailPositionsByShape(shape);
					Vec3i first = pair.getFirst().add(pos);
					Vec3i second = pair.getSecond().add(pos);

					if(distance < 2) {
						if(!poses.contains(first))
							poses.add(first);
						if(!poses.contains(second))
							poses.add(second);

						continue;
					}

					if((shape == RailShape.NORTH_SOUTH && direction == Direction.NORTH) || (shape == RailShape.EAST_WEST && direction == Direction.WEST)) {
						if(!poses.contains(first))
							poses.add(first);
					}
					else {
						if(!poses.contains(second))
							poses.add(second);
					}
				}
			}
		}

		return slowEm;
	}
}
