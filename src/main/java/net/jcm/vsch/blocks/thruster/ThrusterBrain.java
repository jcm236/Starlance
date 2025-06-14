package net.jcm.vsch.blocks.thruster;

import dan200.computercraft.shared.Capabilities;

import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.cc.ThrusterPeripheral;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.ship.thruster.ThrusterData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: make sure it also works when only half thrusters is chunk loaded
public class ThrusterBrain implements IEnergyStorage, IFluidHandler, ICapabilityProvider {
	private static final String THRUSTERS_COUNT_TAG_NAME = "ThrustersCount";
	private static final String MODE_TAG_NAME = "Mode";
	private static final String POWER_TAG_NAME = "Power";
	private static final String CURRENT_POWER_TAG_NAME = "CurrentPower";
	private static final String PERIPHERAL_MOD_TAG_NAME = "PeripheralMode";
	private static final String ENERGY_TAG_NAME = "Energy";
	private static final String TANKS_TAG_NAME = "Tanks";
	private static final int FLUID_TANK_CAPACITY = 10000;
	private static final int THRUSTER_MAX_DIMENSION = 32;

	private final ThrusterData thrusterData;
	private final ThrusterEngine engine;
	private final Direction facing;
	private int maxEnergy;
	private int storedEnergy;
	private final IEnergyStorage extractOnly = this.new ExtractOnly();
	private final FluidTank[] tanks;
	private final IFluidHandler drainOnly = this.new DrainOnly();

	private volatile float power = 0;
	private volatile float currentPower = 0;
	private volatile boolean powerChanged = false;

	private List<AbstractThrusterBlockEntity> connectedBlocks;

	private final String peripheralType;
	// Peripheral mode determines if the throttle is controlled by redstone, or by CC computers
	private volatile boolean isPeripheralMode = false;
	private boolean wasPeripheralMode = true;
	private LazyOptional<Object> lazyPeripheral = LazyOptional.empty();

	private ThrusterBrain(List<AbstractThrusterBlockEntity> connectedBlocks, String peripheralType, Direction facing, ThrusterEngine engine) {
		this.connectedBlocks = connectedBlocks;
		this.peripheralType = peripheralType;
		this.facing = facing;
		this.thrusterData = new ThrusterData(VectorConversionsMCKt.toJOMLD(facing.getNormal()), 0, VSCHConfig.THRUSTER_MODE.get());
		this.engine = engine;
		int count = this.connectedBlocks.size();
		this.maxEnergy = this.engine.getEnergyConsumeRate() * count;
		this.tanks = new FluidTank[this.engine.getTanks()];
		for (int i = 0; i < this.tanks.length; i++) {
			this.tanks[i] = new FluidTank(FLUID_TANK_CAPACITY * count);
		}
	}

	protected ThrusterBrain(AbstractThrusterBlockEntity dataBlock, String peripheralType, Direction facing, ThrusterEngine engine) {
		this(new ArrayList<>(List.of(dataBlock)), peripheralType, facing, engine);
	}

	public ThrusterEngine getEngine() {
		return this.engine;
	}

	public String getPeripheralType() {
		return this.peripheralType;
	}

	public int getThrusterCount() {
		return this.connectedBlocks.size();
	}

	public List<AbstractThrusterBlockEntity> getThrusters() {
		return Collections.unmodifiableList(this.connectedBlocks);
	}

	void addThruster(AbstractThrusterBlockEntity be) {
		this.connectedBlocks.add(be);
	}

	public void setThrusterMode(ThrusterData.ThrusterMode mode) {
		if (this.thrusterData.mode == mode) {
			return;
		}
		this.thrusterData.mode = mode;
		this.getDataBlock().sendUpdate();
	}

	public float getCurrentPower() {
		return this.currentPower;
	}

	protected void setCurrentPower(float power) {
		if (this.currentPower == power) {
			return;
		}
		this.currentPower = power;
		this.markPowerChanged();
	}

	public float getCurrentThrottle() {
		return this.getCurrentPower() * this.engine.getMaxThrottle();
	}

	/**
	 * @return thruster power in range of [0.0, 1.0]
	 */
	public float getPower() {
		return this.power;
	}

	public void setPower(float power) {
		float newPower = Math.min(Math.max(power, 0), 1);
		this.power = newPower;
	}

	public boolean getPeripheralMode() {
		return this.isPeripheralMode;
	}

	public void setPeripheralMode(boolean on) {
		if (this.isPeripheralMode != on) {
			this.isPeripheralMode = on;
			this.getDataBlock().setChanged();
		}
	}

	public AbstractThrusterBlockEntity getDataBlock() {
		return this.connectedBlocks.get(0);
	}

