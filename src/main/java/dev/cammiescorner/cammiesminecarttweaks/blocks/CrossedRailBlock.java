package dev.cammiescorner.cammiesminecarttweaks.blocks;

import com.mojang.serialization.MapCodec;
import dev.upcraft.sparkweave.api.registry.block.BlockItemProvider;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class CrossedRailBlock extends BaseRailBlock implements BlockItemProvider {
	public static final EnumProperty<RailShape> SHAPE = EnumProperty.create("shape", RailShape.class, shape -> shape != RailShape.ASCENDING_NORTH && shape != RailShape.ASCENDING_EAST && shape != RailShape.ASCENDING_SOUTH && shape != RailShape.ASCENDING_WEST && shape != RailShape.NORTH_EAST && shape != RailShape.NORTH_WEST && shape != RailShape.SOUTH_EAST && shape != RailShape.SOUTH_WEST);
	public static final MapCodec<CrossedRailBlock> CODEC = simpleCodec(CrossedRailBlock::new);

	public CrossedRailBlock(BlockBehaviour.Properties properties) {
		super(true, properties);
		registerDefaultState(defaultBlockState().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, false));
	}

	@Override
	protected MapCodec<? extends BaseRailBlock> codec() {
		return CODEC;
	}

	@Override
	public Property<RailShape> getShapeProperty() {
		return SHAPE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SHAPE, WATERLOGGED);
	}
}
