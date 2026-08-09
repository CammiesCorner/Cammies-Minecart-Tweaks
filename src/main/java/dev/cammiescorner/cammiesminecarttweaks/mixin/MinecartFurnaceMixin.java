package dev.cammiescorner.cammiesminecarttweaks.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.logging.LogUtils;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.cca.component.PoweredMinecartData;
import dev.cammiescorner.cammiesminecarttweaks.util.MinecartHelper;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MinecartFurnace.class)
public abstract class MinecartFurnaceMixin extends AbstractMinecart {

	@Unique
	private static final Logger LOGGER = LogUtils.getLogger();

	@Shadow private int fuel;

	@Shadow
	public abstract boolean hasFuel();

	@Shadow
	public double xPush;
	@Shadow
	public double zPush;
	@Unique private ChunkPos prevChunkPos;

	protected MinecartFurnaceMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
		throw new UnsupportedOperationException();
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void construct(CallbackInfo ci) {
		prevChunkPos = chunkPosition();
	}

	@Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
	private void minecarttweaks$increaseSpeed(CallbackInfoReturnable<Double> info) {
		if(hasFuel())
			info.setReturnValue(MinecartTweaksConfig.getFurnaceMinecartSpeed());
		else
			info.setReturnValue(super.getMaxSpeed());
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void minecarttweaks$loadChunks(CallbackInfo info) {
		if(MinecartTweaksConfig.furnaceMinecartsLoadChunks && this.level() instanceof ServerLevel serverLevel) {
			ChunkPos currentChunkPos = SectionPos.of(this).chunk();

			if(hasFuel())
				serverLevel.getChunkSource().addRegionTicket(TicketType.PLAYER, currentChunkPos, 3, chunkPosition());
			if(!currentChunkPos.equals(prevChunkPos) || !hasFuel())
				serverLevel.getChunkSource().removeRegionTicket(TicketType.PLAYER, prevChunkPos, 3, chunkPosition());

			prevChunkPos = currentChunkPos;
		}
	}

	@WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Ingredient;test(Lnet/minecraft/world/item/ItemStack;)Z"))
	public boolean minecarttweaks$addOtherFuels(Ingredient instance, ItemStack stack, Operation<Boolean> original, Player player, InteractionHand hand) {
		if(!stack.isEmpty() && MinecartTweaksConfig.furnacesCanUseAllFuels) {
			@Nullable Integer fuelTime = FuelRegistry.INSTANCE.get(stack.getItem());
			int totalFuelTime = fuelTime != null && fuelTime > 0 ? (int) (fuelTime * 2.25) : 0;

			if(totalFuelTime > 0 && fuel + totalFuelTime < MinecartTweaksConfig.furnaceMaxBurnTime) {
				if(stack.getItem() instanceof BucketItem bucketItem) {
					bucketItem.playEmptySound(player, level(), blockPosition());
					if(!player.hasInfiniteMaterials()) {
						player.getInventory().setItem(player.getInventory().selected, BucketItem.getEmptySuccessItem(stack, player));
					}
				} else {
					stack.consume(1, player);
				}

				fuel += totalFuelTime;
			}
		}

		return original.call(instance, stack);
	}

	@Inject(method = "interact", at = @At("HEAD"))
	private void interactStart(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Share("comp") LocalRef<PoweredMinecartData> compShare) {
		compShare.set(PoweredMinecartData.of(this));
	}

	@WrapOperation(method = "interact", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/vehicle/MinecartFurnace;xPush:D"))
	private void setXPush(MinecartFurnace instance, double value, Operation<Void> original, @Share("comp") LocalRef<PoweredMinecartData> compShare) {
		var comp = compShare.get();
		if(comp.isParking()) {
			comp.storeImpulse(value, comp.getStoredImpulseZ());
			xPush = 0.0D;
			return;
		}

		original.call(instance, value);
	}

	@WrapOperation(method = "interact", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/vehicle/MinecartFurnace;zPush:D"))
	private void setZPush(MinecartFurnace instance, double value, Operation<Void> original, @Share("comp") LocalRef<PoweredMinecartData> compShare) {
		var comp = compShare.get();
		if(comp.isParking()) {
			comp.storeImpulse(comp.getStoredImpulseX(), value);
			zPush = 0.0D;
			comp.sync();
			return;
		}

		original.call(instance, value);
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

	@Inject(method = "moveAlongTrack", at = @At("HEAD"))
	private void checkParking(CallbackInfo ci) {
		var comp = PoweredMinecartData.of(this);

		var wasParked = comp.isParking();

		int x = Mth.floor(this.getX());
		int y = Mth.floor(this.getY());
		int z = Mth.floor(this.getZ());
		var pos = new BlockPos(x, y, z);
		var below = pos.below();
		BlockState blockState = level().getBlockState(below);
		if (blockState.is(BlockTags.RAILS)) {
			pos = below;
		} else {
			blockState = level().getBlockState(pos);
		}
		comp.setParking(MinecartHelper.shouldApplyBrakes(this, level(), pos, blockState));

		if(wasParked != comp.isParking()) {
			LOGGER.info("OLD: {}, NEW: {}", wasParked, comp.isParking());
			if(comp.isParking()) {
				comp.storeImpulse(xPush, zPush);
				xPush = 0.0D;
				zPush = 0.0D;
			}
			else if(hasFuel()) {
				xPush = comp.getStoredImpulseX();
				zPush = comp.getStoredImpulseZ();
			} else {
				xPush = 0.0D;
				zPush = 0.0D;
				comp.storeImpulse(0.0D, 0.0D);
			}
			LOGGER.info(" x: {},  z: {}", xPush, zPush);
			LOGGER.info("sx: {}, sz: {}", comp.getStoredImpulseX(), comp.getStoredImpulseZ());

			this.hasImpulse = true;
			comp.sync();
		}
	}

	@Definition(id = "fuel", field = "Lnet/minecraft/world/entity/vehicle/MinecartFurnace;fuel:I")
	@Expression("this.fuel > 0")
	@ModifyExpressionValue(method = "tick", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 0))
	private boolean reduceParkingFuelUsage(boolean original) {
		if(original && PoweredMinecartData.of(this).isParking()) {
			return tickCount % 10 == 0;
		}

		return original;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	public void minecarttweaks$readNbt(CompoundTag nbt, CallbackInfo info) {
		var subTag = nbt.getCompound(MinecartTweaks.MOD_ID);
		fuel = subTag.getInt("RealFuel");
		var chunkX = subTag.getInt("PrevChunkPosX");
		var chunkZ = subTag.getInt("PrevChunkPosZ");
		prevChunkPos = new ChunkPos(chunkX, chunkZ);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	public void minecarttweaks$writeNbt(CompoundTag nbt, CallbackInfo info) {
		if(fuel > Short.MAX_VALUE)
			nbt.putShort("Fuel", Short.MAX_VALUE);

		var subTag = new CompoundTag();
		subTag.putInt("RealFuel", fuel);
		subTag.putLong("PrevChunkPosX", prevChunkPos.x);
		subTag.putLong("PrevChunkPosZ", prevChunkPos.z);

		nbt.put(MinecartTweaks.MOD_ID, subTag);
	}
}
