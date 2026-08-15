package dev.cammiescorner.cammiesminecarttweaks.data;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class MTTags {
	public static class Items {
		public static final TagKey<Item> LINK_ITEMS = TagKey.create(Registries.ITEM, MinecartTweaks.id("link_items"));
	}
}