	protected void markPowerChanged() {
		this.powerChanged = true;
		this.getDataBlock().sendUpdate();
	}

	public ThrusterData.ThrusterMode getThrusterMode() {
		return this.thrusterData.mode;
	}

	public ThrusterData getThrusterData() {
		return this.thrusterData;
	}

	public void readFromNBT(CompoundTag data) {
		int count = data.getInt(THRUSTERS_COUNT_TAG_NAME);
		this.thrusterData.mode = ThrusterData.ThrusterMode.values()[data.getByte(MODE_TAG_NAME)];
		this.setPower(data.getFloat(POWER_TAG_NAME));
		this.currentPower = data.getFloat(CURRENT_POWER_TAG_NAME);
		this.isPeripheralMode = CompatMods.COMPUTERCRAFT.isLoaded() && data.getBoolean(PERIPHERAL_MOD_TAG_NAME);
		this.maxEnergy = this.engine.getEnergyConsumeRate() * count;
		this.storedEnergy = Math.min(this.maxEnergy, data.getInt(ENERGY_TAG_NAME));
		if (data.contains(TANKS_TAG_NAME)) {
			ListTag tanks = data.getList(TANKS_TAG_NAME, Tag.TAG_COMPOUND);
			if (tanks.size() == this.tanks.length) {
				for (int i = 0; i < this.tanks.length; i++) {
					FluidTank tank = this.tanks[i];
					tank.setCapacity(FLUID_TANK_CAPACITY * count);
					tank.readFromNBT(tanks.getCompound(i));
				}
			}
		}
		this.thrusterData.throttle = this.getCurrentThrottle();
	}

