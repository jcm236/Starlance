package net.jcm.vsch.ship.dragger;

import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.IVSCHForceApplier;
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
		if (!data.on) {
			return;
		}

		final Vector3dc linearVelocity = physShip.getPoseVel().getVel();
		final Vector3dc angularVelocity = physShip.getPoseVel().getOmega();

		final Vector3d force = linearVelocity.mul(-physShip.getInertia().getShipMass(), new Vector3d());

		final double maxDrag = VSCHServerConfig.MAX_DRAG.get().intValue();
		if (force.lengthSquared() > maxDrag * maxDrag) {
			force.normalize(maxDrag);
		}

		final Vector3d rotForce = angularVelocity.mul(-physShip.getInertia().getShipMass(), new Vector3d());

		VSCHUtils.clampVector(rotForce, VSCHServerConfig.MAX_DRAG.get().intValue());

		physShip.applyInvariantForce(force);
		physShip.applyInvariantTorque(rotForce);
	}
}
