package dev.cammiescorner.cammiesminecarttweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import commonnetwork.api.Network;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.blocks.CrossedRailBlock;
import dev.cammiescorner.cammiesminecarttweaks.data.MTDamageTypes;
import dev.cammiescorner.cammiesminecarttweaks.packets.ClientboundSyncChainedMinecartPacket;
import dev.cammiescorner.cammiesminecarttweaks.util.ext.MinecartPhysicsAccess;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends Entity implements Linkable, MinecartPhysicsAccess {
	@Shadow
	public abstract boolean canCollideWith(Entity entity);

	@Unique private @Nullable UUID parentUuid;
	@Unique private @Nullable UUID childUuid;
	@Unique private int parentIdClient;
	@Unique private int childIdClient;
	@Unique private boolean isMovingOnRail;


	public AbstractMinecartMixin(EntityType<?> type, Level level) {
		super(type, level);
		throw new UnsupportedOperationException();
	}

	/*	MIT License

		Copyright (c) 2022 2No2Name, Inspector Talon

		Permission is hereby granted, free of charge, to any person obtaining a copy
		of this software and associated documentation files (the "Software"), to deal
		in the Software without restriction, including without limitation the rights
		to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
		copies of the Software, and to permit persons to whom the Software is
		furnished to do so, subject to the following conditions:

		The above copyright notice and this permission notice shall be included in all
		copies or substantial portions of the Software.

		THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
		IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
		FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
		AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
		LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
		OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
		SOFTWARE.
	*/
	/* === From Here === */
	@Inject(method = "moveAlongTrack", at = @At("HEAD"))
	private void minecarttweaks$isMovingOnRail(BlockPos pos, BlockState state, CallbackInfo info) {
		isMovingOnRail = true;
	}

	@Inject(method = "moveAlongTrack", at = @At(
		value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;applyNaturalSlowdown()V"
	))
	private void fixVelocityLoss(BlockPos previousPos, BlockState state, CallbackInfo info, @Local(ordinal = 1) Vec3 previousVelocity) {
		if(blockPosition().equals(previousPos))
			return;

		boolean hasHitWall = false;
		var velocity = getDeltaMovement();

		var scale = getBlockSpeedFactor();
		if(velocity.x == 0 && Math.abs(previousVelocity.x) > 0.5) {
			velocity = velocity.with(Direction.Axis.X, previousVelocity.x * scale);
			hasHitWall = true;
		}

		if(velocity.z == 0 && Math.abs(previousVelocity.z) > 0.5) {
			velocity = velocity.with(Direction.Axis.Z, previousVelocity.z * scale);
			hasHitWall = true;
		}

		if(!hasHitWall)
			return;

		BlockState blockState = level().getBlockState(blockPosition());

		if(blockState.is(BlockTags.RAILS))
			this.setDeltaMovement(velocity);
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
		return false;
	}

	@Inject(method = "moveAlongTrack", at = @At("RETURN"))
	private void minecarttweaks$isNotMovingOnRail(BlockPos pos, BlockState state, CallbackInfo info) {
		isMovingOnRail = false;
	}

	@WrapMethod(method = "lerpTo")
	private void minecarttweaks$setMinecartPosLikeOtherEntities(double x, double y, double z, float yRot, float xRot, int steps, Operation<Void> original) {
		if(level().isClientSide()) {
			super.lerpTo(x, y, z, yRot, xRot, steps);
			return;
		}

		original.call(x, y, z, yRot, xRot, steps);
	}
	/* === To Here === */

	@Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
	private void minecarttweaks$increaseSpeed(CallbackInfoReturnable<Double> info) {
		if(getLinkedParent() != null)
			info.setReturnValue(getLinkedParent().getMaxSpeed());
		else
			info.setReturnValue(MinecartTweaksConfig.getOtherMinecartSpeed());
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void minecarttweaks$tick(CallbackInfo info) {
		if(!level().isClientSide()) {
			// TODO make system where the cart with the highest velocity has the most influence, no more of this parent/child crap
//			Vec3d avgVelocity = connectedMinecarts.stream().map(Entity::getVelocity).reduce(Vec3d.ZERO, Vec3d::add).multiply(1f / connectedMinecarts.size());
//			double avgSpeed = avgVelocity.horizontalLength();
//
//			setVelocity(avgVelocity);

			int velocity = Mth.ceil(getDeltaMovement().horizontalDistance());
			Direction direction = Direction.getNearest(getDeltaMovement().x(), 0, getDeltaMovement().z());
			BlockPos minecartPos = blockPosition();
			Vec3i pain = new Vec3i(minecartPos.getX(), 0, minecartPos.getZ());
			var pos = new BlockPos.MutableBlockPos();
			List<Vec3i> poses = new ArrayList<>();

			poses.add(minecartPos);

			for(Vec3i pose : poses) {
				pos.set(pose);
				int distance = pain.distManhattan(new Vec3i(pos.getX(), 0, pos.getZ()));

				if(distance > velocity)
					break;

				if(level().getBlockState(pos.below()).is(BlockTags.RAILS))
					pos.move(0, -1, 0);

				BlockState state = level().getBlockState(pos);

				if(state.is(BlockTags.RAILS) && state.getBlock() instanceof CrossedRailBlock rails) {
					RailShape shape = state.getValue(rails.getShapeProperty());

					if(getDeltaMovement().horizontalDistanceSqr() > 0) {
						if(shape == RailShape.NORTH_SOUTH && direction.getAxis() == Direction.Axis.X) {
							level().setBlockAndUpdate(pos, state.setValue(rails.getShapeProperty(), RailShape.EAST_WEST));
							break;
						}

						if(shape == RailShape.EAST_WEST && direction.getAxis() == Direction.Axis.Z) {
							level().setBlockAndUpdate(pos, state.setValue(rails.getShapeProperty(), RailShape.NORTH_SOUTH));
							break;
						}
					}
				}
			}

			if(getLinkedParent() != null) {
				double distance = getLinkedParent().distanceTo(this) - 1;

				if(distance <= 4) {
					var directionToParent = getLinkedParent().position().subtract(position()).normalize();

					if(distance > 1) {
						var parentVelocity = getLinkedParent().getDeltaMovement();

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
					Linkable.unsetParentChild(getLinkedParent(), this);
					spawnAtLocation(Items.CHAIN);
					return;
				}

				if(getLinkedParent().isRemoved())
					Linkable.unsetParentChild(getLinkedParent(), this);
			}

			if(getLinkedChild() != null && getLinkedChild().isRemoved())
				Linkable.unsetParentChild(this, getLinkedChild());

			for(Entity other : level().getEntities(this, getBoundingBox().inflate(0.1), this::canCollideWith)) {
				if(other instanceof AbstractMinecart minecart && getLinkedParent() != null && !getLinkedParent().equals(minecart))
					minecart.setDeltaMovement(getDeltaMovement());

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
	public AbstractMinecart getLinkedParent() {
		var entity = this.level() instanceof ServerLevel serverLevel && this.parentUuid != null ? serverLevel.getEntity(this.parentUuid) : this.level().getEntity(this.parentIdClient);
		return entity instanceof AbstractMinecart abstractMinecartEntity ? abstractMinecartEntity : null;
	}

	@Override
	public void setLinkedParent(@Nullable AbstractMinecart parent) {
		if(parent != null) {
			this.parentUuid = parent.getUUID();
			this.parentIdClient = parent.getId();
		}
		else {
			this.parentUuid = null;
			this.parentIdClient = -1;
		}

		if(!this.level().isClientSide())
			PlayerLookup.tracking(this).forEach(player -> Network.getNetworkHandler().sendToClient(new ClientboundSyncChainedMinecartPacket(getLinkedParent() != null ? getLinkedParent().getId() : -1, getId()), player));
	}

	@Override
	public void setLinkedParentClient(int id) {
		this.parentIdClient = id;
	}

	@Override
	public @Nullable AbstractMinecart getLinkedChild() {
		var entity = this.level() instanceof ServerLevel serverWorld && this.childUuid != null ? serverWorld.getEntity(this.childUuid) : this.level().getEntity(this.childIdClient);
		return entity instanceof AbstractMinecart abstractMinecartEntity ? abstractMinecartEntity : null;
	}

	@Override
	public void setLinkedChild(@Nullable AbstractMinecart child) {
		if(child != null) {
			this.childUuid = child.getUUID();
			this.childIdClient = child.getId();
		}
		else {
			this.childUuid = null;
			this.childIdClient = -1;
		}
	}

	@Override
	public void setLinkedChildClient(int id) {
		this.childIdClient = id;
	}

	@Override
	public boolean isSelfMovingOnRail() {
		return isMovingOnRail;
	}
}
