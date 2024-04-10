package dev.cammiescorner.cammiesminecarttweaks.common.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;

public class MinecartTweaksModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> MinecartTweaksConfig.getScreen(parent, MinecartTweaks.MOD_ID);
	}
}
