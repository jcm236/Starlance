package net.jcm.vsch.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

// Highly inspired by AdvancedPeripherals
public final class RenderUtil {
	private RenderUtil() {}

	public static void drawBox(PoseStack poseStack, VertexConsumer buffer, BoxLightMap lightMap, Vector4f rgba, Vector3i offseti, Quaternionf rot, Vector3i sizei) {
		poseStack.pushPose();
		// Sizes are in pixels
		Vector3f offset = new Vector3f(offseti).div(16);
		Vector3f size = new Vector3f(sizei).div(16);

		poseStack.mulPose(rot);

		drawPlane(poseStack, buffer, lightMap, rgba, Direction.UP, offset, size);
		drawPlane(poseStack, buffer, lightMap, rgba, Direction.DOWN, offset, size);
		drawPlane(poseStack, buffer, lightMap, rgba, Direction.EAST, offset, size);
		drawPlane(poseStack, buffer, lightMap, rgba, Direction.WEST, offset, size);
		drawPlane(poseStack, buffer, lightMap, rgba, Direction.NORTH, offset, size);
		drawPlane(poseStack, buffer, lightMap, rgba, Direction.SOUTH, offset, size);
		poseStack.popPose();
	}

	public static void drawPlane(PoseStack posestack, VertexConsumer buffer, BoxLightMap lightMap, Vector4f rgba, Direction perspective, Vector3f offset, Vector3f size) {
		posestack.pushPose();

		float pX = offset.x, pY = offset.y, pZ = offset.z;
		pX += 0.5f;
		pY += 0.5f;
		pZ += 0.5f;

		posestack.translate(pX, pY, pZ);

		Matrix4f matrix4f = posestack.last().pose();

		float sX = size.x, sY = size.y, sZ = size.z;
		sX /= 2;
		sY /= 2;
		sZ /= 2;

		final float r = rgba.x, g = rgba.y, b = rgba.z, a = rgba.w;

		switch (perspective) {
			case UP -> {
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv2(lightMap.unw).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv2(lightMap.une).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.use).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.usw).endVertex();
			}
			case DOWN -> {
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.dnw).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.dsw).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.dse).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.dne).endVertex();
			}
			case SOUTH -> {
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.dne).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.dse).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.use).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv2(lightMap.une).endVertex();
			}
			case NORTH -> {
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.dnw).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv2(lightMap.unw).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.usw).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.dsw).endVertex();
			}
			case EAST -> {
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.dsw).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.usw).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.use).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.dse).endVertex();
			}
			case WEST -> {
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.dnw).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.dne).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv2(lightMap.une).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv2(lightMap.unw).endVertex();
			}
		}
		posestack.popPose();
	}

	public static void drawBoxWithTexture(PoseStack poseStack, VertexConsumer buffer, BoxLightMap lightMap, ResourceLocation texture, Vector3f rgb, Vector3i offseti, Quaternionf rot, Vector3i sizei, float pUOffset, float pVOffset) {
		poseStack.pushPose();
		// Sizes are in pixels
		Vector3f offset = new Vector3f(offseti).div(16);
		Vector3f size = new Vector3f(sizei).div(16);

		poseStack.mulPose(rot);

		drawPlaneWithTexture(poseStack, buffer, lightMap, texture, rgb, Direction.UP, offset, size, pUOffset, pVOffset);
		drawPlaneWithTexture(poseStack, buffer, lightMap, texture, rgb, Direction.DOWN, offset, size, pUOffset, pVOffset);
		drawPlaneWithTexture(poseStack, buffer, lightMap, texture, rgb, Direction.EAST, offset, size, pUOffset, pVOffset);
		drawPlaneWithTexture(poseStack, buffer, lightMap, texture, rgb, Direction.WEST, offset, size, pUOffset, pVOffset);
		drawPlaneWithTexture(poseStack, buffer, lightMap, texture, rgb, Direction.NORTH, offset, size, pUOffset, pVOffset);
		drawPlaneWithTexture(poseStack, buffer, lightMap, texture, rgb, Direction.SOUTH, offset, size, pUOffset, pVOffset);
		poseStack.popPose();
	}

	public static void drawPlaneWithTexture(PoseStack poseStack, VertexConsumer buffer, BoxLightMap lightMap, ResourceLocation texture, Vector3f rgb, Direction perspective, Vector3f offset, Vector3f size, float pUOffset, float pVOffset) {
		poseStack.pushPose();

		float pX = offset.x, pY = offset.y, pZ = offset.z;
		pX += 0.5f;
		pY += 0.5f;
		pZ += 0.5f;

		poseStack.translate(pX, pY, pZ);

		Matrix4f matrix4f = poseStack.last().pose();

		float sX = size.x, sY = size.y, sZ = size.z;
		sX /= 2;
		sY /= 2;
		sZ /= 2;

		final float r = rgb.x, g = rgb.y, b = rgb.z, a = 1f;

		TextureAtlasSprite stillTexture = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);

		final float u1 = stillTexture.getU(pUOffset);
		final float v1 = stillTexture.getV(pVOffset);

		switch (perspective) {
			case UP -> {
				final float u2 = stillTexture.getU(pUOffset + size.z * 16);
				final float v2 = stillTexture.getV(pVOffset + size.x * 16);
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.unw).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.une).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.use).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.usw).normal(0f, 1f, 0f).endVertex();
			}
			case DOWN -> {
				final float u2 = stillTexture.getU(pUOffset + size.z * 16);
				final float v2 = stillTexture.getV(pVOffset + size.x * 16);
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dnw).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dsw).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dse).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dne).normal(0f, -1f, 0f).endVertex();
			}
			case SOUTH -> {
				final float u2 = stillTexture.getU(pUOffset + size.x * 16);
				final float v2 = stillTexture.getV(pVOffset + size.y * 16);
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dne).normal(1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dse).normal(1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.use).normal(1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.une).normal(1f, 0f, 0f).endVertex();
			}
			case NORTH -> {
				final float u2 = stillTexture.getU(pUOffset + size.x * 16);
				final float v2 = stillTexture.getV(pVOffset + size.y * 16);
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dnw).normal(-1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.unw).normal(-1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.usw).normal(-1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dsw).normal(-1f, 0f, 0f).endVertex();
			}
			case EAST -> {
				final float u2 = stillTexture.getU(pUOffset + size.y * 16);
				final float v2 = stillTexture.getV(pVOffset + size.z * 16);
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dsw).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.usw).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.use).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dse).normal(0f, 1f, 0f).endVertex();
			}
			case WEST -> {
				final float u2 = stillTexture.getU(pUOffset + size.y * 16);
				final float v2 = stillTexture.getV(pVOffset + size.z * 16);
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dnw).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dne).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.une).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.unw).normal(0f, -1f, 0f).endVertex();
			}
		}
		poseStack.popPose();
	}

	public static final class BoxLightMap {
		public int use, usw, une, unw, dse, dsw, dne, dnw;

		public BoxLightMap setAll(final int packedLight) {
			return this.set(packedLight, packedLight, packedLight, packedLight, packedLight, packedLight, packedLight, packedLight);
		}

		public BoxLightMap set(final int use, final int usw, final int une, final int unw, final int dse, final int dsw, final int dne, final int dnw) {
			this.use = use;
			this.usw = usw;
			this.une = une;
			this.unw = unw;
			this.dse = dse;
			this.dsw = dsw;
			this.dne = dne;
			this.dnw = dnw;
			return this;
		}
	}
}
