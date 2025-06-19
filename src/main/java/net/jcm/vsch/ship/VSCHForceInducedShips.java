package net.jcm.vsch.ship;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.jcm.vsch.api.force.IVSCHForceApplier;
import net.jcm.vsch.ship.dragger.DraggerData;
import net.jcm.vsch.ship.dragger.DraggerForceApplier;
import net.jcm.vsch.ship.thruster.ThrusterData;
import net.jcm.vsch.ship.thruster.ThrusterForceApplier;
import net.jcm.vsch.ship.gyro.GyroData;
import net.jcm.vsch.ship.gyro.GyroForceApplier;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@SuppressWarnings("deprecation")
public class VSCHForceInducedShips implements ShipForcesInducer {

	/**
	 * Don't mess with this unless you know what your doing. I'm making it public for all the people that do know what their doing.
	 * Instead, look at {@link #addApplier(BlockPos, IVSCHForceApplier)} or {@link #removeApplier(BlockPos)} or {@link #getApplierAtPos(BlockPos)} or their respective thruster/dragger counterparts.
	 * @see IVSCHForceApplier
	 */
	public Map<BlockPos, IVSCHForceApplier> appliers = new ConcurrentHashMap<>();


	private String dimensionId = null;

	public VSCHForceInducedShips() {}

	public VSCHForceInducedShips(String dimensionId) {
		this.dimensionId = dimensionId;
	}

	@Override
	public void applyForces(@NotNull PhysShip physicShip) {
		PhysShipImpl physShip = (PhysShipImpl) physicShip;
		appliers.forEach((pos,applier) -> {
			applier.applyForces(pos,physShip);
		});
	}

	// ----- Force Appliers ----- //

	public void addApplier(BlockPos pos, IVSCHForceApplier applier){
		appliers.put(pos, applier);
	}

	public void removeApplier(BlockPos pos){
		appliers.remove(pos);
	}

	@Nullable
	public IVSCHForceApplier getApplierAtPos(BlockPos pos){
		return appliers.get(pos);
	}

	// ----- Draggers ----- //

	public void addDragger(BlockPos pos, DraggerData data) {
		addApplier(pos, new DraggerForceApplier(data));
	}

	public void removeDragger(BlockPos pos) {
		if (getDraggerAtPos(pos) != null){
			removeApplier(pos);
		}
	}

	@Nullable
	public DraggerData getDraggerAtPos(BlockPos pos) {
		IVSCHForceApplier applier = getApplierAtPos(pos);
		if (applier instanceof DraggerForceApplier dragger) {
			return dragger.getData();
		} else {
			return null;
		}
	}

	// ----- Thrusters ----- //

	public void addThruster(BlockPos pos, ThrusterData data) {
		 addApplier(pos, new ThrusterForceApplier(data));
	}


	public void removeThruster(BlockPos pos) {
		if (getThrusterAtPos(pos) != null){
			removeApplier(pos);
		}
	}

	@Nullable
	public ThrusterData getThrusterAtPos(BlockPos pos) {
		IVSCHForceApplier applier = getApplierAtPos(pos);
		if (applier instanceof ThrusterForceApplier thruster) {
			return thruster.getData();
		} else {
			return null;
		}
	}

	// ----- Gyros ----- //

	public void addGyro(BlockPos pos, GyroData data) {
		 addApplier(pos, new GyroForceApplier(data));
	}


	public void removeGyro(BlockPos pos) {
		if (getGyroAtPos(pos) != null){
			removeApplier(pos);
		}
	}

	@Nullable
	public GyroData getGyroAtPos(BlockPos pos) {
		IVSCHForceApplier applier = getApplierAtPos(pos);
		if (applier instanceof GyroForceApplier gyro) {
			return gyro.getData();
		} else {
			return null;
		}
	}

	// ----- Force induced ships ----- //

	public static VSCHForceInducedShips getOrCreate(ServerShip ship, String dimensionId) {
		VSCHForceInducedShips attachment = ship.getAttachment(VSCHForceInducedShips.class);
		if (attachment == null) {
			attachment = new VSCHForceInducedShips(dimensionId);
			ship.saveAttachment(VSCHForceInducedShips.class, attachment);
		}
		return attachment;
	}

	public static VSCHForceInducedShips getOrCreate(ServerShip ship) {
		return getOrCreate(ship, ship.getChunkClaimDimension());
	}

	public static VSCHForceInducedShips get(Level level, BlockPos pos) {
		ServerLevel serverLevel = (ServerLevel) level;
		// Don't ask, I don't know
		ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos(serverLevel, pos);
		if (ship == null) {
			ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
		}
		// Seems counter-intutive at first. But basically, it returns null if it wasn't a ship. Otherwise, it gets the attachment OR creates and then gets it
		return ship != null ? getOrCreate(ship) : null;
	}
}
