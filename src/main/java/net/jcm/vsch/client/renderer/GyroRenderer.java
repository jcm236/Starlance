package net.jcm.vsch.client.renderer;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.resource.ModelTextures;
import net.jcm.vsch.api.resource.TextureLocation;
import net.jcm.vsch.blocks.entity.GyroBlockEntity;
import net.jcm.vsch.client.RenderUtil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class GyroRenderer implements BlockEntityRenderer<GyroBlockEntity> {
	private static final Vector4f ONE4 = new Vector4f(1, 1, 1, 1);
	private static final Vector3f HALF3 = new Vector3f(0.5f, 0.5f, 0.5f);
	private static final Vector3i CORE_SIZE = new Vector3i(6, 6, 6);
	private static final ModelTextures CORE_MODEL;

	static {
		final ResourceLocation resource = new ResourceLocation(VSCHMod.MODID, "block/gyro");
		CORE_MODEL = new ModelTextures(
			TextureLocation.fromNonStandardSize(resource, 12, 44, 128),
			TextureLocation.fromNonStandardSize(resource, 6, 44, 128),
			TextureLocation.fromNonStandardSize(resource, 6, 50, 128),
			TextureLocation.fromNonStandardSize(resource, 18, 50, 128),
			TextureLocation.fromNonStandardSize(resource, 12, 50, 128),
			TextureLocation.fromNonStandardSize(resource, 0, 50, 128)
		);
	}

	private final BlockEntityRendererProvider.Context ctx;

	public GyroRenderer(BlockEntityRendererProvider.Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void render(
		final GyroBlockEntity be,
		final float partialTick,
		final PoseStack poseStack,
		final MultiBufferSource bufferSource,
		final int packedLight,
		final int packedOverlay
	) {
		final Ship ship = VSGameUtilsKt.getShipManagingPos(be.getLevel(), be.getBlockPos());
		final RenderUtil.BoxLightMap lightMap = new RenderUtil.BoxLightMap().setAll(packedLight);

		final Quaternionf rot = be.getCoreRotation(partialTick);
		if (ship != null) {
			rot.mul(new Quaternionf().setFromNormalized(ship.getShipToWorld()).conjugate()).normalize();
		}

		final VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
		RenderUtil.drawBoxWithTexture(poseStack, buffer, lightMap, CORE_MODEL, ONE4, new Vector3f(), rot, CORE_SIZE, 1f);
	}
}
