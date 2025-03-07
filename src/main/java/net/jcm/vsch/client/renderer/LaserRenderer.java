package net.jcm.vsch.client.renderer;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.jcm.vsch.particle.custom.LaserHitParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.util.VectorConversionsKt;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.jcm.vsch.api.laser.ILaserSource;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import thedarkcolour.kotlinforforge.forge.vectorutil.v3d.Vector3fcUtilKt;

import java.util.List;

public class LaserRenderer implements BlockEntityRenderer<BlockEntity> {
	private static final Vector3f DEFAULT_DIR = new Vector3f(0, 1, 0);

	private final BlockEntityRendererProvider.Context ctx;

	public LaserRenderer(BlockEntityRendererProvider.Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void render(BlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!(be instanceof ILaserSource source)) {
			return;
		}
		final BlockPos blockPos = be.getBlockPos();
		final Vector3d blockCorner = new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		final Ship ship = VSGameUtilsKt.getShipManagingPos(be.getLevel(), blockPos);
		if (ship != null) {
			ship.getShipToWorld().transformPosition(blockCorner);
		}
		final List<LaserContext> lasers = source.getEmittingLasers();
		for (final LaserContext laser : lasers) {
			final Vec3 from = laser.getEmitPosition();
			final Vec3 to = laser.getHitPosition();
			final Vec3 color = laser.getColor();
			final float[] colors = new float[]{(float) (color.x), (float) (color.y), (float) (color.z)};
			final Vec3 path = to.subtract(from);
			final float length = (float) (path.length());
			final Vector3f direction = path.toVector3f().normalize();
			// TODO: find a way to bypass VS renderer
			final Vector3d translation = new Vector3d(from.x, from.y, from.z).sub(blockCorner);
			if (ship != null) {
				ship.getWorldToShip().transformDirection(direction);
				ship.getWorldToShip().transformDirection(translation);
			}
			final Quaternionf rotation = new Quaternionf().rotationTo(DEFAULT_DIR, direction);

			renderBeaconBeam(
				be,
				translation, rotation,
				poseStack, bufferSource,
				BeaconRenderer.BEAM_LOCATION,
				partialTick,
				1, 0, length,
				colors,
				0.05f, 0.09f
			);

			Vec3 pos = to.subtract(direction.x / 5, direction.y / 5, direction.z / 5);
			// TODO: detect if block were hitting is a laser reciever/reflecter/whatever, and if so don't do these particles
			LaserHitParticle.spawnConeOfParticles(be.getLevel(), (float) pos.x, (float) pos.y, (float) pos.z, Vector3fcUtilKt.toVec3(direction), 5, colors);
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

	// TODO: these code are taken from AdvancedPeripherals.
	// We eventually should develop own laser renderer but not beacon beam.
	private static void renderBeaconBeam(
		BlockEntity be,
		Vector3d translation, Quaternionf rotation,
		PoseStack pPoseStack, MultiBufferSource pBufferSource,
		ResourceLocation pBeamLocation,
		float pPartialTick,
		float pTextureScale, int pYOffset, float pHeight,
		float[] pColors,
		float pBeamRadius, float pGlowRadius
	) {
		long pGameTime = be.getLevel().getGameTime();
		float maxX = pYOffset + pHeight;
		pPoseStack.pushPose();
		pPoseStack.translate(translation.x, translation.y, translation.z);
		float degrees = Math.floorMod(pGameTime, 360) + pPartialTick;
		float reversedDegrees = pHeight < 0 ? degrees : -degrees;
		float time = Mth.frac(reversedDegrees * 0.2F - Mth.floor(reversedDegrees * 0.1F));
		float r = pColors[0];
		float g = pColors[1];
		float b = pColors[2];
		pPoseStack.pushPose();
		pPoseStack.mulPose(rotation);
		pPoseStack.mulPose(Axis.YP.rotationDegrees(degrees * 2.25F - 45.0F));
		float f15 = -1.0F + time;
		float f16 = pHeight * pTextureScale * (0.5F / pBeamRadius) + f15;
		renderPart(pPoseStack, pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, false)), r, g, b, 1.0F, pYOffset, maxX, 0.0F, pBeamRadius, pBeamRadius, 0.0F, -pBeamRadius, 0.0F, 0.0F, -pBeamRadius, 0.0F, 1.0F, f16, f15);
		pPoseStack.popPose();
		pPoseStack.mulPose(rotation);
		f15 = -1.0F + time;
		f16 = pHeight * pTextureScale + f15;
		renderPart(pPoseStack, pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, true)), r, g, b, 0.225F, pYOffset, maxX, -pGlowRadius, -pGlowRadius, pGlowRadius, -pGlowRadius, -pBeamRadius, pGlowRadius, pGlowRadius, pGlowRadius, 0.0F, 1.0F, f16, f15);
		pPoseStack.popPose();
	}

	private static void renderPart(PoseStack pPoseStack, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha, int pMinY, float pMaxY, float pX0, float pZ0, float pX1, float pZ1, float pX2, float pZ2, float pX3, float pZ3, float pMinU, float pMaxU, float pMinV, float pMaxV) {
		PoseStack.Pose posestackPose = pPoseStack.last();
		Matrix4f matrix4f = posestackPose.pose();
		Matrix3f matrix3f = posestackPose.normal();
		renderQuad(matrix4f, matrix3f, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxY, pX0, pZ0, pX1, pZ1, pMinU, pMaxU, pMinV, pMaxV);
		renderQuad(matrix4f, matrix3f, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxY, pX3, pZ3, pX2, pZ2, pMinU, pMaxU, pMinV, pMaxV);
		renderQuad(matrix4f, matrix3f, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxY, pX1, pZ1, pX3, pZ3, pMinU, pMaxU, pMinV, pMaxV);
		renderQuad(matrix4f, matrix3f, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxY, pX2, pZ2, pX0, pZ0, pMinU, pMaxU, pMinV, pMaxV);
	}

	private static void renderQuad(Matrix4f pPose, Matrix3f pNormal, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha, int pMinY, float pMaxY, float pMinX, float pMinZ, float pMaxX, float pMaxZ, float pMinU, float pMaxU, float pMinV, float pMaxV) {
		addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, pMaxY, pMinX, pMinZ, pMaxU, pMinV);
		addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMinX, pMinZ, pMaxU, pMaxV);
		addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, pMinY, pMaxX, pMaxZ, pMinU, pMaxV);
		addVertex(pPose, pNormal, pConsumer, pRed, pGreen, pBlue, pAlpha, pMaxY, pMaxX, pMaxZ, pMinU, pMinV);
	}

	private static void addVertex(Matrix4f pPose, Matrix3f pNormal, VertexConsumer pConsumer, float pRed, float pGreen, float pBlue, float pAlpha, float pY, float pX, float pZ, float pU, float pV) {
		pConsumer.vertex(pPose, pX, pY, pZ).color(pRed, pGreen, pBlue, pAlpha).uv(pU, pV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pNormal, 0.0F, 1.0F, 0.0F).endVertex();
	}
}
