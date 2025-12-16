package net.jcm.vsch.ship.gyro;

import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.IVSCHForceApplier;
import net.minecraft.core.BlockPos;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;

public class GyroForceApplier implements IVSCHForceApplier {
	private final GyroData data;

	public GyroData getData() {
		return this.data;
	}

	public GyroForceApplier(GyroData data){
		this.data = data;
	}

	@Override
	public void applyForces(BlockPos pos, PhysShip ship, PhysLevel level) {
		Vector3dc angularVelocity = ship.getAngularVelocity();
		if (VSCHServerConfig.GYRO_LIMIT_SPEED.get()) {
			if (Math.abs(angularVelocity.length()) >= VSCHServerConfig.GYRO_MAX_SPEED.get().doubleValue()) {
				//TODO: someone smarter than me fix this
				return;
			}
		}
		ship.applyRotDependentTorque(this.data.torque);
	}
}
