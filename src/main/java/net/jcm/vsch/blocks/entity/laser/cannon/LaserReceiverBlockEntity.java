package net.jcm.vsch.blocks.entity.laser.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;

import net.jcm.vsch.api.laser.ILaserProcessor;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.blocks.entity.template.AbstractCannonBlockEntity;
import net.jcm.vsch.blocks.entity.template.IColoredBlockEntity;
import net.jcm.vsch.compat.cc.peripherals.laser.LaserReceiverPeripheral;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LaserReceiverBlockEntity extends AbstractLaserCannonBlockEntity implements IColoredBlockEntity, ILaserProcessor {
	private final ReadWriteLock rgbLock = new ReentrantReadWriteLock();
	private int r;
	private int g;
	private int b;
	private LaserContext lastLaser = null;
	private int analogOutput = 0;
	private LaserEmitterBlockEntity emitterBlock = null;

	public LaserReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.LASER_RECEIVER_BLOCK_ENTITY.get(), pos, state);
		this.r = 256;
		this.g = 0;
		this.b = 0;
	}

	@Override
	public int[] getColor() {
		final int r, g, b;
		this.rgbLock.readLock().lock();
		try {
			r = this.r;
			g = this.g;
			b = this.b;
		} finally {
			this.rgbLock.readLock().unlock();
		}
		return new int[]{r, g, b};
	}

	public void setColor(int r, int g, int b) {
		this.rgbLock.writeLock().lock();
		try {
			this.r = r;
			this.g = g;
			this.b = b;
		} finally {
			this.rgbLock.writeLock().unlock();
		}
	}

	public int getAnalogOutput() {
		return this.analogOutput;
	}

	@Override
	public int getMaxLaserStrength() {
		int strength = 256 * 16;
		if (this.lastLaser != null) {
			final LaserProperties props = this.lastLaser.getLaserProperties();
			strength -= props.r + props.g + props.b;
		}
		return strength;
	}

	@Override
	public void partChanged(Direction dir, AbstractCannonBlockEntity be) {
		if (dir == this.facing.getOpposite()) {
			this.emitterBlock = be instanceof LaserEmitterBlockEntity emitter ? emitter : null;
		}
	}

	@Override
	public void onLaserHit(final LaserContext ctx) {
		final BlockHitResult hit = (BlockHitResult) (ctx.getHitResult());
		if (hit.getDirection() != this.facing) {
			return;
		}
		final LaserProperties props = ctx.getLaserOnHitProperties();
		if (this.lastLaser != null) {
			LaserUtil.mergeLaser(ctx, this.lastLaser);
			return;
		}
		this.lastLaser = ctx.redirectWith(
			props, 
			this.emitterBlock == null ? null : LaserEmitter.fromBlockEntityCenter(
				this,
				Vec3.atLowerCornerOf(this.facing.getOpposite().getNormal())
			)
		);
	}

	@Override
	protected LazyOptional<?> getPeripheral() {
		return LazyOptional.of(() -> new LaserReceiverPeripheral(this));
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if (this.lastLaser == null) {
			if (this.analogOutput != 0) {
				this.analogOutput = 0;
				this.setChanged();
			}
			return;
		}
		final LaserProperties props = this.lastLaser.getLaserProperties();
		final double distR = (double)(this.r - props.r);
		final double distG = (double)(this.g - props.g);
		final double distB = (double)(this.b - props.b);
		if (this.emitterBlock != null && this.lastLaser.getLastRedirecter() != null) {
			if (this.emitterBlock.emitLaser(this.lastLaser)) {
				this.emitterBlock.cooldown = 1;
			}
		}
		this.lastLaser = null;
		final double dist = Math.sqrt(distR * distR + distG * distG + distB * distB);
		int newOutput = (int) (Math.min(Math.max(1 - (dist / 255), 0), 1) * 15);
		if (this.analogOutput != newOutput) {
			this.analogOutput = newOutput;
			this.setChanged();
		}
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {}
}
