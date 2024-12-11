package dev.cammiescorner.cammiesminecarttweaks.mixin;

import dev.cammiescorner.cammiesminecarttweaks.common.utils.MinecartHelper;
import net.minecraft.block.*;
import net.minecraft.block.enums.RailShape;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractRailBlock.class)
public abstract class AbstractRailBlockMixin extends Block {
	@Shadow public abstract Property<RailShape> getShapeProperty();

	public AbstractRailBlockMixin(AbstractBlock.Settings settings) { super(settings); }

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		VoxelShape railCollisionShape = super.getCollisionShape(state, world, pos, context);
		return MinecartHelper.getCollisionShape(railCollisionShape, state.get(this.getShapeProperty()), pos, context);
	}
}
