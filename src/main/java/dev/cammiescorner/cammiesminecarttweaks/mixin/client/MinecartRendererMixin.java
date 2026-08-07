package dev.cammiescorner.cammiesminecarttweaks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartRenderer.class)
public abstract class MinecartRendererMixin<T extends AbstractMinecart> extends EntityRenderer<T> {
	@Unique private static final ResourceLocation CHAIN_TEXTURE = MinecartTweaks.id("textures/entity/chain.png");
	@Unique private static final RenderType CHAIN_LAYER = RenderType.entitySmoothCutout(CHAIN_TEXTURE);

	protected MinecartRendererMixin(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Inject(method = "render(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
	public void minecarttweaks$render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		var parent = entity.getLinkedParent();
		if(parent != null) {
			double startX = parent.getX();
			double startY = parent.getY();
			double startZ = parent.getZ();
			double endX = entity.getX();
			double endY = entity.getY();
			double endZ = entity.getZ();

			float distanceX = (float) (startX - endX);
			float distanceY = (float) (startY - endY);
			float distanceZ = (float) (startZ - endZ);
			float distance = entity.distanceTo(parent);

			double hAngle = Math.toDegrees(Math.atan2(endZ - startZ, endX - startX));
			hAngle += Math.ceil(-hAngle / 360) * 360;

			double vAngle = Math.asin(distanceY / distance);

			renderChain(distanceX, distanceY, distanceZ, (float) hAngle, (float) vAngle, poseStack, buffer, packedLight);
		}
	}

	@Unique
	public void renderChain(float x, float y, float z, float hAngle, float vAngle, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		float length = Mth.sqrt(Mth.lengthSquared(x, y, z)) - 1.0F;

		VertexConsumer vertexConsumer = buffer.getBuffer(CHAIN_LAYER);

		poseStack.pushPose();
		poseStack.mulPose(Axis.YN.rotationDegrees(hAngle + 90));
		poseStack.mulPose(Axis.XN.rotation(vAngle));
		poseStack.translate(0, 0, 0.5);

		drawChainQuad(poseStack, vertexConsumer, length, OverlayTexture.NO_OVERLAY, packedLight);

		poseStack.translate(0.19, 0.19, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees(90));

		drawChainQuad(poseStack, vertexConsumer, length, OverlayTexture.NO_OVERLAY, packedLight);

		poseStack.popPose();
	}

	@Unique
	private static void drawChainQuad(PoseStack poseStack, VertexConsumer vertexConsumer, float length, int overlay, int packedLight) {
		float vertX1 = 0f;
		float vertY1 = 0.25f;
		float vertX2 = Mth.sin(6.2831855f) * 0.125f;
		float vertY2 = Mth.cos(6.2831855f) * 0.125f;

		float minU = 0f;
		float maxU = 0.1875f;
		float minV = 0f;
		float maxV = length / 10;

		PoseStack.Pose entry = poseStack.last();
		Matrix4f matrix4f = entry.pose();

		vertexConsumer.addVertex(matrix4f, vertX1, vertY1, 0f).setColor(0, 0, 0, 255).setUv(minU, minV).setOverlay(overlay).setLight(packedLight).setNormal(entry, 0f, -1f, 0f);
		vertexConsumer.addVertex(matrix4f, vertX1, vertY1, length).setColor(255, 255, 255, 255).setUv(minU, maxV).setOverlay(overlay).setLight(packedLight).setNormal(entry, 0f, -1f, 0f);
		vertexConsumer.addVertex(matrix4f, vertX2, vertY2, length).setColor(255, 255, 255, 255).setUv(maxU, maxV).setOverlay(overlay).setLight(packedLight).setNormal(entry, 0f, -1f, 0f);
		vertexConsumer.addVertex(matrix4f, vertX2, vertY2, 0f).setColor(0, 0, 0, 255).setUv(maxU, minV).setOverlay(overlay).setLight(packedLight).setNormal(entry, 0f, -1f, 0f);

	}
}
