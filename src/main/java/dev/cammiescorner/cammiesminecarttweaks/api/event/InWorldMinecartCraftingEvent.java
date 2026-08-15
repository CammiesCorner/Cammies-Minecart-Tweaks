package dev.cammiescorner.cammiesminecarttweaks.api.event;

import dev.upcraft.sparkweave.api.event.Event;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface InWorldMinecartCraftingEvent {

	Event<InWorldMinecartCraftingEvent> EVENT = Event.create(InWorldMinecartCraftingEvent.class, listeners -> ctx -> {
		for (InWorldMinecartCraftingEvent listener : listeners) {
			if(!listener.tryUpgradeMinecart(ctx)) {
				return false;
			}
		}

		return true;
	});

	boolean tryUpgradeMinecart(InWorldMinecartCraftingEvent.Context ctx);

	interface Context {
		ServerPlayer getPlayer();
		InteractionHand getHand();
		ItemStack getItemStack();
		ServerLevel getLevel();
		AbstractMinecart getOriginalEntity();

		@Nullable Entity getResultEntity();
		void setResultEntity(@Nullable Entity entity);
	}
}
