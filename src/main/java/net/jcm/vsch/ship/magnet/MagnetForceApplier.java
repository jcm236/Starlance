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
package net.jcm.vsch.ship.magnet;

import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.IVSCHForceApplier;

import net.minecraft.core.BlockPos;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.api.world.PhysLevel;

public final class MagnetForceApplier implements IVSCHForceApplier {
	private static final Vector3dc ZERO_VEC3D = new Vector3d();

	private final MagnetData data;

	public MagnetForceApplier(MagnetData data) {
		this.data = data;
	}

	public MagnetData getData() {
		return this.data;
	}

	@Override
	public void applyForces(final BlockPos blockPos, final PhysShip ship, final PhysLevel physLevel) {
		final Vector3d centerPos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
		final Vector3f facing = data.facing;
		final boolean isGenerator = data.isGenerator;
		final MagnetData.ForceCalculator forceCalculator = data.forceCalculator;
		final Vector3d frontForce = new Vector3d();
		final Vector3d backForce = new Vector3d();
		forceCalculator.calc(ship, frontForce, backForce);
		if (isGenerator) {
			ship.applyWorldForceToBodyPos(frontForce, ZERO_VEC3D);
			ship.applyWorldTorque(backForce);
			return;
		}

		final boolean hasFrontForce = frontForce.lengthSquared() != 0;
		final boolean hasBackForce = backForce.lengthSquared() != 0;
		if (!hasFrontForce && !hasBackForce) {
			return;
		}
		final ShipTransform transform = ship.getTransform();
		final Vector3d frontPos = new Vector3d(facing.x / 2, facing.y / 2, facing.z / 2).add(centerPos).sub(transform.getPositionInShip());
		final Vector3d backPos = frontPos.sub(facing, new Vector3d());

		// TODO: add speed limit

		if (hasFrontForce) {
			ship.applyWorldForceToBodyPos(frontForce, frontPos);
		}
		if (hasBackForce) {
			ship.applyWorldForceToBodyPos(backForce, backPos);
		}
	}

	private static void applyScaledForce(final PhysShip ship, final Vector3dc velocity, final Vector3dc force, final int maxSpeed) {
		final double deltaTime = 1.0 / (20 * 3);
		final double mass = ship.getMass();

		// Invert the parallel projection of force onto velocity and scales it so that the resulting speed is exactly
		// equal to length of velocity, but still in the direction the ship would have been going without the speed limit
		final Vector3d targetVelocity = force.mul(deltaTime / mass, new Vector3d())
			.add(velocity)
			.normalize(maxSpeed)
			.sub(velocity);

		// Apply the force at the center of gravity
		ship.applyWorldForceToBodyPos(targetVelocity.mul(mass / deltaTime), ZERO_VEC3D);
	}
}
