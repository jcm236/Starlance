package net.jcm.vsch.ship;

import net.jcm.vsch.ship.dragger.DraggerData;
import net.jcm.vsch.ship.dragger.DraggerForceApplier;
import net.jcm.vsch.ship.gyro.GyroData;
import net.jcm.vsch.ship.gyro.GyroForceApplier;
import net.jcm.vsch.ship.thruster.ThrusterData;
import net.jcm.vsch.ship.thruster.ThrusterForceApplier;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.*;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class VSCHForceInducedShips implements ShipPhysicsListener {

	/**
	 * Don't mess with this unless you know what your doing. I'm making it public for all the people that do know what their doing.
	 * Instead, look at {@link #addApplier(BlockPos, IVSCHForceApplier)} or {@link #removeApplier(BlockPos)} or {@link #getApplierAtPos(BlockPos)} or their respective thruster/dragger counterparts.
	 * @see IVSCHForceApplier
	 */
	public Map<BlockPos, IVSCHForceApplier> appliers = new ConcurrentHashMap<>();

	public VSCHForceInducedShips() {}

	@Override
	public void physTick(@NotNull PhysShip ship, @NotNull PhysLevel physLevel) {
		appliers.forEach((pos, applier) -> {
			applier.applyForces(pos, ship, physLevel);
		});
	}

	// ----- Force Appliers ----- //

	public void addApplier(BlockPos pos, IVSCHForceApplier applier) {
		appliers.put(pos, applier);
	}

	public void removeApplier(BlockPos pos) {
		appliers.remove(pos);
	}

	@Nullable
	public IVSCHForceApplier getApplierAtPos(BlockPos pos) {
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

	public static VSCHForceInducedShips get(LoadedServerShip ship) {
		VSCHForceInducedShips attachment = ship.getAttachment(VSCHForceInducedShips.class);
		if (attachment == null) {
			attachment = new VSCHForceInducedShips();
			ship.setAttachment(attachment);
		}
		return attachment;
	}

	public static VSCHForceInducedShips get(ServerLevel level, BlockPos pos) {
		LoadedServerShip ship = VSGameUtilsKt.getLoadedShipManagingPos(level, pos);
		return ship != null ? get(ship) : null;
	}
}
