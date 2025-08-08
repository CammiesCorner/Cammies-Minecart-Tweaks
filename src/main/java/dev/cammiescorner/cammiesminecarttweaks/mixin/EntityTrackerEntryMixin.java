package dev.cammiescorner.cammiesminecarttweaks.mixin;

import commonnetwork.api.Network;
import dev.cammiescorner.cammiesminecarttweaks.common.packets.ClientboundSyncChainedMinecartPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {
    @Shadow @Final private Entity entity;

    @Inject(method = "startTracking", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onStartedTrackingBy(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
    public void minecarttweaks$sendLinkingInitData(ServerPlayerEntity player, CallbackInfo ci) {
        if(this.entity instanceof AbstractMinecartEntity minecart) {
			Network.getNetworkHandler().sendToClient(new ClientboundSyncChainedMinecartPacket(minecart.getLinkedParent() != null ? minecart.getLinkedParent().getId() : -1, entity.getId()), player);
			Network.getNetworkHandler().sendToClient(new ClientboundSyncChainedMinecartPacket(entity.getId(), minecart.getLinkedChild() != null ? minecart.getLinkedChild().getId() : -1), player);
        }
    }
}
