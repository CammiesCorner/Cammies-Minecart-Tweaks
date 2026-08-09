package dev.cammiescorner.cammiesminecarttweaks.cca.component;

import dev.cammiescorner.cammiesminecarttweaks.init.MTComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import org.joml.Vector2d;
import org.jspecify.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class PoweredMinecartData implements Component, AutoSyncedComponent {

	private final MinecartFurnace minecart;

	private boolean isParking;
	private final Vector2d storedImpulse = new Vector2d();

	public PoweredMinecartData(MinecartFurnace minecart) {
		this.minecart = minecart;
	}

	@Override
	public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		var impulseX = tag.getDouble("stored_impulse_x");
		var impulseZ = tag.getDouble("stored_impulse_z");
		storedImpulse.set(impulseX, impulseZ);
	}

	@Override
	public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		tag.putDouble("stored_impulse_x", getStoredImpulseX());
		tag.putDouble("stored_impulse_z", getStoredImpulseZ());
	}

	@Override
	public void applySyncPacket(RegistryFriendlyByteBuf buf) {
		var entityWasParked = isParking();
		AutoSyncedComponent.super.applySyncPacket(buf);
		if(entityWasParked != isParking()) {
			if(isParking()) {
				minecart.xPush = 0.0D;
				minecart.zPush = 0.0D;
			}
			else {
				minecart.xPush = getStoredImpulseX();
				minecart.zPush = getStoredImpulseZ();
			}
		}
	}

	public double getStoredImpulseX() {
		return storedImpulse.x();
	}

	public double getStoredImpulseZ() {
		return storedImpulse.y();
	}

	public boolean isParking() {
		return isParking;
	}

	public void setParking(boolean parking) {
		isParking = parking;
	}

	public void sync() {
		minecart.syncComponent(MTComponents.POWERED_MINECART);
	}

	@Nullable
	public static PoweredMinecartData ofNullable(AbstractMinecart minecart) {
		return MTComponents.POWERED_MINECART.getNullable(minecart);
	}

	public static PoweredMinecartData of(AbstractMinecart minecart) {
		return MTComponents.POWERED_MINECART.get(minecart);
	}

	public void storeImpulse(double xPush, double zPush) {
		storedImpulse.set(xPush, zPush);
	}
}
