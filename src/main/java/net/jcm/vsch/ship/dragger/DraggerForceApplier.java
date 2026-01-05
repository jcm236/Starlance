package net.jcm.vsch.ship.dragger;

import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.IVSCHForceApplier;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;

public class DraggerForceApplier implements IVSCHForceApplier {

	private DraggerData data;

	public DraggerForceApplier(DraggerData data) {
		this.data = data;
	}

	public DraggerData getData(){
		return this.data;
	}

	@Override
	public void applyForces(BlockPos pos, PhysShip ship, PhysLevel physLevel) {
		if (!data.on) {
			return;
		}

		final Vector3dc linearVelocity = ship.getVelocity();
		final Vector3dc angularVelocity = ship.getAngularVelocity();

		final Vector3d force = linearVelocity.mul(-ship.getMass(), new Vector3d());

		final double maxDrag = VSCHServerConfig.MAX_DRAG.get().intValue();
		if (force.lengthSquared() > maxDrag * maxDrag) {
			force.normalize(maxDrag);
		}

		final Vector3d rotForce = angularVelocity.mul(-ship.getMass(), new Vector3d());

		VSCHUtils.clampVector(rotForce, VSCHServerConfig.MAX_DRAG.get().intValue());

		ship.applyInvariantForce(force);
		ship.applyInvariantTorque(rotForce);
	}
}
