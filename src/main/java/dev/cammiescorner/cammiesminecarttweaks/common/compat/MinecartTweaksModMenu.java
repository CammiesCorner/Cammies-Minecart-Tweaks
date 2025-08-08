package dev.cammiescorner.cammiesminecarttweaks.common.compat;

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.client.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaksConfig;

public class MinecartTweaksModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ResourcefulConfig config = MinecartTweaks.CONFIGURATOR.getConfig(MinecartTweaksConfig.class);

			if(config == null)
				return null;

			return new ConfigScreen(null, config);
		};
	}
}
