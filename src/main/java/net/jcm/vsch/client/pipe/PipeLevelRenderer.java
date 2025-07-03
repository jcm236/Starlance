package net.jcm.vsch.client.pipe;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.accessor.INodeLevelChunkSection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.client.RenderUtil;
import net.jcm.vsch.items.custom.WrenchItem;
import net.jcm.vsch.pipe.level.NodeGetter;
import net.jcm.vsch.pipe.level.NodeLevel;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = VSCHMod.MODID, value = Dist.CLIENT)
public class PipeLevelRenderer {
	private static final Vector3i ZERO_VEC3I = new Vector3i();
	private static final int PIPE_VIEW_RANGE = 8;

	private static final Vector4f HINT_COLOR = new Vector4f(0.25f, 0.70f, 0.25f, 0.6f);
	private static final float HINT_SCALE = 0.7f;
	private static final Vector4f HINT_SELECTING_COLOR = new Vector4f(0.25f, 0.92f, 0.25f, 0.8f);
	private static final float HINT_SELECTING_SCALE = 0.85f;

	@SubscribeEvent
	public static void renderLevelState(final RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			return;
		}
		final Minecraft minecraft = Minecraft.getInstance();
		final ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}

		final PoseStack poseStack = event.getPoseStack();
		final float partialTick = event.getPartialTick();
		final Vec3 view = event.getCamera().getPosition();
		// final Frustum frustum = event.getFrustum(); // TODO: see if frustum filter can improve performance

		final MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		// TODO: make our own atlas
		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
		RenderSystem.setShader(GameRenderer::getRendertypeTranslucentShader);

		final VertexConsumer vertexBuilder = bufferSource.getBuffer(RenderType.translucent());

		poseStack.pushPose();

		renderPlaceHint(level, poseStack, vertexBuilder, view, partialTick);
		renderNodes(level, poseStack, vertexBuilder, view);

		bufferSource.endBatch();
		poseStack.popPose();
		RenderSystem.disableBlend();
	}

	private static Stream<LevelChunk> streamRenderingChunks(final ClientLevel level, final Vec3 view) {
		final ClientChunkCache chunkSource = level.getChunkSource();
		final ChunkPos center = new ChunkPos(BlockPos.containing(view));
		Stream<ChunkPos> chunkPosStream = ChunkPos.rangeClosed(center, PIPE_VIEW_RANGE);
		// TODO: concat VS ship chunks
		return chunkPosStream.map((chunkPos) -> chunkSource.getChunkNow(chunkPos.x, chunkPos.z)).filter(Objects::nonNull);
	}

	private static void renderPlaceHint(final ClientLevel level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final Vec3 view, final float partialTick) {
		final Minecraft minecraft = Minecraft.getInstance();
		final LocalPlayer player = minecraft.player;
		if (player == null || player.isSpectator()) {
			return;
		}
		if (!(player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof WrenchItem)) {
			return;
		}
		final HitResult hit = player.pick(4.5, partialTick, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		final BlockHitResult blockHit = (BlockHitResult) (hit);
		final NodePos lookingPos = NodePos.fromHitResult(level, blockHit.getBlockPos(), blockHit.getLocation(), 4.0 / 16);
		NodePos.streamPlaceHint(NodeLevel.get(level), blockHit.getBlockPos()).forEach((pos) -> {
			renderNodePlaceHint(
				level, poseStack, vertexBuilder, view, pos,
				pos.equals(lookingPos) ? HINT_SELECTING_COLOR : HINT_COLOR,
				pos.equals(lookingPos) ? HINT_SELECTING_SCALE : HINT_SCALE
			);
		});
	}

	private static void renderNodes(final ClientLevel level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final Vec3 view) {
		streamRenderingChunks(level, view).forEach((chunk) -> renderNodesInChunk(level, chunk, poseStack, vertexBuilder, view));
	}

	private static void renderNodesInChunk(final ClientLevel level, final ChunkAccess chunk, final PoseStack poseStack, final VertexConsumer vertexBuilder, final Vec3 view) {
		if (!(chunk instanceof NodeGetter nodeGetter)) {
			return;
		}
		if (!nodeGetter.hasAnyNode()) {
			return;
		}
		final ChunkPos chunkPos = chunk.getPos();
		int sectionIndex = 0;
		for (final LevelChunkSection section : chunk.getSections()) {
			sectionIndex++;
			if (!(section instanceof INodeLevelChunkSection nodeSection)) {
				continue;
			}
			if (!nodeSection.vsch$hasAnyNode()) {
				continue;
			}
			final int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex - 1);
			final SectionPos sectionPos = SectionPos.of(chunkPos, sectionY);
			final BlockPos sectionOrigin = sectionPos.origin();
			for (int index = 0; index < 16 * 16 * 16; index++) {
				final int x = index & 0xf, y = (index >> 8) & 0xf, z = (index >> 4) & 0xf;
				PipeNode[] nodes = nodeSection.vsch$getNodes(x, y, z);
				if (nodes == null) {
					continue;
				}
				for (int i = 0; i < NodePos.UNIQUE_INDEX_BOUND; i++) {
					final PipeNode node = nodes[i];
					if (node != null) {
						renderNode(level, poseStack, vertexBuilder, view, NodePos.fromUniqueIndex(sectionOrigin.offset(x, y, z), i), node);
					}
				}
			}
		}
	}

	private static void renderNode(final BlockAndTintGetter level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final Vec3 view, final NodePos pos, final PipeNode node) {
		final int size = 4;
		final Vec3 nodeCenter = pos.getCenter();
		final RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap();
		final double r = size / 2.0 / 16;
		lightMap.use = LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.add(r, r, r)));
		lightMap.usw = LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.add(-r, r, r)));
		lightMap.une = LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.add(r, r, -r)));
		lightMap.unw = LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.add(-r, r, -r)));
		lightMap.dse = LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.add(r, -r, r)));
		lightMap.dsw = LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.add(-r, -r, r)));
		lightMap.dne = LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.add(r, -r, -r)));
		lightMap.dnw = LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.add(-r, -r, -r)));

		final RenderUtil.BoxLightMap blockLightMap = lightMap.getBlockLightMap();
		final RenderUtil.BoxLightMap skyLightMap = lightMap.getSkyLightMap();

		for (final RenderUtil.BoxLightMap lights : new RenderUtil.BoxLightMap[]{blockLightMap, skyLightMap}) {
			for (int i = 0; i < 2; i++) {
				blockLightMap.use = Math.max(blockLightMap.use, Math.max(Math.max(blockLightMap.usw, blockLightMap.une), blockLightMap.dse) - 2);
				blockLightMap.usw = Math.max(blockLightMap.usw, Math.max(Math.max(blockLightMap.use, blockLightMap.unw), blockLightMap.dsw) - 2);
				blockLightMap.une = Math.max(blockLightMap.une, Math.max(Math.max(blockLightMap.unw, blockLightMap.use), blockLightMap.dne) - 2);
				blockLightMap.unw = Math.max(blockLightMap.unw, Math.max(Math.max(blockLightMap.une, blockLightMap.usw), blockLightMap.dnw) - 2);
				blockLightMap.dse = Math.max(blockLightMap.dse, Math.max(Math.max(blockLightMap.dsw, blockLightMap.dne), blockLightMap.use) - 2);
				blockLightMap.dsw = Math.max(blockLightMap.dsw, Math.max(Math.max(blockLightMap.dse, blockLightMap.dnw), blockLightMap.usw) - 2);
				blockLightMap.dne = Math.max(blockLightMap.dne, Math.max(Math.max(blockLightMap.dnw, blockLightMap.dse), blockLightMap.une) - 2);
				blockLightMap.dnw = Math.max(blockLightMap.dnw, Math.max(Math.max(blockLightMap.dne, blockLightMap.dsw), blockLightMap.unw) - 2);
			}
		}

		lightMap.packLightMaps(blockLightMap, skyLightMap);

		poseStack.pushPose();

		poseStack.translate(nodeCenter.x - 0.5 - view.x, nodeCenter.y - 0.5 - view.y, nodeCenter.z - 0.5 - view.z);
		ResourceLocation nodeTexture = new ResourceLocation(VSCHMod.MODID, "block/pipe/omni_node");
		RenderUtil.drawBoxWithTexture(
			poseStack, vertexBuilder,
			lightMap,
			nodeTexture, new Vector3f(node.getColor().getTextureDiffuseColors()),
			ZERO_VEC3I, new Quaternionf(), new Vector3i(size, size, size),
			0, 0,
			1f
		);

		poseStack.popPose();
	}

	private static void renderNodePlaceHint(final BlockAndTintGetter level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final Vec3 view, final NodePos pos, final Vector4f color, final float scale) {
		final int size = 4;
		final Vec3 nodeCenter = pos.getCenter();
		final RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap().setAll(0xf000f0);

		poseStack.pushPose();

		poseStack.translate(nodeCenter.x - 0.5 - view.x, nodeCenter.y - 0.5 - view.y, nodeCenter.z - 0.5 - view.z);
		ResourceLocation nodeTexture = new ResourceLocation(VSCHMod.MODID, "block/pipe/omni_node");
		RenderUtil.drawBoxWithTexture(
			poseStack, vertexBuilder,
			lightMap,
			nodeTexture, color,
			ZERO_VEC3I, new Quaternionf(), new Vector3i(size, size, size),
			0, 0,
			scale
		);

		poseStack.popPose();
	}
}