	public void writeToNBT(CompoundTag data) {
		data.putInt(THRUSTERS_COUNT_TAG_NAME, this.connectedBlocks.size());
		data.putByte(MODE_TAG_NAME, (byte) (this.thrusterData.mode.ordinal()));
		data.putFloat(POWER_TAG_NAME, this.getPower());
		data.putFloat(CURRENT_POWER_TAG_NAME, this.getCurrentPower());
		data.putBoolean(PERIPHERAL_MOD_TAG_NAME, this.getPeripheralMode());
		data.putInt(ENERGY_TAG_NAME, this.storedEnergy);
		ListTag tanks = new ListTag();
		for (int i = 0; i < this.tanks.length; i++) {
			FluidTank tank = this.tanks[i];
			CompoundTag tag = new CompoundTag();
			tank.writeToNBT(tag);
			tanks.add(tag);
		}
		data.put(TANKS_TAG_NAME, tanks);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction direction) {
		if (cap == ForgeCapabilities.ENERGY || cap == ForgeCapabilities.FLUID_HANDLER) {
			return LazyOptional.of(() -> this).cast();
		}
		if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (!lazyPeripheral.isPresent()) {
				lazyPeripheral = LazyOptional.of(() -> new ThrusterPeripheral(this));
			}
			return lazyPeripheral.cast();
		}
		return LazyOptional.empty();
	}

	public void tick(ServerLevel level) {
		// If we have changed peripheral mode, and we aren't peripheral mode
		if (this.wasPeripheralMode != this.isPeripheralMode && !this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
		this.wasPeripheralMode = this.isPeripheralMode;

		ThrusterEngineContext context = new ThrusterEngineContext(level, this.extractOnly, this.drainOnly, this.getPower(), this.connectedBlocks.size());
		this.engine.tick(context);
		if (context.isRejected()) {
			this.setCurrentPower(0);
		} else {
			final List<BlockPos> positions = this.connectedBlocks.stream().map(BlockEntity::getBlockPos).collect(Collectors.toList());
			this.engine.tickBurningObjects(context, positions, this.facing.getOpposite());
			this.setCurrentPower((float) (context.getPower()));
			context.consume();
		}
		if (this.powerChanged) {
			this.powerChanged = false;
			this.thrusterData.throttle = this.getCurrentThrottle();
		}
	}

	public void neighborChanged(AbstractThrusterBlockEntity thruster, Block block, BlockPos pos, boolean moving) {
		Level level = thruster.getLevel();
		BlockEntity changed = level.getBlockEntity(pos);
		if (changed instanceof AbstractThrusterBlockEntity newThruster) {
			// TODO: check if facing changed
			ThrusterBrain newBrain = newThruster.getBrain();
			if (newBrain != this) {
				this.tryMergeBrain(thruster.getBlockPos(), newBrain, pos);
			}
		} else {
			for (int i = 0; i < this.connectedBlocks.size(); i++) {
				AbstractThrusterBlockEntity be = this.connectedBlocks.get(i);
				if (be.getBlockPos().equals(pos)) {
					this.removeFromBrain(level, i);
					break;
				}
			}
		}
		// TODO: optimize redstone power scanning
		this.updatePowerByRedstone();
	}

	private void tryMergeBrain(BlockPos atPos, ThrusterBrain other, BlockPos otherPos) {
		if (this.facing != other.facing || !this.peripheralType.equals(other.peripheralType)) {
			return;
		}
		if (this.facing.getAxis().choose(otherPos.getX() - atPos.getX(), otherPos.getY() - atPos.getY(), otherPos.getZ() - atPos.getZ()) != 0) {
			return;
		}
		int minX, minY, minZ, maxX, maxY, maxZ;
		BlockPos dataPos = this.getDataBlock().getBlockPos();
		minX = maxX = dataPos.getX();
		minY = maxY = dataPos.getY();
		minZ = maxZ = dataPos.getZ();
		for (AbstractThrusterBlockEntity be : this.connectedBlocks) {
			BlockPos pos = be.getBlockPos();
			int x = pos.getX(), y = pos.getY(), z = pos.getZ();
			if (x < minX) {
				minX = x;
			} else if (x > maxX) {
				maxX = x;
			}
			if (y < minY) {
				minY = y;
			} else if (y > maxY) {
				maxY = y;
			}
			if (z < minZ) {
				minZ = z;
			} else if (z > maxZ) {
				maxZ = z;
			}
		}
		if (maxX - minX > THRUSTER_MAX_DIMENSION || maxY - minY > THRUSTER_MAX_DIMENSION || maxZ - minZ > THRUSTER_MAX_DIMENSION) {
			return;
		}

		this.connectedBlocks.addAll(other.connectedBlocks);
		for (AbstractThrusterBlockEntity be : other.connectedBlocks) {
			be.setBrain(this);
		}
		int count = this.connectedBlocks.size();
		this.maxEnergy = this.engine.getEnergyConsumeRate() * count;
		this.storedEnergy += other.storedEnergy;
		for (int i = 0; i < this.tanks.length; i++) {
			FluidTank tank = this.tanks[i];
			tank.setCapacity(FLUID_TANK_CAPACITY * count);
			tank.fill(other.tanks[i].getFluid(), IFluidHandler.FluidAction.EXECUTE);
		}
		this.getDataBlock().sendUpdate();
	}

	private void removeFromBrain(Level level, int index) {
		AbstractThrusterBlockEntity removed = this.connectedBlocks.remove(index);
		if (index == 0) {
			this.broadcastDataBlockUpdate();
		}
		Set<BlockPos> collected = new HashSet<>();
		collected.add(removed.getBlockPos());
		List<AbstractThrusterBlockEntity>[] sets = streamNeighborPositions(removed.getBlockPos(), this.facing)
			.map(level::getBlockEntity)
			.filter(AbstractThrusterBlockEntity.class::isInstance)
			.map(AbstractThrusterBlockEntity.class::cast)
			.filter((be) -> !collected.contains(be.getBlockPos()))
			.map((be) -> collectAllConnecting(level, be, this.facing, collected))
			.toArray(List[]::new);

		this.connectedBlocks = sets[0];
		int count = this.connectedBlocks.size();
		this.maxEnergy = this.engine.getEnergyConsumeRate() * count;
		int lastEnergy = this.storedEnergy;
		this.storedEnergy = Math.min(this.maxEnergy, lastEnergy);
		lastEnergy -= this.storedEnergy;
		int[] lastFluids = new int[this.tanks.length];
		for (int i = 0; i < this.tanks.length; i++) {
			FluidTank tank = this.tanks[i];
			FluidStack stack = tank.getFluid();
			tank.setCapacity(FLUID_TANK_CAPACITY * count);
			if (stack.isEmpty()) {
				continue;
			}
			lastFluids[i] = stack.getAmount();
			stack.setAmount(Math.min(tank.getCapacity(), lastFluids[i]));
			lastFluids[i] -= stack.getAmount();
		}
		for (int i = 1; i < sets.length; i++) {
			List<AbstractThrusterBlockEntity> set = sets[i];
			ThrusterBrain newBrain = new ThrusterBrain(set, this.peripheralType, this.facing, engine);
			newBrain.storedEnergy = Math.min(newBrain.maxEnergy, lastEnergy);
			lastEnergy -= newBrain.storedEnergy;
			for (int j = 0; j < newBrain.tanks.length; j++) {
				FluidTank tank = newBrain.tanks[j];
				FluidStack stack = tank.getFluid();
				if (lastFluids[j] > 0) {
					int amount = Math.min(tank.getCapacity(), lastFluids[j]);
					lastFluids[j] -= amount;
					newBrain.tanks[j].setFluid(new FluidStack(stack.getFluid(), amount));
				}
			}
			for (AbstractThrusterBlockEntity t : set) {
				t.setBrain(newBrain);
			}
		}
	}

	private static Stream<BlockPos> streamNeighborPositions(BlockPos origin, Direction facing) {
		return Direction.stream().filter((d) -> d.getAxis() != facing.getAxis()).map(origin::relative);
	}

	private static List<AbstractThrusterBlockEntity> collectAllConnecting(Level level, AbstractThrusterBlockEntity be, Direction facing, Set<BlockPos> collected) {
		List<AbstractThrusterBlockEntity> result = new ArrayList<>();
		if (!collected.add(be.getBlockPos())) {
			return result;
		}
		ArrayDeque<AbstractThrusterBlockEntity> deque = new ArrayDeque<>();
		deque.addLast(be);
		while (!deque.isEmpty()) {
			AbstractThrusterBlockEntity b = deque.removeLast();
			result.add(b);
			BlockPos pos = b.getBlockPos();
			streamNeighborPositions(pos, facing)
				.filter(collected::add)
				.map(level::getBlockEntity)
				.filter(AbstractThrusterBlockEntity.class::isInstance)
				.map(AbstractThrusterBlockEntity.class::cast)
				.forEach(deque::addLast);
		}
		return result;
	}

	private void broadcastDataBlockUpdate() {
		for (AbstractThrusterBlockEntity be : this.connectedBlocks) {
			be.sendUpdate();
		}
	}

	private void updatePowerByRedstone() {
		float newPower = 0;
		for (AbstractThrusterBlockEntity be : this.connectedBlocks) {
			float power = getPowerByRedstone(be.getLevel(), be.getBlockPos());
			if (power > newPower) {
				newPower = power;
			}
		}
		this.setPower(newPower);
	}

	private static float getPowerByRedstone(Level level, BlockPos pos) {
		return (float) (level.getBestNeighborSignal(pos)) / 15;
	}

	/// IEnergyStorage

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

	final class ExtractOnly implements IEnergyStorage {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			return 0;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			if (maxExtract > ThrusterBrain.this.storedEnergy) {
				maxExtract = ThrusterBrain.this.storedEnergy;
			}
			if (!simulate) {
				ThrusterBrain.this.storedEnergy -= maxExtract;
			}
			return maxExtract;
		}

		@Override
		public int getEnergyStored() {
			return ThrusterBrain.this.storedEnergy;
		}

		@Override
		public int getMaxEnergyStored() {
			return ThrusterBrain.this.maxEnergy;
		}

		@Override
		public boolean canExtract() {
			return true;
		}

		@Override
		public boolean canReceive() {
			return false;
		}
	}

	/// IFluidHandler

	@Override
	public int getTanks() {
		return this.tanks.length;
	}

	private IFluidHandler getDrainOnly() {
		return this.drainOnly;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return this.tanks[tank].getFluid();
	}

	@Override
	public int getTankCapacity(int tank) {
		return this.tanks[tank].getCapacity();
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return this.engine.isValidFuel(tank, stack.getFluid());
	}

	private FluidTank getFillable(FluidStack resource) {
		Fluid fluid = resource.getFluid();
		for (int i = 0; i < this.tanks.length; i++) {
			if (this.engine.isValidFuel(i, fluid)) {
				FluidTank tank = this.tanks[i];
				FluidStack stack = tank.getFluid();
				if (stack.getFluid() != fluid) {
					if (!stack.isEmpty()) {
						return null;
					}
					stack = new FluidStack(fluid, 0);
					tank.setFluid(stack);
				}
				return tank;
			}
		}
		return null;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		FluidTank tank = this.getFillable(resource);
		return tank == null ? 0 : tank.fill(resource, action);
	}

	private FluidTank getDrainable(FluidStack resource) {
		Fluid fluid = resource.getFluid();
		for (FluidTank tank : this.tanks) {
			if (tank.getFluid().getFluid() == fluid) {
				return tank;
			}
		}
		return null;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return FluidStack.EMPTY;
	}

	final class DrainOnly implements IFluidHandler {
		@Override
		public int getTanks() {
			return ThrusterBrain.this.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return ThrusterBrain.this.getFluidInTank(tank);
		}

		@Override
		public int getTankCapacity(int tank) {
			return ThrusterBrain.this.getTankCapacity(tank);
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return ThrusterBrain.this.isFluidValid(tank, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			FluidTank tank = ThrusterBrain.this.getDrainable(resource);
			return tank == null ? FluidStack.EMPTY : tank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			for (FluidTank tank : ThrusterBrain.this.tanks) {
				FluidStack stack = tank.drain(maxDrain, action);
				if (!stack.isEmpty()) {
					return stack;
				}
			}
			return FluidStack.EMPTY;
		}
	}
}
