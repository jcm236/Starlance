package net.jcm.vsch.client.pipe;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.accessor.INodeLevelChunkSection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.api.resource.ModelTextures;
import net.jcm.vsch.api.resource.TextureLocation;
import net.jcm.vsch.client.RenderUtil;
import net.jcm.vsch.items.custom.WrenchItem;
import net.jcm.vsch.pipe.level.NodeGetter;
import net.jcm.vsch.pipe.level.NodeLevel;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.primitives.AABBi;

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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber(modid = VSCHMod.MODID, value = Dist.CLIENT)
public class PipeLevelRenderer {
	private static final Vector3f ZERO_VEC3F = new Vector3f();
	private static final int PIPE_VIEW_RANGE = 8;

	private static final Vector4f HINT_COLOR = new Vector4f(0.25f, 0.70f, 0.25f, 0.6f);
	private static final float HINT_SCALE = 0.7f;
	private static final Vector4f HINT_SELECTING_COLOR = new Vector4f(0.25f, 0.92f, 0.25f, 0.8f);
	private static final float HINT_SELECTING_SCALE = 0.85f;
	private static final ModelTextures HINT_MODEL;

	static {
		final ResourceLocation resource = new ResourceLocation(VSCHMod.MODID, "block/pipe/omni_node");
		final TextureLocation texture1 = new TextureLocation(resource, 0, 1);
		final TextureLocation texture2 = new TextureLocation(resource, 0, 0);
		HINT_MODEL = new ModelTextures(texture1, texture2, texture1, texture2, texture1, texture2);
	}

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
		final Vector3d viewBoxMin = new Vector3d(view.x - PIPE_VIEW_RANGE * 16, view.y - PIPE_VIEW_RANGE * 16, view.z - PIPE_VIEW_RANGE * 16);
		final Vector3d viewBoxMax = new Vector3d(view.x + PIPE_VIEW_RANGE * 16, view.y + PIPE_VIEW_RANGE * 16, view.z + PIPE_VIEW_RANGE * 16);
		final AABB viewBox = new AABB(viewBoxMin.x, viewBoxMin.y, viewBoxMin.z, viewBoxMax.x, viewBoxMax.y, viewBoxMax.z);
		final ClientChunkCache chunkSource = level.getChunkSource();
		final ChunkPos center = new ChunkPos(BlockPos.containing(view));

		final List<Stream<ChunkPos>> chunkPosStreams = new ArrayList<>();
		chunkPosStreams.add(ChunkPos.rangeClosed(center, PIPE_VIEW_RANGE));

		final Vector3d viewBoxMinOut = new Vector3d(), viewBoxMaxOut = new Vector3d();
		final AABBi viewBoxOut = new AABBi();
		for (final Ship ship : VSGameUtilsKt.getShipsIntersecting(level, viewBox)) {
			ship.getTransform().getWorldToShip().transformAab(viewBoxMin, viewBoxMax, viewBoxMinOut, viewBoxMaxOut);
			viewBoxOut
				.setMin((int)(viewBoxMinOut.x), (int)(viewBoxMinOut.y), (int)(viewBoxMinOut.z))
				.setMax((int)(viewBoxMaxOut.x), (int)(viewBoxMaxOut.y), (int)(viewBoxMaxOut.z));
			ship.getShipAABB().intersection(viewBoxOut, viewBoxOut);
			if (!viewBoxOut.isValid()) {
				continue;
			}
			final int minX = SectionPos.blockToSectionCoord(viewBoxOut.minX), minZ = SectionPos.blockToSectionCoord(viewBoxOut.minZ);
			final int maxX = SectionPos.blockToSectionCoord(viewBoxOut.maxX), maxZ = SectionPos.blockToSectionCoord(viewBoxOut.maxZ);
			chunkPosStreams.add(StreamSupport.stream(
				new Spliterators.AbstractSpliterator<>(
					(maxX - minX) * (maxZ - minZ),
					Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.NONNULL
				) {
					private ChunkPos pos = null;

					@Override
					public boolean tryAdvance(final Consumer<? super ChunkPos> consumer) {
						if (this.pos == null) {
							this.pos = new ChunkPos(minX, minZ);
						} else {
							int x = this.pos.x, z = this.pos.z;
							final boolean xIn = x < maxX, zIn = z < maxZ;
							if (!xIn && !zIn) {
								return false;
							}
							if (xIn) {
								x++;
							} else {
								x = 0;
								z++;
							}
							this.pos = new ChunkPos(x, z);
						}
						consumer.accept(this.pos);
						return true;
					}
				},
				false
			));
		}

		return chunkPosStreams.stream()
			.flatMap(Function.identity())
			.map((chunkPos) -> chunkSource.getChunkNow(chunkPos.x, chunkPos.z))
			.filter(Objects::nonNull);
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

