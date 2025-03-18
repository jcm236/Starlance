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

public class MagnetForceApplier implements IVSCHForceApplier {
	private MagnetData data;

	public MagnetData getData(){
		return this.data;
	}

	public MagnetForceApplier(MagnetData data){
		this.data = data;
	}

	@Override
	public void applyForces(BlockPos pos, PhysShipImpl physShip) {
		// Get current thrust from thruster
		float throttle = data.throttle;
		if (throttle == 0.0f) {
			return;
		}

		// Transform force direction from ship relative to world relative
		Vector3d tForce = physShip.getTransform().getShipToWorld().transformDirection(data.dir, new Vector3d());
		tForce.mul(throttle);

		Vector3dc linearVelocity = physShip.getPoseVel().getVel();

		if (VSCHConfig.LIMIT_SPEED.get()) {
			int maxSpeed = VSCHConfig.MAX_SPEED.get().intValue();
			if (Math.abs(linearVelocity.length()) >= maxSpeed) {
				double dotProduct = tForce.dot(linearVelocity);
				if (dotProduct > 0) {
					Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
							.add(0.5, 0.5, 0.5, new Vector3d())
							.sub(physShip.getTransform().getPositionInShip());
					Vector3d parallel = new Vector3d(tPos).mul(tForce.dot(tPos) / tForce.dot(tForce));
					Vector3d perpendicular = new Vector3d(tForce).sub(parallel);

					// rotate the ship
					physShip.applyInvariantForceToPos(perpendicular, tPos);

					// apply global force, since the force is perfectly lined up with the centre of gravity
					applyScaledForce(physShip, linearVelocity, parallel, maxSpeed);
					return;
				}
			}
		}

		Vector3d tPos = VectorConversionsMCKt.toJOMLD(pos)
				.add(0.5, 0.5, 0.5, new Vector3d())
				.sub(physShip.getTransform().getPositionInShip());

		physShip.applyInvariantForceToPos(tForce, tPos);
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
