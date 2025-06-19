package net.jcm.vsch.ship.gyro;

import net.jcm.vsch.api.force.IVSCHForceApplier;
import net.jcm.vsch.config.VSCHConfig;
import net.minecraft.core.BlockPos;
import org.joml.Vector3dc;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

public class GyroForceApplier implements IVSCHForceApplier {
	private final GyroData data;

	public GyroData getData() {
		return this.data;
	}

	public GyroForceApplier(GyroData data){
		this.data = data;
	}

	@Override
	public void applyForces(BlockPos pos, PhysShipImpl physShip) {
		Vector3dc angularVelocity = physShip.getPoseVel().getOmega();
		if (VSCHConfig.GYRO_LIMIT_SPEED.get()) {
			if (Math.abs(angularVelocity.length()) >= VSCHConfig.GYRO_MAX_SPEED.get().doubleValue()) {
				//TODO: someone smarter than me fix this
				return;
			}
		}
		physShip.applyRotDependentTorque(this.data.torque);
	}
}
