package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class MixinPlayerRenderer {
	@Inject(method = "render", at = @At("HEAD"))
	public void render$before(
		final AbstractClientPlayer player,
		final float yaw,
		final float partialTick,
		final PoseStack poseStack,
		final MultiBufferSource bufferSource,
		final int packedLight,
		final CallbackInfo ci
	) {
		poseStack.pushPose();
		// poseStack.mulPose();
	}

	@Inject(method = "render", at = @At("RETURN"))
	public void render$after(
		final AbstractClientPlayer player,
		final float yaw,
		final float partialTick,
		final PoseStack poseStack,
		final MultiBufferSource bufferSource,
		final int packedLight,
		final CallbackInfo ci
	) {
		poseStack.popPose();
	}

	@Inject(method = "getRenderOffset", at = @At("RETURN"), cancellable = true)
	public void getRenderOffset(final AbstractClientPlayer player, final float partialTick, final CallbackInfoReturnable<Vec3> cir) {
		if (!(player instanceof FreeRotatePlayerAccessor frp) || !frp.vsch$shouldFreeRotate()) {
			return;
		}
		Vec3 offset = cir.getReturnValue();
		final EntityDimensions vanillaDim = frp.vsch$getVanillaDimensions(player.getPose());
		offset = offset.add(0, 0.6 - vanillaDim.height, 0);
		cir.setReturnValue(offset);
	}
}
