package dev.cammiescorner.cammiesminecarttweaks.mixin;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Map;

@Mixin(MinecartFurnace.class)
public abstract class MinecartFurnaceMixin extends AbstractMinecart {
	@Shadow private int fuel;
	@Shadow @Final @Mutable private static Ingredient INGREDIENT;

	@Shadow
	protected abstract boolean hasFuel();

	@Shadow
	public double xPush;
	@Shadow
	public double zPush;
	@Unique private static final Ingredient OLD_ACCEPTABLE_FUEL = INGREDIENT;
	@Unique private ChunkPos prevChunkPos;

	protected MinecartFurnaceMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
		throw new UnsupportedOperationException();
	}

	@Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDD)V", at = @At("TAIL"))
	public void minecarttweaks$initPrevChunPos(Level level, double x, double y, double z, CallbackInfo ci) {
		prevChunkPos = chunkPosition();
	}

	@Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
	public void minecarttweaks$increaseSpeed(CallbackInfoReturnable<Double> info) {
		if(hasFuel())
			info.setReturnValue(MinecartTweaksConfig.getFurnaceMinecartSpeed());
		else
			info.setReturnValue(super.getMaxSpeed());
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void minecarttweaks$loadChunks(CallbackInfo info) {
		if(MinecartTweaksConfig.furnaceMinecartsLoadChunks && this.level() instanceof ServerLevel serverLevel) {
			ChunkPos currentChunkPos = SectionPos.of(this).chunk();

			if(fuel > 0)
				serverLevel.getChunkSource().addRegionTicket(TicketType.PLAYER, currentChunkPos, 3, chunkPosition());
			if(!currentChunkPos.equals(prevChunkPos) || fuel <= 0)
				serverLevel.getChunkSource().removeRegionTicket(TicketType.PLAYER, prevChunkPos, 3, chunkPosition());

			prevChunkPos = currentChunkPos;
		}
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	public void minecarttweaks$addOtherFuels(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
		if(MinecartTweaksConfig.furnacesCanUseAllFuels) {
			ItemStack stack = player.getItemInHand(hand);
			Map<Item, Integer> fuels = AbstractFurnaceBlockEntity.getFuel();

			if(!stack.isEmpty() && fuels.containsKey(stack.getItem())) {
				int fuelTime = fuels.getOrDefault(stack.getItem(), 0);

				if(!player.isCreative() && fuelTime > 0) {
					if(stack.getItem() instanceof BucketItem) {
						player.getInventory().setItem(player.getInventory().selected, BucketItem.getEmptySuccessItem(stack, player));
					} else {
						stack.shrink(1);
					}
				}

				if(stack.getItem() instanceof BucketItem bucketItem) {
					bucketItem.playEmptySound(player, level(), blockPosition());
				}

				fuel = (int) Math.min(MinecartTweaksConfig.furnaceMaxBurnTime, fuel + (fuelTime * 2.25));
			}

			if(fuel > 0) {
				xPush = getX() - player.getX();
				zPush = getZ() - player.getZ();
			}

			INGREDIENT = Ingredient.of();
			info.setReturnValue(InteractionResult.sidedSuccess(this.level().isClientSide()));
		}
		else {
			INGREDIENT = OLD_ACCEPTABLE_FUEL;
		}
	}

	@ModifyConstant(method = "interact", constant = @Constant(intValue = 32000))
	public int minecarttweaks$maxBurnTime(int maxBurnTime) {
		return MinecartTweaksConfig.furnaceMaxBurnTime;
	}

	@ModifyArgs(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
	))
	public void minecarttweaks$changeSmokeParticle(Args args) {
		if(MinecartTweaksConfig.useCampfireSmoke)
			args.set(0, ParticleTypes.CAMPFIRE_COSY_SMOKE);

		args.set(1, getX() + (random.nextFloat() - 0.5));
		args.set(2, getY() + 1);
		args.set(3, getZ() + (random.nextFloat() - 0.5));
		args.set(5, 0.2);
	}

	@ModifyArg(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"
	))
	public int minecarttweaks$removeRandom(int i) {
		return 1;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	public void minecarttweaks$readNbt(CompoundTag nbt, CallbackInfo info) {
		fuel = nbt.getInt("RealFuel");
		prevChunkPos = new ChunkPos(nbt.getLong("PrevChunkPos"));
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	public void minecarttweaks$writeNbt(CompoundTag nbt, CallbackInfo info) {
		if(fuel > Short.MAX_VALUE)
			nbt.putShort("Fuel", Short.MAX_VALUE);

		nbt.putInt("RealFuel", fuel);
		nbt.putLong("PrevChunkPos", prevChunkPos.toLong());
	}
}
