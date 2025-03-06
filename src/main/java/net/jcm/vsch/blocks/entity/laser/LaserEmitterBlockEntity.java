package net.jcm.vsch.blocks.entity.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.jcm.vsch.api.laser.ILaserSource;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;

import java.util.Collections;
import java.util.List;

public class LaserEmitterBlockEntity extends AbstractLaserCannonBlockEntity implements ILaserSource {
	private int r;
	private int g;
	private int b;
	private LaserContext firedLaser = null;
	private int laserLastTick = 0;

	public LaserEmitterBlockEntity(BlockPos pos, BlockState state) {
		super("laser_emitter", VSCHBlockEntities.LASER_EMITTER_BLOCK_ENTITY.get(), pos, state);
		this.r = 256;
		this.g = 0;
		this.b = 0;
	}

	@Override
	public List<LaserContext> getEmittingLasers() {
		return this.firedLaser == null ? Collections.emptyList() : Collections.singletonList(this.firedLaser);
	}

	@Override
	public void onLaserFired(LaserContext laser) {
		System.out.println("onLaserFired: " + laser);
		this.firedLaser = laser;
		this.laserLastTick = 2;
		this.markUpdated();
	}

	@Override
	public void load(CompoundTag data) {
		super.load(data);
		CompoundTag laser = data.getCompound("Laser");
		if (laser.isEmpty()) {
			this.firedLaser = null;
		} else {
			this.firedLaser = new LaserContext();
			this.firedLaser.readFromNBT(this.getLevel(), laser);
			this.laserLastTick = 20;
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		if (this.firedLaser != null) {
			data.put("Laser", this.firedLaser.writeToNBT(new CompoundTag()));
		}
		return data;
	}

	@Override
	public void signalChanged(int oldSignal, int newSignal) {
		if (newSignal == 0) {
			return;
		}
		LaserProperties props = new LaserProperties(this.r, this.g, this.b);
		LaserUtil.fireLaser(props, LaserEmitter.fromBlockEntity(this, this.facing));
	}

	public void tickLaser() {
		if (this.laserLastTick > 0) {
			this.laserLastTick--;
			if (this.laserLastTick == 0) {
				this.firedLaser = null;
			}
		}
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		this.tickLaser();
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		this.tickLaser();
	}
}
