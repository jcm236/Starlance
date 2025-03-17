package net.jcm.vsch.ship;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.jcm.vsch.api.force.IVSCHForceApplier;
import net.jcm.vsch.ship.dragger.DraggerData;
import net.jcm.vsch.ship.dragger.DraggerForceApplier;
import net.jcm.vsch.ship.thruster.ThrusterData;
import net.jcm.vsch.ship.thruster.ThrusterForceApplier;
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

	private static void applyScaledForce(PhysShipImpl physShip, Vector3dc linearVelocity, Vector3d tForce, int maxSpeed) {
		assert ValkyrienSkiesMod.getCurrentServer() != null;
		double deltaTime = 1.0 / (VSGameUtilsKt.getVsPipeline(ValkyrienSkiesMod.getCurrentServer()).computePhysTps());
		double mass = physShip.getInertia().getShipMass();

		//Invert the parallel projection of tForce onto linearVelocity and scales it so that the resulting speed is exactly
		// equal to length of linearVelocity, but still in the direction the ship would have been going without the speed limit
		Vector3d targetVelocity = (new Vector3d(linearVelocity).add(new Vector3d(tForce).mul(deltaTime / mass)).normalize(maxSpeed)).sub(linearVelocity);

		// Apply the force at no specific position
		physShip.applyInvariantForce(targetVelocity.mul(mass / deltaTime));
	}

	// ----- Force Appliers ----- //

	public void addApplier(BlockPos pos, IVSCHForceApplier applier){
		appliers.put(pos,applier);
	}

	public void removeApplier(BlockPos pos){
		appliers.remove(pos);
	}

	@Nullable
	public IVSCHForceApplier getApplierAtPos(BlockPos pos){
		return appliers.get(pos);
	}

	// ----- Thrusters ----- //

	public void addThruster(BlockPos pos, ThrusterData data) {
		 addApplier(pos,new ThrusterForceApplier(data));
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
