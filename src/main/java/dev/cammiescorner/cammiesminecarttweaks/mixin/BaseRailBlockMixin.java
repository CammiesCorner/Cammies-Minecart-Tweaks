package dev.cammiescorner.cammiesminecarttweaks.mixin;

import dev.cammiescorner.cammiesminecarttweaks.util.MinecartHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BaseRailBlock.class)
public abstract class BaseRailBlockMixin extends Block {
	@Shadow public abstract Property<RailShape> getShapeProperty();

	public BaseRailBlockMixin(BlockBehaviour.Properties properties) {
		super(properties);
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape railCollisionShape = super.getCollisionShape(state, level, pos, context);
		return MinecartHelper.getCollisionShape(railCollisionShape, state.getValue(this.getShapeProperty()), pos, context);
	}
}
