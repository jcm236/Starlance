package net.jcm.vsch.blocks.entity.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;

import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;

public class LaserFlatMirrorBlockEntity extends AbstractDirectionalLaserLenBlockEntity {
	public LaserFlatMirrorBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.LASER_FLAT_MIRROR_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public int getMaxLaserStrengthPerTick() {
		return 256 * 32;
	}

	@Override
	public void onLaserHit(final LaserContext ctx) {
		super.onLaserHit(ctx);
		final LaserProperties props = ctx.getLaserOnHitProperties();
		final Vec3 hitPos = ctx.getHitPosition();
		final Vec3 inputDirVec3 = ctx.getInputDirection();
		final Vector3d inputDir = new Vector3d(inputDirVec3.x, inputDirVec3.y, inputDirVec3.z);
		final Vector3d panel = this.getPanelNormal();
		final Vector3d outputDir = new Vector3d();
		panel.mul(2 * panel.dot(inputDir), outputDir);
		inputDir.sub(outputDir, outputDir);
		LaserUtil.fireRedirectedLaser(
			ctx.redirectWith(
				props,
				LaserEmitter.fromBlockEntity(this, hitPos, new Vec3(outputDir.x, outputDir.y, outputDir.z))
			)
		);
	}
}
