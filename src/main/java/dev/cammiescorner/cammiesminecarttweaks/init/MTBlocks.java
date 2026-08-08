package dev.cammiescorner.cammiesminecarttweaks.init;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.blocks.CrossedRailBlock;
import dev.upcraft.sparkweave.api.registry.RegistryHandler;
import dev.upcraft.sparkweave.api.registry.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class MTBlocks {
	public static final RegistryHandler<Block> BLOCKS = RegistryHandler.create(Registries.BLOCK, MinecartTweaks.MOD_ID);

	public static final RegistrySupplier<CrossedRailBlock> CROSSED_RAIL = BLOCKS.register("crossed_rail", () -> new CrossedRailBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RAIL)));
}
