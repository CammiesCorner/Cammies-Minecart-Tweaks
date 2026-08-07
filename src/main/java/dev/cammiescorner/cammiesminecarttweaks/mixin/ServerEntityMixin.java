package dev.cammiescorner.cammiesminecarttweaks.mixin;

import commonnetwork.api.Network;
import dev.cammiescorner.cammiesminecarttweaks.packets.ClientboundSyncChainedMinecartPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {
    @Shadow @Final private Entity entity;

    @Inject(method = "addPairing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startSeenByPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
    public void minecarttweaks$sendLinkingInitData(ServerPlayer player, CallbackInfo ci) {
        if(this.entity instanceof AbstractMinecart minecart) {
			Network.getNetworkHandler().sendToClient(new ClientboundSyncChainedMinecartPacket(minecart.getLinkedParent() != null ? minecart.getLinkedParent().getId() : -1, entity.getId()), player);
			Network.getNetworkHandler().sendToClient(new ClientboundSyncChainedMinecartPacket(entity.getId(), minecart.getLinkedChild() != null ? minecart.getLinkedChild().getId() : -1), player);
        }
    }
}
