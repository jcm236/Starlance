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
package net.jcm.vsch.ship.thruster;

import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.IVSCHForceApplier;

import net.minecraft.core.BlockPos;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public final class ThrusterForceApplier implements IVSCHForceApplier {
	private static final Vector3dc ZERO_VEC3D = new Vector3d();

	private final ThrusterData data;

	public ThrusterForceApplier(final ThrusterData data) {
		this.data = data;
	}

	public ThrusterData getData() {
		return this.data;
	}

	@Override
	public void applyForces(final BlockPos blockPos, final PhysShip ship, final PhysLevel physLevel) {
		final double throttle = data.throttle;
		if (throttle == 0) {
			return;
		}

		// Transform force direction from ship relative to world relative
		final ShipTransform transform = ship.getTransform();
		final Vector3dc force = transform.getShipToWorld().transformDirection(data.dir, new Vector3d()).mul(throttle);

		final Vector3dc velocity = ship.getVelocity();
		final Vector3dc scaling = transform.getShipToWorldScaling();

		if (VSCHServerConfig.LIMIT_SPEED.get()) {
			final int maxSpeed = VSCHServerConfig.MAX_SPEED.get().intValue();
			if (Math.abs(velocity.length()) >= maxSpeed) {
				final double dotProduct = force.dot(velocity);
				if (dotProduct > 0) {
					switch (data.mode) {
						case GLOBAL -> applyScaledForce(ship, velocity, force, maxSpeed);
						case POSITION -> {
							final Vector3d pos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5)
								.sub(transform.getPositionInShip())
								.mul(scaling);
							final Vector3d parallel = pos.mul(force.dot(pos) / force.lengthSquared(), new Vector3d());
							final Vector3d perpendicular = force.sub(parallel, new Vector3d());

							// rotate the ship
							ship.applyWorldForceToBodyPos(perpendicular, pos);
							// apply global force, since the force is perfectly lined up with the centre of gravity
							applyScaledForce(ship, velocity, parallel, maxSpeed);
						}
					}
					return;
				}
			}
		}

		switch (data.mode) {
			case GLOBAL -> ship.applyWorldForceToBodyPos(force, ZERO_VEC3D);
			case POSITION -> {
				final Vector3dc pos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5)
					.sub(transform.getPositionInShip())
					.mul(scaling);
				ship.applyWorldForceToBodyPos(force, pos);
			}
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
