package dev.cammiescorner.cammiesminecarttweaks.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MinecartItem.class)
public class MinecartItemMixin {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/Item;<init>(Lnet/minecraft/world/item/Item$Properties;)V"
	))
	private static Item.Properties minecarttweaks$increaseStackSize(Item.Properties properties) {
		return properties.stacksTo(16);
	}
}
