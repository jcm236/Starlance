/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
package net.jcm.vsch.ship.gyro;

import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.IVSCHForceApplier;
import net.minecraft.core.BlockPos;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;

public final class GyroForceApplier implements IVSCHForceApplier {
	private final GyroData data;

	public GyroForceApplier(GyroData data) {
		this.data = data;
	}

	public GyroData getData() {
		return this.data;
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
		ship.applyBodyTorque(this.data.torque);
	}
}
