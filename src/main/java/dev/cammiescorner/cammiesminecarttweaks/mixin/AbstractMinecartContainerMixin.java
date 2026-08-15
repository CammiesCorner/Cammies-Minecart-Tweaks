package dev.cammiescorner.cammiesminecarttweaks.mixin;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecartContainer.class)
public abstract class AbstractMinecartContainerMixin {
	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	public void minecarttweaks$heckUMojang(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
		System.out.println("Interact, Client: " + player.level().isClientSide());
		if(MinecartTweaksConfig.canLinkMinecarts && player.isShiftKeyDown()) {
			ItemStack stack = player.getItemInHand(hand);

//			if(MinecartHelper.tryLinkMinecart(player, )) {
//				info.setReturnValue(InteractionResult.sidedSuccess(player.level().isClientSide()));
//			}
		}
	}
}