	private static void renderNode(final ClientLevel level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final Vec3 view, final NodePos pos, final PipeNode node) {
		final double size = node.getSize();
		final Vec3 nodeCenter = pos.getCenter();
		final RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap();
		final double r = size / 2;
		lightMap.setUSE(LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.x + r, nodeCenter.y + r, nodeCenter.y + r)));
		lightMap.setUSW(LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.x - r, nodeCenter.y + r, nodeCenter.y + r)));
		lightMap.setUNE(LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.x + r, nodeCenter.y + r, nodeCenter.y - r)));
		lightMap.setUNW(LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.x - r, nodeCenter.y + r, nodeCenter.y - r)));
		lightMap.setDSE(LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.x + r, nodeCenter.y - r, nodeCenter.y + r)));
		lightMap.setDSW(LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.x - r, nodeCenter.y - r, nodeCenter.y + r)));
		lightMap.setDNE(LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.x + r, nodeCenter.y - r, nodeCenter.y - r)));
		lightMap.setDNW(LevelRenderer.getLightColor(level, BlockPos.containing(nodeCenter.x - r, nodeCenter.y - r, nodeCenter.y - r)));

		final RenderUtil.BoxLightMap blockLightMap = lightMap.getBlockLightMap();
		final RenderUtil.BoxLightMap skyLightMap = lightMap.getSkyLightMap();

		for (final RenderUtil.BoxLightMap lights : new RenderUtil.BoxLightMap[]{blockLightMap, skyLightMap}) {
			for (int i = 0; i < 2; i++) {
				lights.setUSE(Math.max(lights.use, Math.max(Math.max(lights.usw, lights.une), lights.dse) - 2));
				lights.setUSW(Math.max(lights.usw, Math.max(Math.max(lights.use, lights.unw), lights.dsw) - 2));
				lights.setUNE(Math.max(lights.une, Math.max(Math.max(lights.unw, lights.use), lights.dne) - 2));
				lights.setUNW(Math.max(lights.unw, Math.max(Math.max(lights.une, lights.usw), lights.dnw) - 2));
				lights.setDSE(Math.max(lights.dse, Math.max(Math.max(lights.dsw, lights.dne), lights.use) - 2));
				lights.setDSW(Math.max(lights.dsw, Math.max(Math.max(lights.dse, lights.dnw), lights.usw) - 2));
				lights.setDNE(Math.max(lights.dne, Math.max(Math.max(lights.dnw, lights.dse), lights.une) - 2));
				lights.setDNW(Math.max(lights.dnw, Math.max(Math.max(lights.dne, lights.dsw), lights.unw) - 2));
			}
		}

		lightMap.packLightMaps(blockLightMap, skyLightMap);

		Vec3 nodeCenterRender = nodeCenter.subtract(0.5, 0.5, 0.5);
		final Quaternionf rotation = new Quaternionf();
		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos.blockPos());
		if (ship != null) {
			final Vector3d worldNodeCenter = ship.getShipToWorld().transformPosition(new Vector3d(nodeCenterRender.x, nodeCenterRender.y, nodeCenterRender.z));
			nodeCenterRender = new Vec3(worldNodeCenter.x, worldNodeCenter.y, worldNodeCenter.z);
			rotation.setFromNormalized(ship.getShipToWorld());
		}

		poseStack.pushPose();

		poseStack.translate(nodeCenterRender.x - view.x, nodeCenterRender.y - view.y, nodeCenterRender.z - view.z);
		RenderUtil.drawBoxWithTexture(
			poseStack, vertexBuilder,
			lightMap,
			node.getModel(), new Vector3f(node.getColor().getTextureDiffuseColors()),
			ZERO_VEC3F, rotation, new Vector3f().set(size, size, size),
			1f
		);

		poseStack.popPose();
	}

	private static void renderNodePlaceHint(final ClientLevel level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final Vec3 view, final NodePos pos, final Vector4f color, final float scale) {
		final int size = 4;
		Vec3 nodeCenter = pos.getCenter().subtract(0.5, 0.5, 0.5);
		final Quaternionf rotation = new Quaternionf();
		final RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap().setAll(0xf000f0);

		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos.blockPos());
		if (ship != null) {
			final Vector3d worldNodeCenter = ship.getShipToWorld().transformPosition(new Vector3d(nodeCenter.x, nodeCenter.y, nodeCenter.z));
			nodeCenter = new Vec3(worldNodeCenter.x, worldNodeCenter.y, worldNodeCenter.z);
			rotation.setFromNormalized(ship.getShipToWorld());
		}

		poseStack.pushPose();

		poseStack.translate(nodeCenter.x - view.x, nodeCenter.y - view.y, nodeCenter.z - view.z);
		RenderUtil.drawBoxWithTexture(
			poseStack, vertexBuilder,
			lightMap,
			HINT_MODEL, color,
			ZERO_VEC3F, rotation, new Vector3f(size, size, size).div(16),
			scale
		);

		poseStack.popPose();
	}
}
