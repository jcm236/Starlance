package net.jcm.vsch.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import net.jcm.vsch.api.laser.ILaserSource;
import net.jcm.vsch.api.laser.LaserContext;

import java.util.List;

public class LaserRenderer implements BlockEntityRenderer<BlockEntity> {
	private final BlockEntityRendererProvider.Context ctx;

	public LaserRenderer(BlockEntityRendererProvider.Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void render(BlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!(be instanceof ILaserSource source)) {
			return;
		}
		final List<LaserContext> lasers = source.getEmittingLasers();
		for (final LaserContext laser : lasers) {
			System.out.println("rendering laser: " + laser);
		}
	}

	@Override
	public boolean shouldRenderOffScreen(BlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}

	@Override
	public boolean shouldRender(BlockEntity be, Vec3 cameraPos) {
		return true;
	}

}
