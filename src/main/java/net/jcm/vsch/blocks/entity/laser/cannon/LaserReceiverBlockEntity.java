package net.jcm.vsch.blocks.entity.laser.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.LazyOptional;

import net.jcm.vsch.api.laser.ILaserProcessor;
import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
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
	private int lastR = 0;
	private int lastG = 0;
	private int lastB = 0;
	private int analogOutput = 0;

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
		return 256 * 16 - (this.lastR + this.lastG + this.lastB);
	}

	@Override
	public void onLaserHit(final LaserContext ctx) {
		final BlockHitResult hit = (BlockHitResult) (ctx.getHitResult());
		if (hit.getDirection() != this.facing) {
			return;
		}
		final LaserProperties props = ctx.getLaserOnHitProperties();
		this.lastR += props.r;
		this.lastG += props.g;
		this.lastB += props.b;
	}

	@Override
	protected LazyOptional<?> getPeripheral() {
		return LazyOptional.of(() -> new LaserReceiverPeripheral(this));
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		final double distR = (double)(this.r - this.lastR), distG = (double)(this.g - this.lastG), distB = (double)(this.b - this.lastB);
		// TODO: forward laser to emitter when exists
		this.lastR = 0;
		this.lastG = 0;
		this.lastB = 0;
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
