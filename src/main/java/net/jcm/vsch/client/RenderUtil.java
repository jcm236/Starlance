/**
 * Copyright 2024 authors of AdvancedPeripherals
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by zyxkad, 2025
 */
package net.jcm.vsch.client;

import net.jcm.vsch.api.resource.ModelTextures;
import net.jcm.vsch.api.resource.TextureLocation;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class RenderUtil {
	private RenderUtil() {}

	public static void drawBox(PoseStack poseStack, VertexConsumer buffer, BoxLightMap lightMap, Vector4f rgba, Vector3i offseti, Quaternionf rot, Vector3i sizei) {
		poseStack.pushPose();
		// Sizes are in pixels
		Vector3f offset = new Vector3f(offseti).div(16);
		Vector3f size = new Vector3f(sizei).div(16);

		poseStack.translate(0.5f, 0.5f, 0.5f);
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

		posestack.translate(offset.x, offset.y, offset.z);

		Matrix4f matrix4f = posestack.last().pose();

		float sX = size.x, sY = size.y, sZ = size.z;
		sX /= 2;
		sY /= 2;
		sZ /= 2;

		final float r = rgba.x, g = rgba.y, b = rgba.z, a = rgba.w;

		switch (perspective) {
			case UP -> {
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv2(lightMap.usw).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv2(lightMap.use).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.une).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.unw).endVertex();
			}
			case DOWN -> {
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.dsw).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.dnw).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.dne).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.dse).endVertex();
			}
			case SOUTH -> {
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.sdw).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.sde).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv2(lightMap.suw).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv2(lightMap.sue).endVertex();
			}
			case NORTH -> {
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.nde).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.nue).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.nuw).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.ndw).endVertex();
			}
			case EAST -> {
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.edn).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.eun).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv2(lightMap.eus).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.eds).endVertex();
			}
			case WEST -> {
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv2(lightMap.wdn).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv2(lightMap.wds).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv2(lightMap.wus).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv2(lightMap.wun).endVertex();
			}
		}
		posestack.popPose();
	}

	public static void drawBoxWithTexture(PoseStack poseStack, VertexConsumer buffer, BoxLightMap lightMap, ModelTextures model, Vector3f rgb, Vector3f offset, Quaternionf rot, Vector3i size, float scale) {
		drawBoxWithTexture(poseStack, buffer, lightMap, model, new Vector4f(rgb, 1f), offset, rot, size, scale);
	}

	public static void drawBoxWithTexture(PoseStack poseStack, VertexConsumer buffer, BoxLightMap lightMap, ModelTextures model, Vector4f rgba, Vector3f offset, Quaternionf rot, Vector3i size, float scale) {
		poseStack.pushPose();

		poseStack.translate(0.5f, 0.5f, 0.5f);
		poseStack.mulPose(rot);

		for (final Direction dir : Direction.values()) {
			drawPlaneWithTexture(poseStack, buffer, lightMap, model.getTexture(dir), rgba, dir, offset, size, scale);
		}
		poseStack.popPose();
	}

	public static void drawPlaneWithTexture(PoseStack poseStack, VertexConsumer buffer, BoxLightMap lightMap, TextureLocation texture, Vector3f rgb, Direction perspective, Vector3f offset, Vector3i size, float scale) {
		drawPlaneWithTexture(poseStack, buffer, lightMap, texture, new Vector4f(rgb, 1f), perspective, offset, size, scale);
	}

	public static void drawPlaneWithTexture(PoseStack poseStack, VertexConsumer buffer, BoxLightMap lightMap, TextureLocation texture, Vector4f rgba, Direction perspective, Vector3f offset, Vector3i size, float scale) {
		poseStack.pushPose();

		poseStack.translate(offset.x, offset.y, offset.z);

		Matrix4f matrix4f = poseStack.last().pose();

		float sX = size.x, sY = size.y, sZ = size.z;
		sX *= scale / 16f / 2;
		sY *= scale / 16f / 2;
		sZ *= scale / 16f / 2;

		final float r = rgba.x, g = rgba.y, b = rgba.z, a = rgba.w;

		final TextureAtlasSprite stillTexture = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture.location());
		final float textureScale = texture.scale();
		final double pUOffset = texture.offsetX() * textureScale, pVOffset = texture.offsetY() * textureScale;
		final float u1 = stillTexture.getU(pUOffset);
		final float v1 = stillTexture.getV(pVOffset);

		switch (perspective) {
			case UP -> {
				final float u2 = stillTexture.getU(pUOffset + size.z * textureScale);
				final float v2 = stillTexture.getV(pVOffset + size.x * textureScale);
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.unw).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.usw).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.use).normal(0f, 1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.une).normal(0f, 1f, 0f).endVertex();
			}
			case DOWN -> {
				final float u2 = stillTexture.getU(pUOffset + size.z * textureScale);
				final float v2 = stillTexture.getV(pVOffset + size.x * textureScale);
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dnw).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dne).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dse).normal(0f, -1f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.dsw).normal(0f, -1f, 0f).endVertex();
			}
			case SOUTH -> {
				final float u2 = stillTexture.getU(pUOffset + size.x * textureScale);
				final float v2 = stillTexture.getV(pVOffset + size.y * textureScale);
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.sde).normal(0f, 0f, 1f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.sdw).normal(0f, 0f, 1f).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.sue).normal(0f, 0f, 1f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.suw).normal(0f, 0f, 1f).endVertex();
			}
			case NORTH -> {
				final float u2 = stillTexture.getU(pUOffset + size.x * textureScale);
				final float v2 = stillTexture.getV(pVOffset + size.y * textureScale);
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.ndw).normal(0f, 0f, -1f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.nuw).normal(0f, 0f, -1f).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.nue).normal(0f, 0f, -1f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.nde).normal(0f, 0f, -1f).endVertex();
			}
			case EAST -> {
				final float u2 = stillTexture.getU(pUOffset + size.y * textureScale);
				final float v2 = stillTexture.getV(pVOffset + size.z * textureScale);
				buffer.vertex(matrix4f, sX, -sY, -sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.edn).normal(1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.eun).normal(1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, sY, sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.eus).normal(1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, sX, -sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.eds).normal(1f, 0f, 0f).endVertex();
			}
			case WEST -> {
				final float u2 = stillTexture.getU(pUOffset + size.y * textureScale);
				final float v2 = stillTexture.getV(pVOffset + size.z * textureScale);
				buffer.vertex(matrix4f, -sX, -sY, -sZ).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.wdn).normal(-1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, -sY, sZ).color(r, g, b, a).uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.wds).normal(-1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, sZ).color(r, g, b, a).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.wus).normal(-1f, 0f, 0f).endVertex();
				buffer.vertex(matrix4f, -sX, sY, -sZ).color(r, g, b, a).uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightMap.wun).normal(-1f, 0f, 0f).endVertex();
			}
		}
		poseStack.popPose();
	}

	public static final class BoxLightMap {
		public int use, usw, une, unw, dse, dsw, dne, dnw;
		public int sue, suw, nue, nuw, sde, sdw, nde, ndw;
		public int eus, wus, eun, wun, eds, wds, edn, wdn;

		public BoxLightMap setAll(final int packedLight) {
			this.use = this.usw = this.une = this.unw = this.dse = this.dsw = this.dne = this.dnw =
			this.sue = this.suw = this.nue = this.nuw = this.sde = this.sdw = this.nde = this.ndw =
			this.eus = this.wus = this.eun = this.wun = this.eds = this.wds = this.edn = this.wdn =
				packedLight;
			return this;
		}

		public BoxLightMap setCorners(final int use, final int usw, final int une, final int unw, final int dse, final int dsw, final int dne, final int dnw) {
			this.use = this.sue = this.eus = use;
			this.usw = this.suw = this.wus = usw;
			this.une = this.nue = this.eun = une;
			this.unw = this.nuw = this.wun = unw;
			this.dse = this.sde = this.eds = dse;
			this.dsw = this.sdw = this.wds = dsw;
			this.dne = this.nde = this.edn = dne;
			this.dnw = this.ndw = this.wdn = dnw;
			return this;
		}

		public BoxLightMap setUSE(final int value) {
			this.use = this.sue = this.eus = value;
			return this;
		}

		public BoxLightMap setUSW(final int value) {
			this.usw = this.suw = this.wus = value;
			return this;
		}

		public BoxLightMap setUNE(final int value) {
			this.une = this.nue = this.eun = value;
			return this;
		}

		public BoxLightMap setUNW(final int value) {
			this.unw = this.nuw = this.wun = value;
			return this;
		}

		public BoxLightMap setDSE(final int value) {
			this.dse = this.sde = this.eds = value;
			return this;
		}

		public BoxLightMap setDSW(final int value) {
			this.dsw = this.sdw = this.wds = value;
			return this;
		}

		public BoxLightMap setDNE(final int value) {
			this.dne = this.nde = this.edn = value;
			return this;
		}

		public BoxLightMap setDNW(final int value) {
			this.dnw = this.ndw = this.wdn = value;
			return this;
		}

		public BoxLightMap setFaces(final int up, final int down, final int south, final int north, final int east, final int west) {
			this.use = this.usw = this.une = this.unw = up;
			this.dse = this.dsw = this.dne = this.dnw = down;
			this.sue = this.suw = this.sde = this.sdw = south;
			this.nue = this.nuw = this.nde = this.ndw = south;
			this.eus = this.eun = this.eds = this.edn = east;
			this.wus = this.wun = this.wds = this.wdn = west;
			return this;
		}

		public BoxLightMap setFace(final Direction face, final int light) {
			switch (face) {
				case UP -> this.use = this.usw = this.une = this.unw = light;
				case DOWN -> this.dse = this.dsw = this.dne = this.dnw = light;
				case SOUTH -> this.sue = this.suw = this.sde = this.sdw = light;
				case NORTH -> this.nue = this.nuw = this.nde = this.ndw = light;
				case EAST -> this.eus = this.eun = this.eds = this.edn = light;
				case WEST -> this.wus = this.wun = this.wds = this.wdn = light;
			}
			return this;
		}

		public BoxLightMap packLightMaps(final BoxLightMap block, final BoxLightMap sky) {
			this.use = LightTexture.pack(block.use, sky.use);
			this.usw = LightTexture.pack(block.usw, sky.usw);
			this.une = LightTexture.pack(block.une, sky.une);
			this.unw = LightTexture.pack(block.unw, sky.unw);
			this.dse = LightTexture.pack(block.dse, sky.dse);
			this.dsw = LightTexture.pack(block.dsw, sky.dsw);
			this.dne = LightTexture.pack(block.dne, sky.dne);
			this.dnw = LightTexture.pack(block.dnw, sky.dnw);
			this.sue = LightTexture.pack(block.sue, sky.sue);
			this.suw = LightTexture.pack(block.suw, sky.suw);
			this.nue = LightTexture.pack(block.nue, sky.nue);
			this.nuw = LightTexture.pack(block.nuw, sky.nuw);
			this.sde = LightTexture.pack(block.sde, sky.sde);
			this.sdw = LightTexture.pack(block.sdw, sky.sdw);
			this.nde = LightTexture.pack(block.nde, sky.nde);
			this.ndw = LightTexture.pack(block.ndw, sky.ndw);
			this.eus = LightTexture.pack(block.eus, sky.eus);
			this.wus = LightTexture.pack(block.wus, sky.wus);
			this.eun = LightTexture.pack(block.eun, sky.eun);
			this.wun = LightTexture.pack(block.wun, sky.wun);
			this.eds = LightTexture.pack(block.eds, sky.eds);
			this.wds = LightTexture.pack(block.wds, sky.wds);
			this.edn = LightTexture.pack(block.edn, sky.edn);
			this.wdn = LightTexture.pack(block.wdn, sky.wdn);
			return this;
		}

		public BoxLightMap getBlockLightMap() {
			final BoxLightMap block = new BoxLightMap();
			block.use = LightTexture.block(this.use);
			block.usw = LightTexture.block(this.usw);
			block.une = LightTexture.block(this.une);
			block.unw = LightTexture.block(this.unw);
			block.dse = LightTexture.block(this.dse);
			block.dsw = LightTexture.block(this.dsw);
			block.dne = LightTexture.block(this.dne);
			block.dnw = LightTexture.block(this.dnw);
			block.sue = LightTexture.block(this.sue);
			block.suw = LightTexture.block(this.suw);
			block.nue = LightTexture.block(this.nue);
			block.nuw = LightTexture.block(this.nuw);
			block.sde = LightTexture.block(this.sde);
			block.sdw = LightTexture.block(this.sdw);
			block.nde = LightTexture.block(this.nde);
			block.ndw = LightTexture.block(this.ndw);
			block.eus = LightTexture.block(this.eus);
			block.wus = LightTexture.block(this.wus);
			block.eun = LightTexture.block(this.eun);
			block.wun = LightTexture.block(this.wun);
			block.eds = LightTexture.block(this.eds);
			block.wds = LightTexture.block(this.wds);
			block.edn = LightTexture.block(this.edn);
			block.wdn = LightTexture.block(this.wdn);
			return block;
		}

		public BoxLightMap getSkyLightMap() {
			final BoxLightMap sky = new BoxLightMap();
			sky.use = LightTexture.sky(this.use);
			sky.usw = LightTexture.sky(this.usw);
			sky.une = LightTexture.sky(this.une);
			sky.unw = LightTexture.sky(this.unw);
			sky.dse = LightTexture.sky(this.dse);
			sky.dsw = LightTexture.sky(this.dsw);
			sky.dne = LightTexture.sky(this.dne);
			sky.dnw = LightTexture.sky(this.dnw);
			sky.sue = LightTexture.sky(this.sue);
			sky.suw = LightTexture.sky(this.suw);
			sky.nue = LightTexture.sky(this.nue);
			sky.nuw = LightTexture.sky(this.nuw);
			sky.sde = LightTexture.sky(this.sde);
			sky.sdw = LightTexture.sky(this.sdw);
			sky.nde = LightTexture.sky(this.nde);
			sky.ndw = LightTexture.sky(this.ndw);
			sky.eus = LightTexture.sky(this.eus);
			sky.wus = LightTexture.sky(this.wus);
			sky.eun = LightTexture.sky(this.eun);
			sky.wun = LightTexture.sky(this.wun);
			sky.eds = LightTexture.sky(this.eds);
			sky.wds = LightTexture.sky(this.wds);
			sky.edn = LightTexture.sky(this.edn);
			sky.wdn = LightTexture.sky(this.wdn);
			return sky;
		}
	}
}
