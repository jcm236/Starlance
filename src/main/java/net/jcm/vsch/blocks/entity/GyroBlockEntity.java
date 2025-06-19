package net.jcm.vsch.blocks.entity;

import dan200.computercraft.shared.Capabilities;

import net.jcm.vsch.blocks.custom.template.WrenchableBlock;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.cc.peripherals.GyroPeripheral;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.jcm.vsch.ship.gyro.GyroData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class GyroBlockEntity extends BlockEntity implements ParticleBlockEntity, WrenchableBlock {
	private final GyroData data;
	private final EnergyStorage energyStorage;
	private volatile double torqueX = 0;
	private volatile double torqueY = 0;
	private volatile double torqueZ = 0;
	private int percentPower = 10;
	private volatile boolean isPeripheralMode = false;
	private boolean wasPeripheralMode = true;
	private LazyOptional<Object> lazyPeripheral = LazyOptional.empty();

	public GyroBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.GYRO_BLOCK_ENTITY.get(), pos, state);
		this.data = new GyroData(new Vector3d());
		this.energyStorage = new EnergyStorage(VSCHConfig.GYRO_ENERGY_CONSUME_RATE.get());
	}

	public double getTorqueForce() {
		return VSCHConfig.GYRO_STRENGTH.get().doubleValue();
	}

	public double getTorqueX() {
		return this.torqueX;
	}

	public void setTorqueX(double x) {
		x = Math.min(Math.max(x, -1), 1);
		if (this.torqueX == x) {
			return;
		}
		this.torqueX = x;
		this.setChanged();
	}

	public double getTorqueY() {
		return this.torqueY;
	}

	public void setTorqueY(double y) {
		y = Math.min(Math.max(y, -1), 1);
		if (this.torqueY == y) {
			return;
		}
		this.torqueY = y;
		this.setChanged();
	}

	public double getTorqueZ() {
		return this.torqueZ;
	}

	public void setTorqueZ(double z) {
		z = Math.min(Math.max(z, -1), 1);
		if (this.torqueZ == z) {
			return;
		}
		this.torqueZ = z;
		this.setChanged();
	}

	public void resetTorque() {
		this.torqueX = 0;
		this.torqueY = 0;
		this.torqueZ = 0;
		this.setChanged();
	}

	public double[] getTorque() {
		return new double[]{this.torqueX, this.torqueY, this.torqueZ};
	}

	public void setTorque(double x, double y, double z) {
		this.torqueX = Math.min(Math.max(x, -1), 1);
		this.torqueY = Math.min(Math.max(y, -1), 1);
		this.torqueZ = Math.min(Math.max(z, -1), 1);
		this.setChanged();
	}

	public int getPercentPower() {
		return this.percentPower;
	}

	/**
	 * @param power in range {@code [0, 10]}
	 */
	public void setPercentPower(int power) {
		if (power < 0 || power > 10) {
			throw new IllegalArgumentException("power out of range [0, 10]");
		}
		if (this.percentPower == power) {
			return;
		}
		this.percentPower = power;
		this.setChanged();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
	}

	public boolean getPeripheralMode() {
		return this.isPeripheralMode;
	}

	public void setPeripheralMode(boolean on) {
		if (this.isPeripheralMode != on) {
			this.isPeripheralMode = on;
			this.setChanged();
		}
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if (this.wasPeripheralMode != this.isPeripheralMode && !this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
		this.wasPeripheralMode = this.isPeripheralMode;

		// If we're not on a ship, don't bother calculating energy use
		// (It would be mean to consume that energy anyway)
		if (!VSGameUtilsKt.isBlockInShipyard(level, pos)) {
			return;
		}

		double x = this.torqueX;
		double y = this.torqueY;
		double z = this.torqueZ;
		if (this.energyStorage.maxEnergy > 0) {
			final double requirePower = (Math.abs(x) + Math.abs(y) + Math.abs(z)) * this.energyStorage.maxEnergy / 3;
			final double availablePower = this.energyStorage.storedEnergy;
			if (availablePower <= 0) {
				x = 0;
				y = 0;
				z = 0;
			} else if (requirePower > availablePower) {
				final double ratio = availablePower / requirePower;
				x *= ratio;
				y *= ratio;
				z *= ratio;
				this.energyStorage.storedEnergy = 0;
			} else {
				this.energyStorage.storedEnergy -= requirePower;
			}
		}

		final double force = this.getTorqueForce();
		this.data.torque.set(x * force, y * force, z * force);

		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships == null) {
			return;
		}
		if (ships.getGyroAtPos(pos) != this.data) {
			ships.addGyro(pos, this.data);
		}
	}

	@Override
	public InteractionResult onUseWrench(UseOnContext ctx) {
		if (ctx.getHand() != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		final int newPower = (this.getPercentPower() % 10) + 1;
		this.setPercentPower(newPower);

		final Player player = ctx.getPlayer();
		if (player != null) {
			player.displayClientMessage(Component.translatable("vsch.message.gyro", newPower * 10), true);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		//
	}

	@Override
	public void load(CompoundTag data) {
		this.energyStorage.storedEnergy = data.getInt("Energy");
		this.torqueX = data.getDouble("TorqueX");
		this.torqueY = data.getDouble("TorqueY");
		this.torqueZ = data.getDouble("TorqueZ");
		this.percentPower = data.getInt("PercentPower");
		this.isPeripheralMode = CompatMods.COMPUTERCRAFT.isLoaded() && data.getBoolean("PeripheralMode");
		super.load(data);
	}

	@Override
	public void saveAdditional(CompoundTag data) {
		super.saveAdditional(data);
		data.putInt("Energy", this.energyStorage.storedEnergy);
		data.putDouble("TorqueX", this.torqueX);
		data.putDouble("TorqueY", this.torqueY);
		data.putDouble("TorqueZ", this.torqueZ);
		data.putInt("PercentPower", this.percentPower);
		data.putBoolean("PeripheralMode", this.getPeripheralMode());
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		data.putDouble("TorqueX", this.torqueX);
		data.putDouble("TorqueY", this.torqueY);
		data.putDouble("TorqueZ", this.torqueZ);
		data.putInt("PercentPower", this.percentPower);
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction direction) {
		if (cap == ForgeCapabilities.ENERGY) {
			return LazyOptional.of(() -> this.energyStorage).cast();
		}
		if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (!lazyPeripheral.isPresent()) {
				lazyPeripheral = LazyOptional.of(() -> new GyroPeripheral(this));
			}
			return lazyPeripheral.cast();
		}
		return super.getCapability(cap, direction);
	}

	public void neighborChanged(Block block, BlockPos pos, boolean moving) {
		if (!this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
	}

	private void updatePowerByRedstone() {
		final Level level = this.getLevel();
		final BlockPos pos = this.getBlockPos();
		final double x = (level.getSignal(pos.east(), Direction.EAST) - level.getSignal(pos.west(), Direction.WEST)) / 15.0;
		final double y = (level.getSignal(pos.above(), Direction.UP) - level.getSignal(pos.below(), Direction.DOWN)) / 15.0;
		final double z = (level.getSignal(pos.south(), Direction.SOUTH) - level.getSignal(pos.north(), Direction.NORTH)) / 15.0;
		final double scale = this.percentPower / 10.0;
		this.setTorque(x * scale, y * scale, z * scale);
	}

	private static final class EnergyStorage implements IEnergyStorage {
		final int maxEnergy;
		int storedEnergy;

		EnergyStorage(int maxEnergy) {
			this.maxEnergy = maxEnergy;
		}

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int needs = this.maxEnergy - this.storedEnergy;
			if (needs < maxReceive) {
				maxReceive = needs;
			}
			if (!simulate) {
				this.storedEnergy += maxReceive;
			}
			return maxReceive;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return this.storedEnergy;
		}

		@Override
		public int getMaxEnergyStored() {
			return this.maxEnergy;
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	}
}
