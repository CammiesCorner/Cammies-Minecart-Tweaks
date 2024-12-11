package dev.cammiescorner.cammiesminecarttweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.common.compat.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.common.utils.MinecartPhysicsAccess;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements Linkable, MinecartPhysicsAccess {
	@Unique private final List<AbstractMinecartEntity> connectedMinecarts = new ArrayList<>();
	@Unique private boolean isMovingOnRail;

	public AbstractMinecartEntityMixin(EntityType<?> type, World world) { super(type, world); }

	@Inject(method = "moveOnRail", at = @At("HEAD"))
	public void minecarttweaks$isMovingOnRail(BlockPos pos, BlockState state, CallbackInfo info) {
		this.isMovingOnRail = true;
	}

	@Inject(method = "moveOnRail", at = @At(
		value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;applySlowdown()V", shift = At.Shift.BEFORE
	))
	public void fixVelocityLoss(BlockPos previousPos, BlockState state, CallbackInfo info, @Local(ordinal = 1) Vec3d previousVelocity) {
		if(this.getBlockPos().equals(previousPos))
			return;

		boolean hasHitWall = false;
		Vec3d velocity = this.getVelocity();

		if(velocity.x == 0 && Math.abs(previousVelocity.x) > 0.5) {
			velocity = velocity.withAxis(Direction.Axis.X, previousVelocity.x * this.getVelocityMultiplier());
			hasHitWall = true;
		}

		if(velocity.z == 0 && Math.abs(previousVelocity.z) > 0.5) {
			velocity = velocity.withAxis(Direction.Axis.Z, previousVelocity.z * this.getVelocityMultiplier());
			hasHitWall = true;
		}

		if(!hasHitWall)
			return;

		BlockState blockState = this.getWorld().getBlockState(this.getBlockPos());

		if(blockState.isOf(Blocks.RAIL))
			this.setVelocity(velocity);
	}

	@Inject(method = "moveOnRail", at = @At("RETURN"))
	public void minecarttweaks$isNotMovingOnRail(BlockPos pos, BlockState state, CallbackInfo info) {
		this.isMovingOnRail = false;
	}

	@ModifyExpressionValue(method = "tick", at = @At(
		value = "FIELD", target = "Lnet/minecraft/world/World;isClient:Z"
	))
	private boolean minecarttweaks$simulateMinecartOnClient(boolean original) {
		return false;
	}

	@Inject(method = "updateTrackedPositionAndAngles", at = @At("HEAD"), cancellable = true)
	private void minecarttweaks$setMinecartPosLikeOtherEntities(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate, CallbackInfo info) {
		if(getWorld().isClient) {
			super.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, interpolationSteps, interpolate);
			info.cancel();
		}
	}

	@Inject(method = "getMaxOffRailSpeed", at = @At("RETURN"), cancellable = true)
	public void minecarttweaks$increaseSpeed(CallbackInfoReturnable<Double> info) {
		if(getLinkedParent() != null)
			info.setReturnValue(getLinkedParent().getMaxOffRailSpeed());
		else
			info.setReturnValue(MinecartTweaksConfig.getOtherMinecartSpeed());
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void minecarttweaks$tick(CallbackInfo info) {
		if(!this.getWorld().isClient()) {
			// TODO make system where the cart with the highest velocity has the most influence, no more of this parent/child crap
			if(getLinkedChild() != null && getLinkedChild().isRemoved())
				Linkable.unsetParentChild(this, (Linkable) getLinkedChild());

			for(Entity other : getWorld().getOtherEntities(this, getBoundingBox().expand(1, 0, 1), this::collidesWith)) {
				if(other instanceof AbstractMinecartEntity minecart && getLinkedParent() != null && !getLinkedParent().equals(minecart))
					minecart.setVelocity(getVelocity());

				float damage = MinecartTweaksConfig.minecartDamage;

				if(damage > 0 && !this.getWorld().isClient() && other instanceof LivingEntity living && living.isAlive() && !living.hasVehicle() && getVelocity().length() > 1.5) {
					Vec3d knockback = living.getVelocity().add(getVelocity().getX() * 0.9, getVelocity().length() * 0.2, getVelocity().getZ() * 0.9);
					living.setVelocity(knockback);
					living.velocityDirty = true;
					living.damage(MinecartTweaks.minecart(this), damage);
				}
			}
		}
		else {
			if(MinecartTweaksConfig.playerViewIsLocked) {
				Vec3d directionVec = getVelocity().normalize();

				if(getVelocity().length() > MinecartTweaksConfig.getOtherMinecartSpeed() * 0.5) {
					float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(directionVec.getZ(), directionVec.getX())) - 90);

					for(Entity passenger : getPassengerList()) {
						float wantedYaw = MathHelper.wrapDegrees(MathHelper.stepAngleTowards(passenger.getYaw(), yaw, MinecartTweaksConfig.maxViewAngle) - passenger.getYaw());
						float steps = Math.abs(wantedYaw) / 5f;

						if(wantedYaw >= steps)
							passenger.setYaw(passenger.getYaw() + steps);
						if(wantedYaw <= -steps)
							passenger.setYaw(passenger.getYaw() - steps);
					}
				}
			}
		}
	}

	@Inject(method = "dropItems", at = @At("HEAD"))
	public void minecarttweaks$dropChain(DamageSource damageSource, CallbackInfo info) {
		if(getLinkedParent() != null || getLinkedChild() != null)
			dropStack(new ItemStack(Items.CHAIN));
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
	public void minecarttweaks$readNbt(NbtCompound nbt, CallbackInfo info) {

	}

	@Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
	public void minecarttweaks$writeNbt(NbtCompound nbt, CallbackInfo info) {

	}

	@Redirect(method = "moveOnRail", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"))
	private double minecarttweaks$uncapSpeed(double garbo, double uncappedSpeed) {
		return uncappedSpeed;
	}

	@Override
	public boolean isSelfMovingOnRail() {
		return isMovingOnRail;
	}
}
