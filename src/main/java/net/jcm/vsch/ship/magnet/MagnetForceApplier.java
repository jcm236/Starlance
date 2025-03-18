package net.jcm.vsch.ship.magnet;

import net.jcm.vsch.api.force.IVSCHForceApplier;
import net.jcm.vsch.config.VSCHConfig;

import net.minecraft.core.BlockPos;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
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
		final Vector3d centerPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		final Vector3f facing = data.facing;
		final boolean isGenerator = data.isGenerator;
		final MagnetData.ForceCalculator forceCalculator = data.forceCalculator;
		final Vector3d frontForce = new Vector3d();
		final Vector3d backForce = new Vector3d();
		forceCalculator.calc(physShip, frontForce, backForce);
		if (isGenerator) {
			physShip.applyInvariantForce(frontForce);
			physShip.applyInvariantTorque(backForce);
			return;
		}

		final boolean hasFrontForce = frontForce.lengthSquared() != 0;
		final boolean hasBackForce = backForce.lengthSquared() != 0;
		if (!hasFrontForce && !hasBackForce) {
			return;
		}
		final ShipTransform transform = physShip.getTransform();
		final Vector3d frontPos = new Vector3d(facing.x / 2, facing.y / 2, facing.z / 2).add(centerPos).sub(transform.getPositionInShip());
		final Vector3d backPos = frontPos.sub(facing, new Vector3d());

		// TODO: add speed limit

		if (hasFrontForce) {
			physShip.applyInvariantForceToPos(frontForce, frontPos);
		}
		if (hasBackForce) {
			physShip.applyInvariantForceToPos(backForce, backPos);
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
