package dev.cammiescorner.cammiesminecarttweaks.mixin.client;

import dev.cammiescorner.cammiesminecarttweaks.MinecartTweaks;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MinecartEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartEntityRenderer.class)
public abstract class MinecartEntityRendererMixin<T extends AbstractMinecartEntity> extends EntityRenderer<T> {
	@Unique private static final Identifier CHAIN_TEXTURE = MinecartTweaks.id("textures/entity/chain.png");
	@Unique private static final RenderLayer CHAIN_LAYER = RenderLayer.getEntitySmoothCutout(CHAIN_TEXTURE);

	protected MinecartEntityRendererMixin(EntityRendererFactory.Context ctx) { super(ctx); }

	@Inject(method = "render(Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
	public void minecarttweaks$render(T child, float yaw, float tickDelta, MatrixStack stack, VertexConsumerProvider provider, int light, CallbackInfo info) {
		AbstractMinecartEntity parent = child.getLinkedParent();
		if(parent != null) {
			double startX = parent.getX();
			double startY = parent.getY();
			double startZ = parent.getZ();
			double endX = child.getX();
			double endY = child.getY();
			double endZ = child.getZ();

			float distanceX = (float) (startX - endX);
			float distanceY = (float) (startY - endY);
			float distanceZ = (float) (startZ - endZ);
			float distance = child.distanceTo(parent);

			double hAngle = Math.toDegrees(Math.atan2(endZ - startZ, endX - startX));
			hAngle += Math.ceil(-hAngle / 360) * 360;

			double vAngle = Math.asin(distanceY / distance);

			renderChain(distanceX, distanceY, distanceZ, (float) hAngle, (float) vAngle, stack, provider, light);
		}
	}

	@Unique
	public void renderChain(float x, float y, float z, float hAngle, float vAngle, MatrixStack stack, VertexConsumerProvider provider, int light) {
		float squaredLength = x * x + y * y + z * z;
		float length = MathHelper.sqrt(squaredLength) - 1F;

		stack.push();
		stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-hAngle - 90));
		stack.multiply(RotationAxis.POSITIVE_X.rotation(-vAngle));
		stack.translate(0, 0, 0.5);
		stack.push();

		VertexConsumer vertexConsumer = provider.getBuffer(CHAIN_LAYER);
		float vertX1 = 0F;
		float vertY1 = 0.25F;
		float vertX2 = MathHelper.sin(6.2831855F) * 0.125F;
		float vertY2 = MathHelper.cos(6.2831855F) * 0.125F;
		float minU = 0F;
		float maxU = 0.1875F;
		float minV = 0F;
		float maxV = length / 10;
		MatrixStack.Entry entry = stack.peek();
		Matrix4f matrix4f = entry.getPositionMatrix();
		Matrix3f matrix3f = entry.getNormalMatrix();

		vertexConsumer.vertex(matrix4f, vertX1, vertY1, 0F).color(0, 0, 0, 255).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
		vertexConsumer.vertex(matrix4f, vertX1, vertY1, length).color(255, 255, 255, 255).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
		vertexConsumer.vertex(matrix4f, vertX2, vertY2, length).color(255, 255, 255, 255).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
		vertexConsumer.vertex(matrix4f, vertX2, vertY2, 0F).color(0, 0, 0, 255).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();

		stack.pop();
		stack.translate(0.19, 0.19, 0);
		stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));

		entry = stack.peek();
		matrix4f = entry.getPositionMatrix();
		matrix3f = entry.getNormalMatrix();

		vertexConsumer.vertex(matrix4f, vertX1, vertY1, 0F).color(0, 0, 0, 255).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
		vertexConsumer.vertex(matrix4f, vertX1, vertY1, length).color(255, 255, 255, 255).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
		vertexConsumer.vertex(matrix4f, vertX2, vertY2, length).color(255, 255, 255, 255).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
		vertexConsumer.vertex(matrix4f, vertX2, vertY2, 0F).color(0, 0, 0, 255).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();

		stack.pop();
	}
}
