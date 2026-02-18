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
package net.jcm.vsch.api.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;

public class PreShipTravelEvent extends Event {
	private final ServerShip ship;
	private final ResourceKey<Level> oldLevel;
	private final ResourceKey<Level> newLevel;
	private final Vector3dc position;
	private final Quaterniondc rotation;
	private final Vector3d velocity;
	private final Vector3d omega;

	public PreShipTravelEvent(
		final ServerShip ship,
		final ResourceKey<Level> oldLevel,
		final ResourceKey<Level> newLevel,
		final Vector3dc position,
		final Quaterniondc rotation,
		final Vector3d velocity,
		final Vector3d omega
	) {
		this.ship = ship;
		this.oldLevel = oldLevel;
		this.newLevel = newLevel;
		this.position = position;
		this.rotation = rotation;
		this.velocity = velocity;
		this.omega = omega;
	}

	public final ServerShip getShip() {
		return this.ship;
	}

	public final ResourceKey<Level> getOldLevel() {
		return this.oldLevel;
	}

	public final ResourceKey<Level> getNewLevel() {
		return this.newLevel;
	}

	public final Vector3dc getPosition() {
		return this.position;
	}

	public final Quaterniondc getRotation() {
		return this.rotation;
	}

	public final Vector3dc getVelocity() {
		return this.velocity;
	}

	public final void setVelocity(final Vector3dc velocity) {
		this.velocity.set(velocity);
	}

	public final Vector3dc getOmega() {
		return this.omega;
	}

	public final void setOmega(final Vector3dc omega) {
		this.omega.set(omega);
	}

	/**
	 * This event will only fire when ship is moving from planet to space
	 */
	public static class PlanetToSpace extends PreShipTravelEvent {
		public PlanetToSpace(
			final ServerShip ship,
			final ResourceKey<Level> oldLevel,
			final ResourceKey<Level> newLevel,
			final Vector3dc position,
			final Quaterniondc rotation,
			final Vector3d velocity,
			final Vector3d omega
		) {
			super(ship, oldLevel, newLevel, position, rotation, velocity, omega);
		}

		public final ResourceKey<Level> getPlanet() {
			return this.getOldLevel();
		}

		public final ResourceKey<Level> getSpace() {
			return this.getNewLevel();
		}
	}

	/**
	 * This event will only fire when ship is moving from space to planet
	 */
	public static class SpaceToPlanet extends PreShipTravelEvent {
		public SpaceToPlanet(
			final ServerShip ship,
			final ResourceKey<Level> oldLevel,
			final ResourceKey<Level> newLevel,
			final Vector3dc position,
			final Quaterniondc rotation,
			final Vector3d velocity,
			final Vector3d omega
		) {
			super(ship, oldLevel, newLevel, position, rotation, velocity, omega);
		}

		public final ResourceKey<Level> getPlanet() {
			return this.getNewLevel();
		}

		public final ResourceKey<Level> getSpace() {
			return this.getOldLevel();
		}
	}
}
