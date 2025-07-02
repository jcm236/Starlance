package net.jcm.vsch.client.pipe;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.client.RenderUtil;

import org.joml.Quaternionf;
import org.joml.Vector3i;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.resources.ResourceLocation;

@Mod.EventBusSubscriber(modid = VSCHMod.MODID, value = Dist.CLIENT)
public class PipeLevelRenderer {
	@SubscribeEvent
	public static void renderLevelState(final RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}

		final PoseStack poseStack = event.getPoseStack();
		final Tesselator tesselator = Tesselator.getInstance();
		final BufferBuilder bufferBuilder = tesselator.getBuilder();
		final Vec3 view = event.getCamera().getPosition();

		BlockPos blockPos = new BlockPos(0, 10, 0);

		RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
		poseStack.pushPose();

		renderNodes(poseStack, bufferBuilder, view);
		poseStack.pushPose();
		Vector3f color = new Vector3f(1, 1, 1);
		poseStack.translate(blockPos.getX() - view.x, blockPos.getY() - view.y, blockPos.getZ() - view.z);
		RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap();
		lightMap.setAll(0x0000f0);
		RenderUtil.drawBoxWithTexture(poseStack, bufferBuilder, lightMap, new ResourceLocation("minecraft", "block/stone"), color, new Vector3i(8, 8, 8), new Quaternionf(), new Vector3i(2, 2, 2), 1, 1);
		lightMap.setAll(0x000000);
		color = new Vector3f(1, 0, 1);
		RenderUtil.drawBoxWithTexture(poseStack, bufferBuilder, lightMap, new ResourceLocation("minecraft", "block/stone"), color, new Vector3i(8, 0, 8), new Quaternionf(), new Vector3i(2, 14, 2), 1, 1);
		poseStack.popPose();

		tesselator.end();
		poseStack.popPose();
	}

	private static void renderNodes(final PoseStack poseStack, final BufferBuilder bufferBuilder, final Vec3 view) {
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 12, 0), Direction.Axis.Y, 0), DyeColor.MAGENTA);
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 12, 0), Direction.Axis.Y, 1), DyeColor.PURPLE);
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 12, 0), Direction.Axis.Y, 2), DyeColor.BLUE);
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 12, 0), Direction.Axis.Y, 3), DyeColor.CYAN);
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 12, 0), Direction.Axis.Y, 4), DyeColor.GREEN);
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 12, 0), Direction.Axis.Y, 5), DyeColor.LIME);
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 12, 0), Direction.Axis.Y, 6), DyeColor.YELLOW);
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 12, 0), Direction.Axis.Y, 7), DyeColor.ORANGE);
		renderNode(poseStack, bufferBuilder, view, new NodePos(new BlockPos(0, 13, 0), Direction.Axis.Y, 0), DyeColor.RED);

		renderNode4(poseStack, bufferBuilder, view, new NodePos(new BlockPos(3, 12, 0), Direction.Axis.Y, 0), DyeColor.MAGENTA);
		renderNode4(poseStack, bufferBuilder, view, new NodePos(new BlockPos(3, 12, 0), Direction.Axis.Y, 2), DyeColor.BLUE);
		renderNode4(poseStack, bufferBuilder, view, new NodePos(new BlockPos(3, 12, 0), Direction.Axis.Y, 4), DyeColor.GREEN);
		renderNode4(poseStack, bufferBuilder, view, new NodePos(new BlockPos(3, 12, 0), Direction.Axis.Y, 6), DyeColor.YELLOW);
		renderNode4(poseStack, bufferBuilder, view, new NodePos(new BlockPos(3, 13, 0), Direction.Axis.Y, 0), DyeColor.RED);
	}

	private static void renderNode(final PoseStack poseStack, final BufferBuilder bufferBuilder, final Vec3 view, final NodePos pos, final DyeColor color) {
		poseStack.pushPose();

		final Vec3 nodeCenter = pos.getCenter();
		poseStack.translate(nodeCenter.x - 0.5 - view.x, nodeCenter.y - 0.5 - view.y, nodeCenter.z - 0.5 - view.z);
		RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap();
		// TODO: fix uv2 for lighting
		lightMap.setAll(0xf000f0);
		ResourceLocation nodeTexture = new ResourceLocation(VSCHMod.MODID, "block/pipe/omni_node");
		RenderUtil.drawBoxWithTexture(
			poseStack, bufferBuilder,
			lightMap,
			nodeTexture, new Vector3f(color.getTextureDiffuseColors()),
			new Vector3i(), new Quaternionf(), new Vector3i(2, 2, 2),
			0, 0
		);

		poseStack.popPose();
	}

	private static void renderNode4(final PoseStack poseStack, final BufferBuilder bufferBuilder, final Vec3 view, final NodePos pos, final DyeColor color) {
		poseStack.pushPose();

		final Vec3 nodeCenter = pos.getCenter();
		poseStack.translate(nodeCenter.x - 0.5 - view.x, nodeCenter.y - 0.5 - view.y, nodeCenter.z - 0.5 - view.z);
		RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap();
		// TODO: fix uv2 for lighting
		lightMap.setAll(0xf000f0);
		ResourceLocation nodeTexture = new ResourceLocation(VSCHMod.MODID, "block/pipe/omni_node");
		RenderUtil.drawBoxWithTexture(
			poseStack, bufferBuilder,
			lightMap,
			nodeTexture, new Vector3f(color.getTextureDiffuseColors()),
			new Vector3i(), new Quaternionf(), new Vector3i(4, 4, 4),
			0, 0
		);

		poseStack.popPose();
	}
}
