package net.jcm.vsch.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.jcm.vsch.blocks.entity.laser.ScreenBlockEntity;

public class ScreenBlockRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
	private final BlockEntityRendererProvider.Context ctx;

	public ScreenBlockRenderer(BlockEntityRendererProvider.Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void render(ScreenBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		final Vec3 color = be.getColor();
		final Vector4f color4 = new Vector4f((float) color.x, (float) color.y, (float) color.z, 1);

		poseStack.pushPose();
		final VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());
		final PoseStack.Pose posestackPose = poseStack.last();
		final Matrix4f pose = posestackPose.pose();
		final Matrix3f normal = posestackPose.normal();
		addVertex(buffer, pose, 0, 0, 0, color4, 0, 0, normal, 0, 0, -1);
		addVertex(buffer, pose, 1, 0, 0, color4, 1, 0, normal, 0, 0, -1);
		addVertex(buffer, pose, 1, 1, 0, color4, 1, 1, normal, 0, 0, -1);
		addVertex(buffer, pose, 0, 1, 0, color4, 0, 1, normal, 0, 0, -1);

		addVertex(buffer, pose, 0, 0, 1, color4, 0, 0, normal, 0, 0, 1);
		addVertex(buffer, pose, 1, 0, 1, color4, 1, 0, normal, 0, 0, 1);
		addVertex(buffer, pose, 1, 1, 1, color4, 1, 1, normal, 0, 0, 1);
		addVertex(buffer, pose, 0, 1, 1, color4, 0, 1, normal, 0, 0, 1);

		addVertex(buffer, pose, 0, 0, 0, color4, 0, 0, normal, -1, 0, 0);
		addVertex(buffer, pose, 0, 0, 1, color4, 1, 0, normal, -1, 0, 0);
		addVertex(buffer, pose, 0, 1, 1, color4, 1, 1, normal, -1, 0, 0);
		addVertex(buffer, pose, 0, 1, 0, color4, 0, 1, normal, -1, 0, 0);

		addVertex(buffer, pose, 1, 0, 0, color4, 0, 0, normal, 1, 0, 0);
		addVertex(buffer, pose, 1, 0, 1, color4, 1, 0, normal, 1, 0, 0);
		addVertex(buffer, pose, 1, 1, 1, color4, 1, 1, normal, 1, 0, 0);
		addVertex(buffer, pose, 1, 1, 0, color4, 0, 1, normal, 1, 0, 0);

		addVertex(buffer, pose, 0, 0, 0, color4, 0, 0, normal, 0, -1 ,0);
		addVertex(buffer, pose, 1, 0, 0, color4, 1, 0, normal, 0, -1 ,0);
		addVertex(buffer, pose, 1, 0, 1, color4, 1, 1, normal, 0, -1 ,0);
		addVertex(buffer, pose, 0, 0, 1, color4, 0, 1, normal, 0, -1 ,0);

		addVertex(buffer, pose, 0, 1, 0, color4, 0, 0, normal, 0, 1 ,0);
		addVertex(buffer, pose, 1, 1, 0, color4, 1, 0, normal, 0, 1 ,0);
		addVertex(buffer, pose, 1, 1, 1, color4, 1, 1, normal, 0, 1 ,0);
		addVertex(buffer, pose, 0, 1, 1, color4, 0, 1, normal, 0, 1 ,0);
		poseStack.popPose();
	}

	private static void addVertex(VertexConsumer buffer, Matrix4f pose, float x, float y, float z, Vector4f color, int u, int v, Matrix3f normal, float normalX, float normalY, float normalZ) {
		buffer.vertex(pose, x, y, z).color(color.x, color.y, color.z, color.w).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00f000f0).normal(normal, normalX, normalY, normalZ).endVertex();
	}
}
