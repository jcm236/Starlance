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

	private final DraggerData data;

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

		final double s = ship.getTransform().getShipToWorldScaling().x();

		final Vector3d force = linearVelocity.mul(-ship.getMass(), new Vector3d());

		// Mass is scaled by s^3
		final double maxDrag = VSCHServerConfig.MAX_DRAG.get().intValue() * s * s * s;
		if (force.lengthSquared() > maxDrag * maxDrag) {
			force.normalize(maxDrag);
		}

		// Torques scale by scaling^5
		final Vector3d rotForce = angularVelocity.mul(-ship.getMass(), new Vector3d()).mul(s * s);

		VSCHUtils.clampVector(rotForce, VSCHServerConfig.MAX_DRAG.get().intValue());

		ship.applyWorldForceToBodyPos(force, new Vector3d());
		ship.applyWorldTorque(rotForce);
	}
}
