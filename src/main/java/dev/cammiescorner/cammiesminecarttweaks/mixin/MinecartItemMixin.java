package dev.cammiescorner.cammiesminecarttweaks.mixin;

import dev.cammiescorner.cammiesminecarttweaks.util.ext.MinecartItemExt;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MinecartItem.class)
public abstract class MinecartItemMixin implements MinecartItemExt {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/Item;<init>(Lnet/minecraft/world/item/Item$Properties;)V"
	))
	private static Item.Properties minecarttweaks$increaseStackSize(Item.Properties properties) {
		return properties.stacksTo(16);
	}

	@Accessor("type")
	@Override
	public abstract AbstractMinecart.Type minecarttweaks$getType();
}
