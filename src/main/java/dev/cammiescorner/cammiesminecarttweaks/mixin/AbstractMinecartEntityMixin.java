package dev.cammiescorner.cammiesminecarttweaks.mixin;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.api.Linkable;
import dev.cammiescorner.cammiesminecarttweaks.common.packets.SyncChainedMinecartPacket;
import dev.cammiescorner.cammiesminecarttweaks.integration.MinecartTweaksConfig;
import dev.cammiescorner.cammiesminecarttweaks.utils.MinecartHelper;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity implements Linkable {
	@Unique private @Nullable UUID parentUuid;
	@Unique private @Nullable UUID childUuid;

	@Unique private int parentIdClient;
	@Unique private int childIdClient;

	public AbstractMinecartEntityMixin(EntityType<?> type, World world) { super(type, world); }

	@Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
	public void minecarttweaks$increaseSpeed(CallbackInfoReturnable<Double> info) {
		if(getLinkedParent() != null)
			info.setReturnValue(getLinkedParent().getMaxSpeed());
		else
			info.setReturnValue(MinecartTweaksConfig.getOtherMinecartSpeed());
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void minecarttweaks$tick(CallbackInfo info) {
		if(!this.getWorld().isClient()) {
			if(getLinkedParent() != null) {
				double distance = getLinkedParent().distanceTo(this) - 1;

				if(distance <= 4) {
					Vec3d direction = getLinkedParent().getPos().subtract(getPos()).normalize();

					if(distance > 1) {
						Vec3d parentVelocity = getLinkedParent().getVelocity();

						if(parentVelocity.length() == 0) {
							setVelocity(direction.multiply(0.05));
						}
						else {
							setVelocity(direction.multiply(parentVelocity.length()));
							setVelocity(getVelocity().multiply(distance));
						}
					}
					else if(distance < 0.8)
						setVelocity(direction.multiply(-0.05));
					else
						setVelocity(Vec3d.ZERO);
				}
				else {
					Linkable.unsetParentChild(this.getLinkedParent(), this);
					dropStack(new ItemStack(Items.CHAIN));
					return;
				}

				if(getLinkedParent().isRemoved())
					Linkable.unsetParentChild(getLinkedParent(), this);
			}
			else {
				MinecartHelper.shouldSlowDown((AbstractMinecartEntity) (Object) this, this.getWorld());
			}

			if(getLinkedChild() != null && getLinkedChild().isRemoved())
				Linkable.unsetParentChild(this, getLinkedChild());

			this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(this.getVelocity()), this::collidesWith).forEach(other -> {
				if(other instanceof AbstractMinecartEntity minecart && getLinkedParent() != null && !getLinkedParent().equals(minecart)) {
					minecart.setVelocity(getVelocity());
				}

				float damage = MinecartTweaksConfig.minecartDamage;

				if(damage > 0 && !this.getWorld().isClient() && other instanceof LivingEntity living && living.isAlive() && !living.hasVehicle() && getVelocity().length() > 1.5) {
					Vec3d knockback = living.getVelocity().add(getVelocity().getX() * 0.9, getVelocity().length() * 0.2, getVelocity().getZ() * 0.9);
					living.setVelocity(knockback);
					living.velocityDirty = true;
					living.damage(MinecartTweaks.minecart(this), damage);
				}
			});
		}
		else {
			if(MinecartTweaksConfig.playerViewIsLocked) {
				Vec3d directionVec = getVelocity().normalize();

				if(getVelocity().length() > MinecartTweaksConfig.getOtherMinecartSpeed() * 0.5) {
					float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(directionVec.getZ(), directionVec.getX())) - 90);

					for(Entity passenger : getPassengerList()) {
						float wantedYaw = MathHelper.wrapDegrees(MathHelper.clampAngle(passenger.getYaw(), yaw, MinecartTweaksConfig.maxViewAngle) - passenger.getYaw());
						float steps = Math.abs(wantedYaw) / 5F;

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
		if(nbt.contains("ParentUuid"))
			parentUuid = nbt.getUuid("ParentUuid");
		if(nbt.contains("ChildUuid"))
			childUuid = nbt.getUuid("ChildUuid");
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
	public void minecarttweaks$writeNbt(NbtCompound nbt, CallbackInfo info) {
		if(this.parentUuid != null)
			nbt.putUuid("ParentUuid", this.parentUuid);
		if(this.childUuid != null)
			nbt.putUuid("ChildUuid", this.childUuid);
	}

	@Redirect(method = "moveOnRail", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"))
	private double minecarttweaks$uncapSpeed(double garbo, double uncappedSpeed) {
		return uncappedSpeed;
	}

	@Override
	public AbstractMinecartEntity getLinkedParent() {
		var entity = this.getWorld() instanceof ServerWorld serverWorld && this.parentUuid != null ? serverWorld.getEntity(this.parentUuid) : this.getWorld().getEntityById(this.parentIdClient);
		return entity instanceof AbstractMinecartEntity abstractMinecartEntity ? abstractMinecartEntity : null;
	}

	@Override
	public void setLinkedParent(@Nullable AbstractMinecartEntity parent) {
		if (parent != null) {
			this.parentUuid = parent.getUuid();
			this.parentIdClient = parent.getId();
		} else {
			this.parentUuid = null;
			this.parentIdClient = -1;
		}

		if (!this.getWorld().isClient()) {
			PlayerLookup.tracking(this).forEach(player -> SyncChainedMinecartPacket.send(this.getLinkedParent(), (AbstractMinecartEntity) (Object) this, player));
		}
	}

	@Override
	public void setLinkedParentClient(int id) {
		this.parentIdClient = id;
	}

	@Override
	public AbstractMinecartEntity getLinkedChild() {
		var entity = this.getWorld() instanceof ServerWorld serverWorld && this.childUuid != null ? serverWorld.getEntity(this.childUuid) : this.getWorld().getEntityById(this.childIdClient);
		return entity instanceof AbstractMinecartEntity abstractMinecartEntity ? abstractMinecartEntity : null;
	}

	@Override
	public void setLinkedChild(@Nullable AbstractMinecartEntity child) {
		if (child != null) {
			this.childUuid = child.getUuid();
			this.childIdClient = child.getId();
		} else {
			this.childUuid = null;
			this.childIdClient = -1;
		}
	}

	@Override
	public void setLinkedChildClient(int id) {
		this.childIdClient = id;
	}
}
