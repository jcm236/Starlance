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

public final class DraggerForceApplier implements IVSCHForceApplier {
	private final DraggerData data;

	public DraggerForceApplier(DraggerData data) {
		this.data = data;
	}

	public DraggerData getData() {
		return this.data;
	}

	@Override
	public void applyForces(BlockPos pos, PhysShip ship, PhysLevel physLevel) {
		if (!data.on) {
			return;
		}

		final double mass = ship.getMass();
		final double scale = ship.getTransform().getShipToWorldScaling().x();

		// Scale max drag to induce the same acceleration regardless of ship scale
		final double maxDrag = VSCHServerConfig.MAX_DRAG.get().intValue() * scale * scale * scale;

		final Vector3dc linearVelocity = ship.getVelocity();
		final Vector3dc angularVelocity = ship.getAngularVelocity();

		final Vector3d force = linearVelocity.mul(-mass, new Vector3d());

		if (force.lengthSquared() > maxDrag * maxDrag) {
			force.normalize(maxDrag);
		}

		// Torques scale by scale^5
		// since mass already accounts for scale^3, its only necessary to multiply by scale^2
		final Vector3d rotForce = angularVelocity.mul(-mass, new Vector3d()).mul(scale * scale);
		VSCHUtils.clampVector(rotForce, maxDrag);

		ship.applyWorldForceToBodyPos(force, new Vector3d());
		ship.applyWorldTorque(rotForce);
	}
}
