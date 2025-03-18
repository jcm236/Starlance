package net.jcm.vsch.ship.dragger;

import net.jcm.vsch.api.force.IVSCHForceApplier;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

public class DraggerForceApplier implements IVSCHForceApplier {

	private DraggerData data;

	public DraggerForceApplier(DraggerData data) {
		this.data = data;
	}

	public DraggerData getData(){
		return this.data;
	}

	@Override
	public void applyForces(BlockPos pos, PhysShipImpl physShip) {
		Vector3dc linearVelocity = physShip.getPoseVel().getVel();
		Vector3dc angularVelocity = physShip.getPoseVel().getOmega();

		if (!data.on) {
			return;
		}

		Vector3d acceleration = linearVelocity.negate(new Vector3d());
		Vector3d force = acceleration.mul(physShip.getInertia().getShipMass());

		force = VSCHUtils.clampVector(force, VSCHConfig.MAX_DRAG.get().intValue());

		Vector3d rotAcceleration = angularVelocity.negate(new Vector3d());
		Vector3d rotForce = rotAcceleration.mul(physShip.getInertia().getShipMass());

		rotForce = VSCHUtils.clampVector(rotForce, VSCHConfig.MAX_DRAG.get().intValue());

		physShip.applyInvariantForce(force);
		physShip.applyInvariantTorque(rotForce);
	}
}
