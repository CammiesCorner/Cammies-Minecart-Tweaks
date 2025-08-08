package dev.cammiescorner.cammiesminecarttweaks.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;

public class CrossedRailBlock extends AbstractRailBlock {
	public static final EnumProperty<RailShape> SHAPE = EnumProperty.of("shape", RailShape.class, shape -> shape != RailShape.ASCENDING_NORTH && shape != RailShape.ASCENDING_EAST && shape != RailShape.ASCENDING_SOUTH && shape != RailShape.ASCENDING_WEST && shape != RailShape.NORTH_EAST && shape != RailShape.NORTH_WEST && shape != RailShape.SOUTH_EAST && shape != RailShape.SOUTH_WEST);
	public static final MapCodec<CrossedRailBlock> CODEC = createCodec(CrossedRailBlock::new);

	public CrossedRailBlock(Settings settings) {
		super(true, settings);
		setDefaultState(getDefaultState().with(SHAPE, RailShape.NORTH_SOUTH).with(WATERLOGGED, false));
	}

	@Override
	protected MapCodec<? extends AbstractRailBlock> getCodec() {
		return CODEC;
	}

	@Override
	public Property<RailShape> getShapeProperty() {
		return SHAPE;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(SHAPE, WATERLOGGED);
	}
}
