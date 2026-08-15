package dev.cammiescorner.cammiesminecarttweaks.mixin;

import com.google.common.base.Suppliers;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.blocks.CrossedRailBlock;
import dev.cammiescorner.cammiesminecarttweaks.cca.component.LinkableData;
import dev.cammiescorner.cammiesminecarttweaks.data.MTDamageTypes;
import dev.cammiescorner.cammiesminecarttweaks.ext.AbstractMinecartExt;
import dev.cammiescorner.cammiesminecarttweaks.init.MTComponents;
import dev.cammiescorner.cammiesminecarttweaks.util.MinecartVelocityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends Entity implements AbstractMinecartExt {

	@Shadow
	public abstract boolean canCollideWith(Entity entity);

	@Shadow
	private Vec3 targetDeltaMovement;
	@Unique private boolean isMovingOnRail;
	@Unique private Supplier<LinkableData> linkable;

	public AbstractMinecartMixin(EntityType<?> type, Level level) {
		super(type, level);
		throw new UnsupportedOperationException();
	}

	@Inject(method = "<init>*", at = @At("CTOR_HEAD"))
	private void construct(CallbackInfo ci) {
		linkable = Suppliers.memoize(() -> this.getComponent(MTComponents.LINKABLE));
	}

	@Inject(method = "moveAlongTrack", at = @At("HEAD"))
	private void minecarttweaks$isMovingOnRail(BlockPos pos, BlockState state, CallbackInfo info) {
		isMovingOnRail = true;
	}

	@Inject(method = "moveAlongTrack", at = @At(
		value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;applyNaturalSlowdown()V"
	))
	private void fixVelocityLoss(BlockPos previousPos, BlockState state, CallbackInfo info, @Local(ordinal = 1) Vec3 previousVelocity) {
		BlockState blockState = level().getBlockState(blockPosition());

		MinecartVelocityHelper.fixMinecartVelocityLoss((AbstractMinecart) (Object) this, previousPos, previousVelocity, blockState);
	}

	@ModifyExpressionValue(
		method = "tick",
		at = @At(
			value = "FIELD",
			opcode = Opcodes.GETFIELD,
			target = "Lnet/minecraft/world/level/Level;isClientSide:Z"
		)
	)
	private boolean minecarttweaks$simulateMinecartOnClient(boolean original) {
		// FIXME temp fix, this fixes clientside minecarts just continuously moving and being teleported back when they run out of fuel,
		//  but does not fix the minecart emitting sound
		return (Object) this instanceof MinecartFurnace minecartFurnace && !minecartFurnace.hasFuel();
	}

	@Inject(method = "lerpMotion", at = @At("RETURN"))
	private void setClientTargetVelocity(double x, double y, double z, CallbackInfo ci) {
	}

	@Inject(method = "moveAlongTrack", at = @At("RETURN"))
	private void minecarttweaks$isNotMovingOnRail(BlockPos pos, BlockState state, CallbackInfo info) {
		isMovingOnRail = false;
	}

	@WrapMethod(method = "lerpTo")
	private void minecarttweaks$setMinecartPosLikeOtherEntities(double x, double y, double z, float yRot, float xRot, int steps, Operation<Void> original) {
//		if(level().isClientSide()) {
//			super.lerpTo(x, y, z, yRot, xRot, steps);
//			return;
//		}

		original.call(x, y, z, yRot, xRot, steps);
	}

	@ModifyReturnValue(method = "getMaxSpeed", at = @At("RETURN"))
	private double minecarttweaks$increaseSpeed(double original) {
		return getLinkedParent() instanceof AbstractMinecart parentMinecart ? parentMinecart.getMaxSpeed() : MinecartTweaksConfig.getOtherMinecartSpeed();
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void minecarttweaks$tick(CallbackInfo info) {
		if(!level().isClientSide()) {
			// TODO make system where the cart with the highest velocity has the most influence, no more of this parent/child crap
//			Vec3d avgVelocity = connectedMinecarts.stream().map(Entity::getVelocity).reduce(Vec3d.ZERO, Vec3d::add).multiply(1f / connectedMinecarts.size());
//			double avgSpeed = avgVelocity.horizontalLength();
//
//			setVelocity(avgVelocity);

			Direction direction = Direction.getNearest(getDeltaMovement().x(), 0, getDeltaMovement().z());
			BlockPos railPos = blockPosition();

			var below = railPos.below();
			if(level().getBlockState(railPos.below()).is(BlockTags.RAILS)) {
				railPos = below;
			}

			BlockState railState = level().getBlockState(railPos);

			// TODO move this to wherever it is read back, crossed rails should not need updating in-world
			if(railState.is(BlockTags.RAILS) && railState.getBlock() instanceof CrossedRailBlock rails) {
				if(getDeltaMovement().horizontalDistanceSqr() > 0) {
					switch (direction.getAxis()) {
						case X -> level().setBlockAndUpdate(railPos, railState.setValue(rails.getShapeProperty(), RailShape.EAST_WEST));
						case Z -> level().setBlockAndUpdate(railPos, railState.setValue(rails.getShapeProperty(), RailShape.NORTH_SOUTH));
						default -> {}
					}
				}
			}

			var parentObj = getLinkedParent();
			if(parentObj instanceof AbstractMinecart parent) {
				double distance = parent.distanceTo(this) - 1;

				if(distance <= 4) {
					var directionToParent = parent.position().subtract(position()).normalize();

					if(distance > 1) {
						var parentVelocity = parent.getDeltaMovement();

						if(parentVelocity.lengthSqr() == 0) {
							setDeltaMovement(directionToParent.scale(0.05));
						}
						else {
							setDeltaMovement(directionToParent.scale(parentVelocity.length() * distance));
						}
					}
					else if(distance < 0.8) {
						setDeltaMovement(directionToParent.scale(-0.05));
					} else {
						setDeltaMovement(Vec3.ZERO);
					}
				} else {
					Linkable.setParentChild(parent, null);
					spawnAtLocation(Items.CHAIN);
				}
			}

			for(Entity other : level().getEntities(this, getBoundingBox().inflate(0.1), this::canCollideWith)) {
				if(other instanceof AbstractMinecart minecart && minecart != parentObj) {
					minecart.setDeltaMovement(getDeltaMovement());
				}

				float damage = MinecartTweaksConfig.minecartDamage;

				if(damage > 0 && !level().isClientSide() && other instanceof LivingEntity living && living.isAlive() && !living.isPassenger() && getDeltaMovement().lengthSqr() > 1.5D * 1.5D) {
					var knockback = living.getDeltaMovement().add(getDeltaMovement().x() * 0.9, getDeltaMovement().length() * 0.2, getDeltaMovement().z() * 0.9);
					living.setDeltaMovement(knockback);
					living.hasImpulse = true;
					living.hurt(living.damageSources().source(MTDamageTypes.MINECART_DAMAGE, this, getFirstPassenger()), damage);
				}
			}
		}
		else {
			// TODO doesnt appear to be working ;-;
			if(MinecartTweaksConfig.playerViewIsLocked) {
				var directionVec = getDeltaMovement().normalize();

				var maxSpeed = MinecartTweaksConfig.getOtherMinecartSpeed() * 0.5;
				if(getDeltaMovement().lengthSqr() > maxSpeed * maxSpeed) {
					float yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(directionVec.z(), directionVec.x())) - 90);

					for(Entity passenger : getPassengers()) {
						float wantedYaw = Mth.wrapDegrees(Mth.rotateIfNecessary(passenger.getYRot(), yaw, MinecartTweaksConfig.maxViewAngle) - passenger.getYRot());
						float steps = Math.abs(wantedYaw) / 5f;

						if(wantedYaw >= steps)
							passenger.setYRot(passenger.getYRot() + steps);
						if(wantedYaw <= -steps)
							passenger.setYRot(passenger.getYRot() - steps);
					}
				}
			}
		}
	}

	// TODO move chain dropping logic
//	@Inject(method = "dropItems", at = @At("HEAD"))
//	private void minecarttweaks$dropChain(DamageSource damageSource, CallbackInfo info) {
//		if(getLinkedParent() != null || getLinkedChild() != null)
//			dropStack(new ItemStack(Items.CHAIN));
//	}

	@WrapOperation(method = "moveAlongTrack", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"))
	private double minecarttweaks$uncapSpeed(double garbo, double uncappedSpeed, Operation<Double> original) {
		return uncappedSpeed;
	}

	@Override
	public Linkable getLinkedParent() {
		return linkable.get().getParent();
	}

	@Override
	public void setLinkedParent(@Nullable Linkable parent) {
		linkable.get().setParentRef(parent instanceof Entity entity ? entity.getUUID() : null);
	}

	@Override
	public @Nullable Linkable getLinkedChild() {
		return linkable.get().getChild();
	}

	@Override
	public void setLinkedChild(@Nullable Linkable child) {
		linkable.get().setChildRef(child instanceof Entity entity ? entity.getUUID() : null);
	}

	@Override
	public boolean isSelfMovingOnRail() {
		return isMovingOnRail;
	}
}
