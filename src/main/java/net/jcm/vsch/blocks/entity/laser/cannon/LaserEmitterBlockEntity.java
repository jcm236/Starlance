package net.jcm.vsch.blocks.entity.laser.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;

import net.jcm.vsch.api.laser.LaserEmitter;
import net.jcm.vsch.api.laser.LaserProperties;
import net.jcm.vsch.api.laser.LaserUtil;
import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.blocks.entity.template.IAnalogOutputBlockEntity;
import net.jcm.vsch.blocks.entity.template.IColoredBlockEntity;
import net.jcm.vsch.compat.cc.peripherals.laser.LaserEmitterPeripheral;

public class LaserEmitterBlockEntity extends AbstractLaserCannonBlockEntity implements IAnalogOutputBlockEntity, IColoredBlockEntity {
	private int r;
	private int g;
	private int b;
	private int analogOutput = 0;
	private int cooldown = 0;

	public LaserEmitterBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.LASER_EMITTER_BLOCK_ENTITY.get(), pos, state);
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

	@Override
	public int getAnalogOutput() {
		return Math.abs(this.analogOutput);
	}

	@Override
	public void signalChanged(int oldSignal, int newSignal) {
		if (oldSignal != 0 || newSignal == 0) {
			return;
		}
		if (this.fireLaser()) {
			this.cooldown = 4;
		}
	}

	@Override
	protected LazyOptional<?> getPeripheral() {
		return LazyOptional.of(() -> new LaserEmitterPeripheral(this));
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		super.tickForce(level, pos, state);
		if (this.cooldown == 0 && this.analogOutput != 0) {
			this.analogOutput = 0;
			this.setChanged();
		}
		this.cooldown--;
	}

	public boolean fireLaser() {
		if (this.cooldown > 0) {
			return false;
		}
		// TODO: consumes energy
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
		this.analogOutput = 15;
		this.setChanged();
		return true;
	}
}
