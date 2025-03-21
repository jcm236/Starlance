package net.jcm.vsch.blocks.entity;

import dan200.computercraft.shared.Capabilities;

import net.jcm.vsch.blocks.custom.template.BlockEntityWithEntity;
import net.jcm.vsch.blocks.custom.MagnetBlock;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.compat.cc.peripherals.MagnetPeripheral;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.entity.MagnetEntity;
import net.jcm.vsch.entity.VSCHEntities;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.jcm.vsch.ship.magnet.MagnetData;
import net.jcm.vsch.util.VSCHUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: connect magnets
public class MagnetBlockEntity extends BlockEntityWithEntity<MagnetEntity> {
	private static final double MAGNET_GENERATE_RATE = 5e2; // FE/m
	private static final double MAGNET_GENERATE_LOSS = 10; // FE/t

	private Vec3 facing; // TODO: update facing as block updated
	private final MagnetData magnetData;
	private boolean needInit = true;

	private volatile float power = -1.0f; // Range: [-1.0, 1.0]
	private volatile float tickPower = 0f;

	private volatile boolean isPeripheralMode = false;
	private boolean wasPeripheralMode = true;

	private final AtomicReference<BlockState> updatingState = new AtomicReference<>();
	private volatile boolean isGenerator;
	private Vector3d lastVeloctiy = new Vector3d();
	private final DoubleAdder lastGenerated = new DoubleAdder();

	private LazyOptional<Object> lazyPeripheral = LazyOptional.empty();
	private final MagnetEnergyStorage energyStorage = new MagnetEnergyStorage(VSCHConfig.MAGNET_BLOCK_CONSUME_ENERGY.get().intValue());

	private Map<MagnetBlockEntity, DuoVector3d> cachedForces = new HashMap<>();

