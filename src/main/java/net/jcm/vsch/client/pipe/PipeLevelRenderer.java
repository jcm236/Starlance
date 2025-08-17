package net.jcm.vsch.client.pipe;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.accessor.INodeLevelChunkSection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.api.resource.ModelTextures;
import net.jcm.vsch.api.resource.TextureLocation;
import net.jcm.vsch.client.RenderUtil;
import net.jcm.vsch.items.custom.WrenchItem;
import net.jcm.vsch.pipe.PipeNetworkOperator;
import net.jcm.vsch.pipe.level.NodeGetter;
import net.jcm.vsch.pipe.level.NodeLevel;

import org.joml.Matrix4d;
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
import net.minecraft.world.level.Level;
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
	private static final int PIPE_VIEW_RANGE = 8;
	private static final Vector3f ZERO_VEC3F = new Vector3f();

	private static final Vector4f HINT_COLOR = new Vector4f(0.25f, 0.70f, 0.25f, 0.6f);
	private static final float HINT_SCALE = 0.7f;
	private static final Vector4f HINT_SELECTING_COLOR = new Vector4f(0.25f, 0.92f, 0.25f, 0.8f);
	private static final float HINT_SELECTING_SCALE = 0.85f;
	private static final ModelTextures HINT_MODEL;

	static {
		final ResourceLocation resource = new ResourceLocation(VSCHMod.MODID, "block/pipe/omni_node");
		final TextureLocation texture1 = new TextureLocation(resource, 8, 4);
		final TextureLocation texture2 = new TextureLocation(resource, 8, 8);
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
		poseStack.translate(-view.x, -view.y, -view.z);

		renderPlaceHint(level, poseStack, vertexBuilder, partialTick);
		renderNodes(level, poseStack, vertexBuilder, partialTick, view);

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
			final int maxX = SectionPos.blockToSectionCoord(viewBoxOut.maxX) + 1, maxZ = SectionPos.blockToSectionCoord(viewBoxOut.maxZ) + 1;
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

	private static void renderPlaceHint(final ClientLevel level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final float partialTick) {
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
				level, poseStack, vertexBuilder, partialTick, pos,
				pos.equals(lookingPos) ? HINT_SELECTING_COLOR : HINT_COLOR,
				pos.equals(lookingPos) ? HINT_SELECTING_SCALE : HINT_SCALE
			);
		});
	}

	private static void renderNodes(final ClientLevel level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final float partialTick, final Vec3 view) {
		streamRenderingChunks(level, view).forEach((chunk) -> renderNodesInChunk(level, chunk, poseStack, vertexBuilder, partialTick));
	}

	private static void renderNodesInChunk(final ClientLevel level, final ChunkAccess chunk, final PoseStack poseStack, final VertexConsumer vertexBuilder, final float partialTick) {
		if (!(chunk instanceof NodeGetter nodeGetter)) {
			return;
		}
		if (!nodeGetter.hasAnyNode()) {
			return;
		}
		nodeGetter.streamNodes().forEach((node) -> renderNode(level, poseStack, vertexBuilder, partialTick, node));
	}

	private static void renderNode(final ClientLevel level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final float partialTick, final PipeNode node) {
		final NodeLevel nodeLevel = NodeLevel.get(level);
		final PipeNetworkOperator network = nodeLevel.getNetwork();
		final int size = node.getSize();
		final NodePos pos = node.getPos();
		final Vec3 nodeCenter = pos.getCenter();
		final Vector3f color = new Vector3f(node.getColor().getTextureDiffuseColors());
		final RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap();
		final double r = size / 16.0 / 2;
		final double l = 1 - r * 2;
		lightMap.fillFromLevel(
			level,
			new AABB(
				nodeCenter.x - r, nodeCenter.y - r, nodeCenter.y - r,
				nodeCenter.x + r, nodeCenter.y + r, nodeCenter.y + r
			)
		);

		final Vec3 nodeCenterRender = toWorldCoordinatesLerp(level, partialTick, nodeCenter);
		final Quaternionf rotation = new Quaternionf();
		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos.blockPos());
		if (ship != null) {
			rotation.setFromNormalized(ship.getPrevTickTransform().getShipToWorld().lerp(ship.getTransform().getShipToWorld(), partialTick, new Matrix4d()));
		}

		poseStack.pushPose();
		poseStack.translate(nodeCenterRender.x, nodeCenterRender.y, nodeCenterRender.z);

		RenderUtil.drawBoxWithTexture(
			poseStack, vertexBuilder,
			lightMap,
			node.getModel(), color,
			ZERO_VEC3F, rotation, new Vector3i(size, size, size),
			1f
		);

		poseStack.popPose();

		for (final NodePos otherPos : network.getConnections(node)) {
			if (pos.compareTo(otherPos) >= 0) {
				continue;
			}
			final PipeNode other = nodeLevel.getNode(otherPos);
			final Direction[] path = pos.connectPathTo(otherPos);
			final Direction path0 = path[0];
			final Direction.Axis pathAxis0 = path0.getAxis();
			switch (path.length) {
				case 1:
					final Vec3 center1 = nodeCenter.add(path0.getStepX() * r, path0.getStepY() * r, path0.getStepZ() * r);
					final Vec3 center3 = center1.add(path0.getStepX() * l, path0.getStepY() * l, path0.getStepZ() * l);
					final Vec3 center2 = center1.add(center3).scale(0.5);
					final double
						x2 = center2.x + pathAxis0.choose(0, r, r),
						y2 = center2.y + pathAxis0.choose(r, 0, r),
						z2 = center2.z + pathAxis0.choose(r, r, 0);
					final AABB box1 = new AABB(
						x2, y2, z2,
						center1.x - pathAxis0.choose(0, r, r), center1.y - pathAxis0.choose(r, 0, r), center1.z - pathAxis0.choose(r, r, 0)
					);
					final AABB box2 = new AABB(
						x2, y2, z2,
						center3.x - pathAxis0.choose(0, r, r), center3.y - pathAxis0.choose(r, 0, r), center3.z - pathAxis0.choose(r, r, 0)
					);

					poseStack.pushPose();
					final Vec3 box1Center = toWorldCoordinatesLerp(level, partialTick, box1.getCenter());
					poseStack.translate(box1Center.x, box1Center.y, box1Center.z);
					RenderUtil.drawBoxWithTexture(
						poseStack, vertexBuilder,
						lightMap.fillFromLevel(level, box1),
						node.getPipeModel(path0.getOpposite()), color,
						ZERO_VEC3F, rotation,
						new Vector3i((int) (Math.round(box1.getXsize() * 16)), (int) (Math.round(box1.getYsize() * 16)), (int) (Math.round(box1.getZsize() * 16))),
						1f
					);
					poseStack.popPose();

					poseStack.pushPose();
					final Vec3 box2Center = toWorldCoordinatesLerp(level, partialTick, box2.getCenter());
					poseStack.translate(box2Center.x, box2Center.y, box2Center.z);
					RenderUtil.drawBoxWithTexture(
						poseStack, vertexBuilder,
						lightMap.fillFromLevel(level, box2),
						other.getPipeModel(path0), color,
						ZERO_VEC3F,
						rotation.rotateXYZ(
							(float) (pathAxis0.choose(Math.PI, Math.PI, 0)),
							(float) (pathAxis0.choose(0, Math.PI, Math.PI)),
							(float) (pathAxis0.choose(Math.PI, 0, Math.PI)),
							new Quaternionf()
						),
						new Vector3i((int) (Math.round(box2.getXsize() * 16)), (int) (Math.round(box2.getYsize() * 16)), (int) (Math.round(box2.getZsize() * 16))),
						1f
					);
					poseStack.popPose();
					break;
			}
		}
	}

	private static void renderNodePlaceHint(final ClientLevel level, final PoseStack poseStack, final VertexConsumer vertexBuilder, final float partialTick, final NodePos pos, final Vector4f color, final float scale) {
		final int size = 4;
		final Vec3 nodeCenter = toWorldCoordinatesLerp(level, partialTick, pos.getCenter());
		final Quaternionf rotation = new Quaternionf();
		final RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap().setAll(0xf000f0);

		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos.blockPos());
		if (ship != null) {
			rotation.setFromNormalized(ship.getPrevTickTransform().getShipToWorld().lerp(ship.getTransform().getShipToWorld(), partialTick, new Matrix4d()));
		}

		poseStack.pushPose();

		poseStack.translate(nodeCenter.x, nodeCenter.y, nodeCenter.z);
		RenderUtil.drawBoxWithTexture(
			poseStack, vertexBuilder,
			lightMap,
			HINT_MODEL, color,
			ZERO_VEC3F, rotation, new Vector3i(size, size, size),
			scale
		);

		poseStack.popPose();
	}

	private static Vec3 toWorldCoordinatesLerp(final Level level, final float partialTick, final Vec3 pos) {
		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
		if (ship == null) {
			return pos;
		}
		final Vector3d worldPos = ship.getPrevTickTransform().getShipToWorld().lerp(
			ship.getTransform().getShipToWorld(),
			partialTick,
			new Matrix4d()
		)
			.transformPosition(pos.x, pos.y, pos.z, new Vector3d());
		return new Vec3(worldPos.x, worldPos.y, worldPos.z);
	}
}
