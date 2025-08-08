package dev.cammiescorner.cammiesminecarttweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import commonnetwork.api.Network;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.common.blocks.CrossedRailBlock;
import dev.cammiescorner.cammiesminecarttweaks.common.packets.ClientboundSyncChainedMinecartPacket;
import dev.cammiescorner.cammiesminecarttweaks.common.utils.MinecartPhysicsAccess;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.minecraft.util.math.Direction.*;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements Linkable, MinecartPhysicsAccess {
	@Unique private @Nullable UUID parentUuid;
	@Unique private @Nullable UUID childUuid;
	@Unique private int parentIdClient;
	@Unique private int childIdClient;
	@Unique private boolean isMovingOnRail;


	public AbstractMinecartEntityMixin(EntityType<?> type, World world) { super(type, world); }

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
	@Inject(method = "moveOnRail", at = @At("HEAD"))
	private void minecarttweaks$isMovingOnRail(BlockPos pos, BlockState state, CallbackInfo info) {
		isMovingOnRail = true;
	}

	@Inject(method = "moveOnRail", at = @At(
		value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;applySlowdown()V", shift = At.Shift.BEFORE
	))
	private void fixVelocityLoss(BlockPos previousPos, BlockState state, CallbackInfo info, @Local(ordinal = 1) Vec3d previousVelocity) {
		if(getBlockPos().equals(previousPos))
			return;

		boolean hasHitWall = false;
		Vec3d velocity = getVelocity();

		if(velocity.x == 0 && Math.abs(previousVelocity.x) > 0.5) {
			velocity = velocity.withAxis(Direction.Axis.X, previousVelocity.x * getVelocityMultiplier());
			hasHitWall = true;
		}

		if(velocity.z == 0 && Math.abs(previousVelocity.z) > 0.5) {
			velocity = velocity.withAxis(Direction.Axis.Z, previousVelocity.z * getVelocityMultiplier());
			hasHitWall = true;
		}

		if(!hasHitWall)
			return;

		BlockState blockState = getWorld().getBlockState(getBlockPos());

		if(blockState.isOf(Blocks.RAIL))
			this.setVelocity(velocity);
	}

	@Inject(method = "moveOnRail", at = @At("RETURN"))
	private void minecarttweaks$isNotMovingOnRail(BlockPos pos, BlockState state, CallbackInfo info) {
		isMovingOnRail = false;
	}

	@ModifyExpressionValue(method = "tick", at = @At(
		value = "FIELD", target = "Lnet/minecraft/world/World;isClient:Z"
	))
	private boolean minecarttweaks$simulateMinecartOnClient(boolean original) {
		return false;
	}

	@Inject(method = "updateTrackedPositionAndAngles", at = @At("HEAD"), cancellable = true)
	private void minecarttweaks$setMinecartPosLikeOtherEntities(double x, double y, double z, float yaw, float pitch, int interpolationSteps, CallbackInfo ci) {
		if(getWorld().isClient) {
			super.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, interpolationSteps);
			ci.cancel();
		}
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
		if(!getWorld().isClient()) {
			// TODO make system where the cart with the highest velocity has the most influence, no more of this parent/child crap
//			Vec3d avgVelocity = connectedMinecarts.stream().map(Entity::getVelocity).reduce(Vec3d.ZERO, Vec3d::add).multiply(1f / connectedMinecarts.size());
//			double avgSpeed = avgVelocity.horizontalLength();
//
//			setVelocity(avgVelocity);

			int velocity = MathHelper.ceil(getVelocity().horizontalLength());
			Direction direction = Direction.getFacing(getVelocity().getX(), 0, getVelocity().getZ());
			BlockPos minecartPos = getBlockPos();
			Vec3i pain = new Vec3i(minecartPos.getX(), 0, minecartPos.getZ());
			BlockPos.Mutable pos = new BlockPos.Mutable();
			List<Vec3i> poses = new ArrayList<>();

			poses.add(minecartPos);

			for(Vec3i pose : poses) {
				pos.set(pose);
				int distance = pain.getManhattanDistance(new Vec3i(pos.getX(), 0, pos.getZ()));

				if(distance > velocity)
					break;

				if(getWorld().getBlockState(pos.down()).isIn(BlockTags.RAILS))
					pos.move(0, -1, 0);

				BlockState state = getWorld().getBlockState(pos);

				if(state.isIn(BlockTags.RAILS) && state.getBlock() instanceof CrossedRailBlock rails) {
					RailShape shape = state.get(rails.getShapeProperty());

					if(getVelocity().horizontalLength() > 0) {
						if(shape == RailShape.NORTH_SOUTH && (direction == EAST || direction == WEST)) {
							getWorld().setBlockState(pos, state.with(rails.getShapeProperty(), RailShape.EAST_WEST));
							break;
						}

						if(shape == RailShape.EAST_WEST && (direction == Direction.NORTH || direction == SOUTH)) {
							getWorld().setBlockState(pos, state.with(rails.getShapeProperty(), RailShape.NORTH_SOUTH));
							break;
						}
					}
				}
			}

			if(getLinkedParent() != null) {
				double distance = getLinkedParent().distanceTo(this) - 1;

				if(distance <= 4) {
					Vec3d directionToParent = getLinkedParent().getPos().subtract(getPos()).normalize();

					if(distance > 1) {
						Vec3d parentVelocity = getLinkedParent().getVelocity();

						if(parentVelocity.length() == 0) {
							setVelocity(directionToParent.multiply(0.05));
						}
						else {
							setVelocity(directionToParent.multiply(parentVelocity.length()));
							setVelocity(getVelocity().multiply(distance));
						}
					}
					else if(distance < 0.8)
						setVelocity(directionToParent.multiply(-0.05));
					else
						setVelocity(Vec3d.ZERO);
				}
				else {
					Linkable.unsetParentChild(getLinkedParent(), this);
					dropStack(new ItemStack(Items.CHAIN));
					return;
				}

				if(getLinkedParent().isRemoved())
					Linkable.unsetParentChild(getLinkedParent(), this);
			}

			if(getLinkedChild() != null && getLinkedChild().isRemoved())
				Linkable.unsetParentChild(this, getLinkedChild());

			for(Entity other : getWorld().getOtherEntities(this, getBoundingBox().expand(0.1), this::collidesWith)) {
				if(other instanceof AbstractMinecartEntity minecart && getLinkedParent() != null && !getLinkedParent().equals(minecart))
					minecart.setVelocity(getVelocity());

				float damage = MinecartTweaksConfig.minecartDamage;

				if(damage > 0 && !getWorld().isClient() && other instanceof LivingEntity living && living.isAlive() && !living.hasVehicle() && getVelocity().length() > 1.5) {
					Vec3d knockback = living.getVelocity().add(getVelocity().getX() * 0.9, getVelocity().length() * 0.2, getVelocity().getZ() * 0.9);
					living.setVelocity(knockback);
					living.velocityDirty = true;
					living.damage(getWorld().getDamageSources().create(MinecartTweaks.MINECART_DAMAGE, this, getFirstPassenger()), damage);
				}
			}
		}
		else {
			// TODO doesnt appear to be working ;-;
			if(MinecartTweaksConfig.playerViewIsLocked) {
				Vec3d directionVec = getVelocity().normalize();

				if(getVelocity().length() > MinecartTweaksConfig.getOtherMinecartSpeed() * 0.5) {
					float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(directionVec.getZ(), directionVec.getX())) - 90);

					for(Entity passenger : getPassengerList()) {
						float wantedYaw = MathHelper.wrapDegrees(MathHelper.clampAngle(passenger.getYaw(), yaw, MinecartTweaksConfig.maxViewAngle) - passenger.getYaw());
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

	// TODO move chain dropping logic
//	@Inject(method = "dropItems", at = @At("HEAD"))
//	private void minecarttweaks$dropChain(DamageSource damageSource, CallbackInfo info) {
//		if(getLinkedParent() != null || getLinkedChild() != null)
//			dropStack(new ItemStack(Items.CHAIN));
//	}

	@WrapOperation(method = "moveOnRail", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"))
	private double minecarttweaks$uncapSpeed(double garbo, double uncappedSpeed, Operation<Double> original) {
		return uncappedSpeed;
	}

	@Override
	public AbstractMinecartEntity getLinkedParent() {
		var entity = this.getWorld() instanceof ServerWorld serverWorld && this.parentUuid != null ? serverWorld.getEntity(this.parentUuid) : this.getWorld().getEntityById(this.parentIdClient);
		return entity instanceof AbstractMinecartEntity abstractMinecartEntity ? abstractMinecartEntity : null;
	}

	@Override
	public void setLinkedParent(@Nullable AbstractMinecartEntity parent) {
		if(parent != null) {
			this.parentUuid = parent.getUuid();
			this.parentIdClient = parent.getId();
		}
		else {
			this.parentUuid = null;
			this.parentIdClient = -1;
		}

		if(!this.getWorld().isClient())
			PlayerLookup.tracking(this).forEach(player -> Network.getNetworkHandler().sendToClient(new ClientboundSyncChainedMinecartPacket(getLinkedParent() != null ? getLinkedParent().getId() : -1, getId()), player));
	}

	@Override
	public void setLinkedParentClient(int id) {
		this.parentIdClient = id;
	}

	@Override
	public @Nullable AbstractMinecartEntity getLinkedChild() {
		var entity = this.getWorld() instanceof ServerWorld serverWorld && this.childUuid != null ? serverWorld.getEntity(this.childUuid) : this.getWorld().getEntityById(this.childIdClient);
		return entity instanceof AbstractMinecartEntity abstractMinecartEntity ? abstractMinecartEntity : null;
	}

	@Override
	public void setLinkedChild(@Nullable AbstractMinecartEntity child) {
		if(child != null) {
			this.childUuid = child.getUuid();
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
