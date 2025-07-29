package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.joml.Quaternionf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {
	@WrapOperation(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"
		)
	)
	protected void render$setupRotations(
		final LivingEntityRenderer self,
		final LivingEntity entity,
		final PoseStack poseStack,
		final float bob,
		final float yaw,
		final float partialTick,
		final Operation<Void> operation
	) {
		if (!(entity instanceof Player player) || !(player instanceof FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
			operation.call(self, entity, poseStack, bob, yaw, partialTick);
			return;
		}

		final Quaternionf rotation = frp.vsch$getRotationO().slerp(frp.vsch$getRotation(), partialTick, new Quaternionf());

		final EntityDimensions vanillaDim = frp.vsch$getVanillaDimensions(player.getPose());
		poseStack.translate(0, 0.6f / 2, 0);
		poseStack.mulPose(rotation);
		poseStack.translate(0, 0.6f - vanillaDim.height - 0.6f / 2, 0);

		operation.call(self, entity, poseStack, bob, 0f, partialTick);
	}

	@WrapOperation(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"
		)
	)
	public void render$setupAnim(
		final EntityModel model,
		final Entity entity,
		final float limbSwing,
		final float limbSwingAmount,
		final float age,
		float headYaw,
		float headPitch,
		final Operation<Void> operation
	) {
		if ((entity instanceof Player player) && (player instanceof FreeRotatePlayerAccessor frp) && frp.vsch$isFreeRotating()) {
			headYaw = 0;
			headPitch = 0;
		}
		operation.call(model, entity, limbSwing, limbSwingAmount, age, headYaw, headPitch);
	}
}
