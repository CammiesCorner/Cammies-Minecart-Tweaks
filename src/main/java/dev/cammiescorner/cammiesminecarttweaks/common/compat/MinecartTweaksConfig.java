package dev.cammiescorner.cammiesminecarttweaks.common.compat;

import eu.midnightdust.lib.config.MidnightConfig;

public class MinecartTweaksConfig extends MidnightConfig {
	@Entry public static boolean useCampfireSmoke = true;
	@Entry public static boolean dontEatEnchantedItems = true;
	@Entry public static boolean playerViewIsLocked = false;
	@Entry(min = 0, max = 90) public static int maxViewAngle = 90;

	@Entry public static double furnaceMinecartSpeed = 20D;
	@Entry public static double otherMinecartSpeed = 8D;
	@Entry public static double maxSpeedAroundTurns = 8D;
	@Entry public static float minecartDamage = 20F;
	@Entry public static int furnaceMaxBurnTime = 72000;
	@Entry public static boolean canLinkMinecarts = true;
	@Entry public static boolean shouldPoweredRailsStopFurnace = true;
	@Entry public static boolean furnacesCanUseAllFuels = true;
	@Entry public static boolean furnaceMinecartsLoadChunks = false;

	public static double getFurnaceMinecartSpeed() {
		return Math.max(0.1, furnaceMinecartSpeed * 0.05);
	}

	public static double getOtherMinecartSpeed() {
		return Math.max(0.1, otherMinecartSpeed * 0.05);
	}

	public static double getMaxSpeedAroundTurns() {
		return Math.min(1, maxSpeedAroundTurns * 0.05);
	}
}
