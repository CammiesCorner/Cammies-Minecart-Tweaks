package dev.cammiescorner.cammiesminecarttweaks;

import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Config(MinecartTweaks.MOD_ID)
public class MinecartTweaksConfig {
	@ConfigEntry(id = "useCampfireSmoke")
	public static boolean useCampfireSmoke = true;

	@ConfigEntry(id = "dontEatEnchantedItems")
	public static boolean dontEatEnchantedItems = true;

	@ConfigEntry(id = "playerViewIsLocked")
	public static boolean playerViewIsLocked = false;

	@ConfigEntry(id = "maxViewAngle")
	@ConfigOption.Range(min = 0, max = 90)
	public static int maxViewAngle = 90;

	@ConfigEntry(id = "furnaceMinecartSpeed")
	public static double furnaceMinecartSpeed = 20D;
	@ConfigEntry(id = "otherMinecartSpeed")
	public static double otherMinecartSpeed = 8D;
	@ConfigEntry(id = "minecartDamage")
	public static float minecartDamage = 20f;
	@ConfigEntry(id = "furnaceMaxBurnTime")
	public static int furnaceMaxBurnTime = 72000;
	@ConfigEntry(id = "canLinkMinecarts")
	public static boolean canLinkMinecarts = true;
	@ConfigEntry(id = "shouldPoweredRailsStopFurnace")
	public static boolean shouldPoweredRailsStopFurnace = true;
	@ConfigEntry(id = "furnacesCanUseAllFuels")
	public static boolean furnacesCanUseAllFuels = true;
	@ConfigEntry(id = "furnaceMinecartsLoadChunks")
	public static boolean furnaceMinecartsLoadChunks = false;

	public static double getFurnaceMinecartSpeed() {
		return Math.max(0.1, furnaceMinecartSpeed * 0.05);
	}

	public static double getOtherMinecartSpeed() {
		return Math.max(0.1, otherMinecartSpeed * 0.05);
	}
}
