package net.jcm.vsch.ship.thruster;

import net.jcm.vsch.api.force.IVSCHForceApplier;
import net.jcm.vsch.config.VSCHConfig;

import net.minecraft.core.BlockPos;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class ThrusterForceApplier implements IVSCHForceApplier {
	private ThrusterData data;

	public ThrusterData getData(){
		return this.data;
	}

	public ThrusterForceApplier(ThrusterData data){
		this.data = data;
	}

	@Override
	public void applyForces(BlockPos pos, PhysShipImpl physShip) {
		final Vector3d force = data.force;
		if (force.lengthSquared() == 0.0f) {
			return;
		}

		// Transform force direction from ship relative to world relative
		Vector3d tForce = physShip.getTransform().getShipToWorld().transformDirection(force, new Vector3d());

		if (VSCHConfig.LIMIT_SPEED.get()) {
			final int maxSpeed = VSCHConfig.MAX_SPEED.get().intValue();
			final Vector3dc linearVelocity = physShip.getPoseVel().getVel();
			if (linearVelocity.lengthSquared() >= maxSpeed * maxSpeed && tForce.dot(linearVelocity) > 0) {
				switch (data.mode) {
					case GLOBAL -> applyScaledForce(physShip, linearVelocity, tForce, maxSpeed);
					case POSITION -> {
						Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
							.add(0.5, 0.5, 0.5)
							.sub(physShip.getTransform().getPositionInShip());

						Vector3d parallel = new Vector3d(tPos).mul(tForce.dot(tPos) / tForce.dot(tForce));
						Vector3d perpendicular = new Vector3d(tForce).sub(parallel);

						physShip.applyInvariantForceToPos(perpendicular, tPos);

						// apply global force, since the force is perfectly lined up with the centre of gravity
						applyScaledForce(physShip, linearVelocity, parallel, maxSpeed);
					}
				}
				return;
			}
		}

		// Switch between applying force at position and just applying the force
		switch (data.mode) {
			case GLOBAL -> physShip.applyInvariantForce(tForce);
			case POSITION -> {
				Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
					.add(0.5, 0.5, 0.5)
					.sub(physShip.getTransform().getPositionInShip());
				physShip.applyInvariantForceToPos(tForce, tPos);
			}
		}
	}

	private static void applyScaledForce(PhysShipImpl physShip, Vector3dc linearVelocity, Vector3d tForce, int maxSpeed) {
		assert ValkyrienSkiesMod.getCurrentServer() != null;
		double deltaTime = 1.0 / (VSGameUtilsKt.getVsPipeline(ValkyrienSkiesMod.getCurrentServer()).computePhysTps());
		double mass = physShip.getInertia().getShipMass();

		// Invert the parallel projection of tForce onto linearVelocity and scales it so that the resulting speed is exactly
		// equal to length of linearVelocity, but still in the direction the ship would have been going without the speed limit
		Vector3d targetVelocity = (new Vector3d(linearVelocity).add(new Vector3d(tForce).mul(deltaTime / mass)).normalize(maxSpeed)).sub(linearVelocity);

		// Apply the force at no specific position
		physShip.applyInvariantForce(targetVelocity.mul(mass / deltaTime));
	}
}
