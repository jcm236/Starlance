package net.jcm.vsch.blocks.entity.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.jcm.vsch.api.laser.ILaserProcessor;
import net.jcm.vsch.api.laser.ILaserSource;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLaserLenBlockEntity extends BlockEntity implements ILaserProcessor, ILaserSource, ParticleBlockEntity {
	private final List<LaserContext> emittingLasers = new ArrayList<>();
	private int laserLastTick = 0;
	private int lasering = 0;

	public AbstractLaserLenBlockEntity(BlockEntityType<? extends AbstractLaserLenBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public int getMaxLaserStrength() {
		return this.getMaxLaserStrengthPerTick() - this.lasering;
	}

	public abstract int getMaxLaserStrengthPerTick();

	@Override
	public void onLaserHit(LaserContext ctx) {
		final LaserProperties props = ctx.getLaserOnHitProperties();
		this.lasering += props.r + props.g + props.b;
	}

	@Override
	public List<LaserContext> getEmittingLasers() {
		return this.emittingLasers;
	}

	@Override
	public void onLaserFired(LaserContext laser) {
		if (this.laserLastTick != 2) {
			this.emittingLasers.clear();
		}
		this.emittingLasers.add(laser);
		this.laserLastTick = 2;
		this.markUpdated();
	}

	public void markUpdated() {
		this.setChanged();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 11);
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		final ListTag lasers = data.getList("Lasers", Tag.TAG_COMPOUND);
		this.emittingLasers.clear();
		if (!lasers.isEmpty()) {
			lasers.stream()
				.map(tag -> new LaserContext().readFromNBT(this.getLevel(), (CompoundTag) (tag)))
				.forEach(this.emittingLasers::add);
			this.laserLastTick = 3;
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		final CompoundTag data = super.getUpdateTag();
		if (!this.emittingLasers.isEmpty()) {
			final ListTag lasers = new ListTag();
			data.put("Lasers", lasers);
			this.emittingLasers.stream()
				.map(l -> l.writeToNBT(new CompoundTag()))
				.forEach(lasers::add);
		}
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void tickLaser() {
		if (this.laserLastTick > 0) {
			this.laserLastTick--;
			if (this.laserLastTick == 0) {
				this.emittingLasers.clear();
			}
		}
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		this.tickLaser();
		this.lasering = 0;
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		this.tickLaser();
	}
}
