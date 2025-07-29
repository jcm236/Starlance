package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerRenderer.class)
public abstract class MixinPlayerRenderer {
	@WrapMethod(method = "render")
	public void render(
		final AbstractClientPlayer player,
		final float yaw,
		final float partialTick,
		final PoseStack poseStack,
		final MultiBufferSource bufferSource,
		final int packedLight,
		final Operation<Void> operation
	) {
		System.out.println("rendering player: " + player + ", " + (player instanceof FreeRotatePlayerAccessor) + ", " + (!(player instanceof FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()));
		if (!(player instanceof FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
			operation.call(player, yaw, partialTick, poseStack, bufferSource, packedLight);
			return;
		}

		poseStack.pushPose();

		final float playerYHeadRotO = player.yHeadRotO;
		final float playerYHeadRot = player.yHeadRot;
		final float playerYBodyRotO = player.yBodyRotO;
		final float playerYBodyRot = player.yBodyRot;
		final float playerXRotO = player.xRotO;
		final float playerXRot = player.getXRot();
		final Quaternionf rotation = new Quaternionf(frp.vsch$getRotation());

		player.yHeadRotO = 0;
		player.yHeadRot = 0;
		player.yBodyRotO = 0;
		player.yBodyRot = 0;
		player.xRotO = 0;
		player.setXRot(0);

		final EntityDimensions vanillaDim = frp.vsch$getVanillaDimensions(player.getPose());
		poseStack.translate(0, 0.6f / 2, 0);
		poseStack.mulPose(rotation);
		poseStack.translate(0, 0.6f - vanillaDim.height - 0.6f / 2, 0);

		operation.call(player, yaw, partialTick, poseStack, bufferSource, packedLight);

		player.yHeadRotO = playerYHeadRotO;
		player.yHeadRot = playerYHeadRot;
		player.yBodyRotO = playerYBodyRotO;
		player.yBodyRot = playerYBodyRot;
		player.xRotO = playerXRotO;
		player.setXRot(playerXRot);

		poseStack.popPose();
	}
}
