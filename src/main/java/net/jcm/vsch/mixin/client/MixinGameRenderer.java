package net.jcm.vsch.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
	@Redirect(
		method = "renderLevel",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V")
		),
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 0)
	)
	public void renderLevel$mulPose$0(
		final PoseStack stack,
		final Quaternionf rotationZ,
		@Local final Camera camera
	) {
		final Vector3f angles = camera.rotation().getEulerAnglesYXZ(new Vector3f());
		stack.mulPose(Axis.ZP.rotation(angles.z));
		stack.mulPose(Axis.XP.rotation(angles.x));
		stack.mulPose(Axis.YP.rotation(-angles.y + (float)(Math.PI)));
	}

	@Redirect(
		method = "renderLevel",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V")
		),
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 1)
	)
	public void renderLevel$mulPose$1(
		final PoseStack stack,
		final Quaternionf rotationX
	) {
	}

	@Redirect(
		method = "renderLevel",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V")
		),
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 2)
	)
	public void renderLevel$mulPose$2(
		final PoseStack stack,
		final Quaternionf rotationY
	) {
	}
}
