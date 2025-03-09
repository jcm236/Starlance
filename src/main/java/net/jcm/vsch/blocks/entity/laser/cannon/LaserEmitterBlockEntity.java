package net.jcm.vsch.blocks.entity.laser.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.blocks.entity.template.IColoredBlockEntity;

public class LaserEmitterBlockEntity extends AbstractLaserCannonBlockEntity implements IColoredBlockEntity {
	private int r;
	private int g;
	private int b;

	public LaserEmitterBlockEntity(BlockPos pos, BlockState state) {
		super("laser_emitter", VSCHBlockEntities.LASER_EMITTER_BLOCK_ENTITY.get(), pos, state);
		this.r = 256;
		this.g = 0;
		this.b = 0;
	}

	@Override
	public int[] getColor() {
		return new int[]{this.r, this.g, this.b};
	}

	public void setColor(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	// @Override
	// public void signalChanged(int oldSignal, int newSignal) {
	// 	if (newSignal == 0 || oldSignal != 0) {
	// 		return;
	// 	}
	// 	this.fireLaser(props, LaserEmitter.fromBlockEntity(this, this.facing));
	// }

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		super.tickForce(level, pos, state);
		if (this.redstone > 0) {
			this.fireLaser();
		}
	}

	public void fireLaser() {
		LaserProperties props = new LaserProperties(this.r, this.g, this.b);
		AbstractLaserCannonBlockEntity current = this;
		while (true) {
			final AbstractLaserCannonBlockEntity next = current.getNeighbor(this.facing);
			if (next == null || !next.canProcessLaser(this.facing)) {
				break;
			}
			current = next;
			props = current.processLaser(props);
		}
		LaserUtil.fireLaser(props, current.createEmitter(this));
	}
}