	public MagnetBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.MAGNET_BLOCK_ENTITY.get(), pos, state);
		this.facing = Vec3.atLowerCornerOf(state.getValue(DirectionalBlock.FACING).getNormal());
		this.isGenerator = state.getValue(MagnetBlock.GENERATOR);
		this.magnetData = new MagnetData(this.facing.toVector3f(), this.isGenerator);
	}

	public float getAttractDistance() {
		return VSCHConfig.MAGNET_BLOCK_DISTANCE.get().floatValue() + 1;
	}

	@Override
	public MagnetEntity createLinkedEntity(ServerLevel level, BlockPos pos) {
		MagnetEntity entity = new MagnetEntity(VSCHEntities.MAGNET_ENTITY.get(), level);
		entity.setAttachedBlockPos(this.getBlockPos());
		return entity;
	}

	public Vector3d getWorldPos() {
		BlockPos pos = this.getBlockPos();
		Vector3d vec3 = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		Ship ship = VSGameUtilsKt.getShipObjectManagingPos(this.getLevel(), pos);
		if (ship != null) {
			ship.getShipToWorld().transformPosition(vec3);
		}
		return vec3;
	}

	public Vector3f getFacing() {
		Vector3f facing = this.facing.toVector3f();
		Ship ship = VSGameUtilsKt.getShipObjectManagingPos(this.getLevel(), this.getBlockPos());
		if (ship != null) {
			facing = ship.getShipToWorld().transformDirection(facing);
		}
		return facing;
	}

	/**
	 * @return magnet target working power between -1.0~1.0
	 */
	public float getPower() {
		return this.power;
	}

	/**
	 * @return the max power the magnet can activate based on the energy input
	 */
	public float getMaxAvaliablePower() {
		return this.energyStorage.maxEnergyRate == 0 ? 1.0f : (float)(this.energyStorage.stored) / this.energyStorage.maxEnergyRate;
	}

	public float getActivatablePower() {
		return this.tickPower;
	}

	public void setPower(float power) {
		this.setPower(power, true);
	}

	protected void setPower(float power, boolean update) {
		float newPower = Math.min(Math.max(power, -1), 1);
		if (this.power == newPower) {
			return;
		}
		this.power = newPower;
		if (update) {
			this.markPowerChanged();
		}
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

	public boolean getIsGenerator() {
		return this.isGenerator;
	}

	public void setIsGenerator(boolean isGenerator) {
		if (this.isGenerator == isGenerator) {
			return;
		}
		this.isGenerator = isGenerator;
		this.updatingState.updateAndGet(
			(state) -> (state != null ? state : this.getBlockState()).setValue(MagnetBlock.GENERATOR, isGenerator));
	}

	protected void markPowerChanged() {
		this.setChanged();
		BlockState state = this.getBlockState();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_ALL_IMMEDIATE);
	}

	private static Vector3d getStandardForceTo(Vector3d selfPos, float angle, Vector3d pos, Vector3d dest) {
		final double maxForce = VSCHConfig.MAGNET_BLOCK_MAX_FORCE.get().doubleValue();
		pos.sub(selfPos, dest);
		float force = (float)(maxForce / dest.lengthSquared() * Math.abs(Math.cos(angle)));
		return dest.normalize(force);
	}

	/**
	 * @return the list of magnets that the current magnet is moving towards to.
	 *   or {@code null} if the current magnet is not on a {@link ServerShip}
	 */
	private List<MagnetEntity> scanMagnets() {
		Level level = this.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return null;
		}
		BlockPos blockPos = this.getBlockPos();
		ServerShip currentShip = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, blockPos);
		if (currentShip == null || currentShip.isStatic()) {
			return Collections.emptyList();
		}

		Vector3f facing = this.getFacing();
		Matrix4dc transform = currentShip.getShipToWorld();
		Vector3d center = transform.transformPosition(new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5));

		final float maxDistance = this.getAttractDistance();
		final float maxDistanceSqr = maxDistance * maxDistance;

		Vec3 axisX = new Vec3(transform.transformDirection(new Vector3f(maxDistance, 0, 0)));
		Vec3 axisY = new Vec3(transform.transformDirection(new Vector3f(0, maxDistance, 0)));
		Vec3 axisZ = new Vec3(transform.transformDirection(new Vector3f(0, 0, maxDistance)));
		// TODO: ~~find a easier calculation without ton of min/maxes~~
		// This is beautiful enough :)
		double minX = Math.min(Math.min(Math.min(Math.min(Math.min(axisX.x, axisY.x), axisZ.x), -axisX.x), -axisY.x), -axisZ.x);
		double minY = Math.min(Math.min(Math.min(Math.min(Math.min(axisX.y, axisY.y), axisZ.y), -axisX.y), -axisY.y), -axisZ.y);
		double minZ = Math.min(Math.min(Math.min(Math.min(Math.min(axisX.z, axisY.z), axisZ.z), -axisX.z), -axisY.z), -axisZ.z);
		double maxX = Math.max(Math.max(Math.max(Math.max(Math.max(axisX.x, axisY.x), axisZ.x), -axisX.x), -axisY.x), -axisZ.x);
		double maxY = Math.max(Math.max(Math.max(Math.max(Math.max(axisX.y, axisY.y), axisZ.y), -axisX.y), -axisY.y), -axisZ.y);
		double maxZ = Math.max(Math.max(Math.max(Math.max(Math.max(axisX.z, axisY.z), axisZ.z), -axisX.z), -axisY.z), -axisZ.z);
		AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(center.x, center.y, center.z);

		return serverLevel.<MagnetEntity>getEntities(MagnetEntity.TESTER, box, (magnet) -> {
			Vec3 position = magnet.position();
			if (center.distanceSquared(position.x, position.y, position.z) > maxDistanceSqr) {
				return false;
			}
			MagnetBlockEntity block = magnet.getAttachedBlock();
			if (block == null || block == this) {
				return false;
			}
			ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, magnet.getAttachedBlockPos());
			if (ship == currentShip) {
				return false;
			}
			return true;
		});
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction direction) {
		if (cap == ForgeCapabilities.ENERGY) {
			return LazyOptional.of(() -> this.energyStorage).cast();
		} else if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (!lazyPeripheral.isPresent()) {
				lazyPeripheral = LazyOptional.of(() -> new MagnetPeripheral(this));
			}
			return lazyPeripheral.cast();
		}
		return super.getCapability(cap, direction);
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putFloat("Power", this.power);
		tag.putInt("StoredEnergy", this.energyStorage.stored);
	}

	@Override
	public void load(CompoundTag tag) {
		this.power = tag.getFloat("Power");
		this.energyStorage.stored = tag.getInt("StoredEnergy");
		super.load(tag);
	}

	public void neighborChanged(Block block, BlockPos pos, boolean moving) {
		if (!this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
	}

	private Vector3d getNorthPolePos() {
			final Vector3d selfPos = this.getWorldPos();
			final Vector3f selfFacing = this.getFacing().mul(0.4f);
			return new Vector3d(selfPos).add(selfFacing);
	}

	private Vector3d getSouthPolePos() {
			final Vector3d selfPos = this.getWorldPos();
			final Vector3f selfFacing = this.getFacing().mul(0.4f);
			return new Vector3d(selfPos).sub(selfFacing);
	}

	@Override
	public void tickForce(final ServerLevel level, final BlockPos blockPos, final BlockState state) {
		super.tickForce(level, blockPos, state);

		final BlockState updatingState = this.updatingState.getAndSet(null);
		if (updatingState != null) {
			level.setBlockAndUpdate(blockPos, updatingState);
		}

		if (this.wasPeripheralMode != this.isPeripheralMode && !this.isPeripheralMode) {
			this.updatePowerByRedstone();
		}
		this.wasPeripheralMode = this.isPeripheralMode;

		// TODO: find a proper way to check if the facing is changed
		boolean facingChanged = false;
		if (facingChanged) {
			this.facing = Vec3.atLowerCornerOf(state.getValue(DirectionalBlock.FACING).getNormal());
			this.magnetData.facing = this.facing.toVector3f();
		}

		if (this.isGenerator) {
			int lastGenerated = (int) this.lastGenerated.sumThenReset();
			lastGenerated -= MAGNET_GENERATE_LOSS;
			if (lastGenerated > 0) {
				this.energyStorage.stored = Math.min(this.energyStorage.stored + lastGenerated, this.energyStorage.maxEnergyRate);
			}
		} else {
			// Determine the energy required for this tick
			float requiredEnergy = this.power * this.energyStorage.maxEnergyRate;

			if (requiredEnergy == 0) {
				// No energy needed (aka we store 0 energy), directly set tickPower
				this.tickPower = this.power;
			} else {
				// Clamp required energy within the available stored energy range
				if (requiredEnergy < 0) {
					requiredEnergy = Math.max(requiredEnergy, -this.energyStorage.stored);
				} else {
					requiredEnergy = Math.min(requiredEnergy, this.energyStorage.stored);
				}

				// Consume the energy
				this.energyStorage.stored -= (int) Math.abs(requiredEnergy);
				this.tickPower = requiredEnergy / this.energyStorage.maxEnergyRate;
			}
		}

		Ship selfShip = VSGameUtilsKt.getShipObjectManagingPos(level, blockPos);
		if (selfShip == null) {
			return;
		}

		if (this.needInit) {
			VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, blockPos);
			if (ships != null && ships.getThrusterAtPos(blockPos) == null) {
				ships.addMagnet(blockPos, this.magnetData);
				this.needInit = false;
			}
		}

		this.magnetData.isGenerator = this.isGenerator;
		float selfPower = this.getActivatablePower();
		if (!this.isGenerator && selfPower == 0) {
			this.magnetData.forceCalculator = MagnetData.EMPTY_FORCE;
			return;
		}
		List<MagnetEntity> magnets = this.scanMagnets();
		Stream<MagnetBlockEntity> magnetStream = magnets.stream().map(MagnetEntity::getAttachedBlock).filter((b) -> b != null);
		if (this.isGenerator) {
			magnetStream = magnetStream.filter((b) -> b.isGenerator);
		} else {
			magnetStream = magnetStream.filter((b) -> !b.isGenerator && b.getActivatablePower() != 0);
		}
		List<MagnetBlockEntity> magnetBlocks = magnetStream.collect(Collectors.toList());
		if (magnetBlocks.size() == 0) {
			this.magnetData.forceCalculator = MagnetData.EMPTY_FORCE;
		} else if (this.isGenerator) {
			this.magnetData.forceCalculator = (physShip, totalForce, totalTorque) -> {};
			// final double MAX_FORCE_MULTIPLIER = VSCHConfig.MAGNET_BLOCK_MAX_FORCE.get().doubleValue();
			// Vector3d selfPosInShip = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5).sub(selfShip.getTransform().getPositionInShip());
			// double selfRadius = selfPosInShip.length();
			// this.magnetData.forceCalculator = (physShip, totalForce, totalTorque) -> {
			// 	final double tps = 3 * 20;
			// 	final double dt = 1.0 / tps;
			// 	final Vector3d selfPos = this.getWorldPos();
			// 	final double shipMass = physShip.getInertia().getShipMass();
			// 	double forceMultiplier = MAX_FORCE_MULTIPLIER * 2;
			// 	Vector3dc selfLinearVel = physShip.getPoseVel().getVel();
			// 	final Vector3dc omega = physShip.getPoseVel().getOmega();
			// 	// TODO: need fix
			// 	final Vector3d selfAngularVel = omega.mul(selfRadius, new Vector3d());
			// 	selfAngularVel.set(
			// 		selfAngularVel.y * Math.sin(Math.atan2(selfPosInShip.z, selfPosInShip.x) + Math.PI / 2) +
			// 			selfAngularVel.z * Math.sin(Math.atan2(selfPosInShip.x, selfPosInShip.y) + Math.PI / 2),
			// 		selfAngularVel.x * Math.sin(Math.atan2(selfPosInShip.y, selfPosInShip.z) + Math.PI / 2) +
			// 			selfAngularVel.z * Math.cos(Math.atan2(selfPosInShip.x, selfPosInShip.y) + Math.PI / 2),
			// 		selfAngularVel.y * Math.cos(Math.atan2(selfPosInShip.z, selfPosInShip.x) + Math.PI / 2) +
			// 			selfAngularVel.x * Math.cos(Math.atan2(selfPosInShip.y, selfPosInShip.z) + Math.PI / 2)
			// 	);
			// 	selfShip.getShipToWorld().transformDirection(selfAngularVel);
			// 	Vector3d selfVelocity = new Vector3d(selfAngularVel);
			// 	double linearDist = selfLinearVel.length();
			// 	double angularDist = selfVelocity.length();
			// 	if (linearDist == 0 && angularDist == 0) {
			// 		return;
			// 	}
			// 	double linearWeight = linearDist / (linearDist + angularDist);
			// 	selfVelocity.add(selfLinearVel);
			// 	this.lastVeloctiy = new Vector3d(selfVelocity);
			// 	Vector3d velocityDiff = new Vector3d();
			// 	for (MagnetBlockEntity block : magnetBlocks) {
			// 		Vector3d pos = block.getWorldPos();
			// 		Vector3d otherVelocity = block.lastVeloctiy;
			// 		double dist = selfPos.distanceSquared(pos);

			// 		otherVelocity.sub(selfVelocity, velocityDiff).mul(0.5);
			// 		block.lastGenerated.add(MAGNET_GENERATE_RATE * velocityDiff.length() * dt / dist);

			// 		velocityDiff.mul(forceMultiplier / dist);
			// 		Vector3d changingVel = velocityDiff.mul(dt / shipMass, new Vector3d());
			// 		totalForce.add(velocityDiff.mul(linearWeight, new Vector3d()));
			// 		if (angularDist > 0 && selfRadius > 0) {
			// 			velocityDiff.mul(1 - linearWeight);
			// 			selfShip.getWorldToShip().transformDirection(velocityDiff);
			// 			// TODO: need fix
			// 			totalTorque.add(-omega.x(), -omega.y(), -omega.z());
			// 		}

			// 		selfVelocity.add(changingVel);
			// 		if (selfVelocity.lengthSquared() < 1e-6) {
			// 			break;
			// 		}
			// 	}
			// };
		} else {
			this.magnetData.forceCalculator = (physShip, frontForce, backForce) -> {
				final Vector3d selfPos = this.getWorldPos();
				final Vector3f selfFacing = this.getFacing();
				final Vector3d selfNorthPos = this.getNorthPolePos();
				final Vector3d selfSouthPos = this.getSouthPolePos();
				final Vector3d forceDest = new Vector3d();
				final Vector3d northTotalForce = new Vector3d();
				final Vector3d southTotalForce = new Vector3d();
				for (MagnetBlockEntity other : magnetBlocks) {
					DuoVector3d cachedForce = this.cachedForces.get(other);
					if (cachedForce != null) {
						frontForce.add(cachedForce.north());
						backForce.add(cachedForce.south());
						continue;
					}

					final Vector3d pos = other.getWorldPos();
					final Vector3f facing = other.getFacing();
					final Vector3d otherNorthPos = other.getNorthPolePos();
					final Vector3d otherSouthPos = other.getSouthPolePos();
					final float angle = selfFacing.angle(facing);
					final float power = selfPower * other.getActivatablePower();

					// N-N
					getStandardForceTo(selfNorthPos, angle, otherNorthPos, forceDest).mul(-power);
					northTotalForce.set(forceDest);

					// N-S
					getStandardForceTo(selfNorthPos, angle, otherSouthPos, forceDest).mul(power);
					northTotalForce.add(forceDest);

					// S-N
					getStandardForceTo(selfSouthPos, angle, otherNorthPos, forceDest).mul(power);
					southTotalForce.set(forceDest);

					// S-S
					getStandardForceTo(selfSouthPos, angle, otherSouthPos, forceDest).mul(-power);
					southTotalForce.add(forceDest);

					frontForce.add(northTotalForce);
					backForce.add(southTotalForce);
					if (VSGameUtilsKt.isBlockInShipyard(other.getLevel(), other.getBlockPos())) {
						other.cachedForces.put(this, new DuoVector3d(northTotalForce.negate(new Vector3d()), southTotalForce.negate(new Vector3d())));
					}
				}
				this.cachedForces.clear();
			};
		}
	}

	private void updatePowerByRedstone() {
		float newPower = getPowerByRedstone(this.getLevel(), this.getBlockPos());
		this.setPower(newPower);
	}

	private static float getPowerByRedstone(Level level, BlockPos pos) {
		int signal = level.getBestNeighborSignal(pos);
		return signal == 0 ? -1 : (float)(signal - 1) / 14 * 2 - 1;
	}

	private class MagnetEnergyStorage implements IEnergyStorage {
		final int maxEnergyRate;
		int stored = 0;

		MagnetEnergyStorage(int maxEnergyRate) {
			this.maxEnergyRate = maxEnergyRate;
		}

		@Override
		public boolean canReceive() {
			return !MagnetBlockEntity.this.isGenerator;
		}

		@Override
		public int receiveEnergy(int avaliable, boolean simulate) {
			int received = this.maxEnergyRate - this.stored;
			if (received > avaliable) {
				received = avaliable;
			}
			if (!simulate) {
				this.stored += received;
			}
			return received;
		}

		@Override
		public int getEnergyStored() {
			return this.stored;
		}

		@Override
		public int getMaxEnergyStored() {
			return this.maxEnergyRate;
		}

		@Override
		public boolean canExtract() {
			return MagnetBlockEntity.this.isGenerator;
		}

		@Override
		public int extractEnergy(int require, boolean simulate) {
			if (require > this.stored) {
				require = this.stored;
			}
			if (!simulate) {
				this.stored -= require;
			}
			return require;
		}
	}

	private static record DuoVector3d(Vector3d north, Vector3d south) {}
}
