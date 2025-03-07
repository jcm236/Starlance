package net.jcm.vsch.blocks.entity.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.jcm.vsch.api.laser.ILaserSource;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.blocks.entity.template.AbstractCannonBlockEntity;

import java.util.Collections;
import java.util.List;

public abstract class AbstractLaserCannonBlockEntity extends AbstractCannonBlockEntity implements ILaserSource {
	private LaserContext firedLaser = null;
	private int laserLastTick = 0;

	protected AbstractLaserCannonBlockEntity(String peripheralType, BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(peripheralType, type, pos, state);
	}

	@Override
	public List<LaserContext> getEmittingLasers() {
		return this.firedLaser == null ? Collections.emptyList() : Collections.singletonList(this.firedLaser);
	}

	@Override
	public void onLaserFired(final LaserContext laser) {
		this.firedLaser = laser;
		this.laserLastTick = 2;
		this.markUpdated();
	}

	@Override
	public AbstractLaserCannonBlockEntity getNeighbor(Direction dir) {
		return (AbstractLaserCannonBlockEntity) (this.neighbors.get(dir));
	}

	@Override
	public boolean isValidPart(Direction dir, AbstractCannonBlockEntity be) {
		return be instanceof AbstractLaserCannonBlockEntity && (dir == this.facing || dir.getOpposite() == this.facing);
	}

	@Override
	public void partChanged(Direction dir, AbstractCannonBlockEntity be) {
		// System.out.println("partChanged: " + this + ": " + dir + " = " + be);
	}

	public boolean canProcessLaser(Direction dir) {
		return false;
	}

	public LaserProperties processLaser(LaserProperties props) {
		return props;
	}

	public LaserEmitter createEmitter(AbstractLaserCannonBlockEntity sourceBlock) {
		return LaserEmitter.fromBlockEntityCenter(this, Vec3.atLowerCornerOf(this.facing.getNormal()));
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		final CompoundTag laser = data.getCompound("Laser");
		if (laser.isEmpty()) {
			this.firedLaser = null;
		} else {
			this.firedLaser = new LaserContext().readFromNBT(this.getLevel(), laser);
			this.laserLastTick = 3;
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		final CompoundTag data = super.getUpdateTag();
		if (this.firedLaser != null) {
			data.put("Laser", this.firedLaser.writeToNBT(new CompoundTag()));
		}
		return data;
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
