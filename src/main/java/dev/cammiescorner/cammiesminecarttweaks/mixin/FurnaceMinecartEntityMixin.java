package dev.cammiescorner.cammiesminecarttweaks.mixin;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Map;

@Mixin(FurnaceMinecartEntity.class)
public abstract class FurnaceMinecartEntityMixin extends AbstractMinecartEntity {
	@Shadow public abstract boolean isLit();
	@Shadow private int fuel;
	@Shadow public double pushX;
	@Shadow public double pushZ;
	@Shadow @Final @Mutable private static Ingredient ACCEPTABLE_FUEL;
	@Shadow public abstract ActionResult interact(PlayerEntity player, Hand hand);

	@Unique private static final Ingredient OLD_ACCEPTABLE_FUEL = ACCEPTABLE_FUEL;
	@Unique private ChunkPos prevChunkPos;

	protected FurnaceMinecartEntityMixin(EntityType<?> entityType, World world) { super(entityType, world); }

	@Inject(method = "<init>(Lnet/minecraft/world/World;DDD)V", at = @At("TAIL"))
	public void minecarttweaks$initPrevChunPos(World world, double x, double y, double z, CallbackInfo info) {
		prevChunkPos = getChunkPos();
	}

	@Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
	public void minecarttweaks$increaseSpeed(CallbackInfoReturnable<Double> info) {
		if(isLit())
			info.setReturnValue(MinecartTweaksConfig.getFurnaceMinecartSpeed());
		else
			info.setReturnValue(super.getMaxSpeed());
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void minecarttweaks$loadChunks(CallbackInfo info) {
		if(MinecartTweaksConfig.furnaceMinecartsLoadChunks && this.getWorld() instanceof ServerWorld server) {
			ChunkPos currentChunkPos = ChunkSectionPos.from(this).toChunkPos();

			if(fuel > 0)
				server.getChunkManager().addTicket(ChunkTicketType.PLAYER, currentChunkPos, 3, getChunkPos());
			if(!currentChunkPos.equals(prevChunkPos) || fuel <= 0)
				server.getChunkManager().removeTicket(ChunkTicketType.PLAYER, prevChunkPos, 3, getChunkPos());

			prevChunkPos = currentChunkPos;
		}
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	public void minecarttweaks$addOtherFuels(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> info) {
		if(MinecartTweaksConfig.furnacesCanUseAllFuels) {
			ItemStack stack = player.getStackInHand(hand);
			Map<Item, Integer> fuels = AbstractFurnaceBlockEntity.createFuelTimeMap();

			if(!stack.isEmpty() && fuels.containsKey(stack.getItem())) {
				int fuelTime = fuels.getOrDefault(stack.getItem(), 0);

				if(!player.isCreative() && fuelTime > 0) {
					if(stack.getItem() instanceof BucketItem)
						player.getInventory().setStack(player.getInventory().selectedSlot, BucketItem.getEmptiedStack(stack, player));
					else
						stack.decrement(1);
				}

				if(stack.getItem() instanceof BucketItem) {
					SoundEvent soundEvent = SoundEvents.ITEM_BUCKET_EMPTY_LAVA;
					this.getWorld().playSound(player, player.getBlockPos(), soundEvent, SoundCategory.BLOCKS, 1.0f, 1.0f);
				}

				fuel = (int) Math.min(MinecartTweaksConfig.furnaceMaxBurnTime, fuel + (fuelTime * 2.25));
			}

			if(fuel > 0) {
				pushX = getX() - player.getX();
				pushZ = getZ() - player.getZ();
			}

			ACCEPTABLE_FUEL = Ingredient.empty();
			info.setReturnValue(ActionResult.success(this.getWorld().isClient));
		}
		else {
			ACCEPTABLE_FUEL = OLD_ACCEPTABLE_FUEL;
		}
	}

	@ModifyConstant(method = "interact", constant = @Constant(intValue = 32000))
	public int minecarttweaks$maxBurnTime(int maxBurnTime) {
		return MinecartTweaksConfig.furnaceMaxBurnTime;
	}

	@ModifyArgs(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"
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
			target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I"
	))
	public int minecarttweaks$removeRandom(int i) {
		return 1;
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	public void minecarttweaks$readNbt(NbtCompound nbt, CallbackInfo info) {
		fuel = nbt.getInt("RealFuel");
		prevChunkPos = new ChunkPos(nbt.getLong("PrevChunkPos"));
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	public void minecarttweaks$writeNbt(NbtCompound nbt, CallbackInfo info) {
		if(fuel > Short.MAX_VALUE)
			nbt.putShort("Fuel", Short.MAX_VALUE);

		nbt.putInt("RealFuel", fuel);
		nbt.putLong("PrevChunkPos", prevChunkPos.toLong());
	}
}
