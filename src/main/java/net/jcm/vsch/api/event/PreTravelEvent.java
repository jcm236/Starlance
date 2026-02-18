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

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PreTravelEvent extends Event {
	private final ResourceKey<Level> oldLevel;
	private final Vector3dc oldPos;
	private final ResourceKey<Level> newLevel;
	private final Vector3d newPos;
	private final Quaterniond rotation;

	public PreTravelEvent(
		final ResourceKey<Level> oldLevel,
		final Vector3dc oldPos,
		final ResourceKey<Level> newLevel,
		final Vector3d newPos,
		final Quaterniond rotation
	) {
		this.oldLevel = oldLevel;
		this.oldPos = oldPos;
		this.newLevel = newLevel;
		this.newPos = newPos;
		this.rotation = rotation;
	}

	/**
	 * @return original level for the ship cluster
	 */
	public final ResourceKey<Level> getOldLevel() {
		return this.oldLevel;
	}

	/**
	 * @return original position for the ship cluster
	 */
	public final Vector3dc getOldPosition() {
		return this.oldPos;
	}

	/**
	 * @return target level the ship cluster is moving to
	 */
	public final ResourceKey<Level> getNewLevel() {
		return this.newLevel;
	}

	/**
	 * Get the new position the ship cluster will move to.
	 * The returned vector may change its value after {@link setNewPosition} is invoked.
	 *
	 * @return new position the ship cluster will move to
	 */
	public final Vector3dc getNewPosition() {
		return this.newPos;
	}

	public final void setNewPosition(final Vector3dc newPos) {
		this.newPos.set(newPos);
	}

	/**
	 * Get relative rotation the ship cluster will apply when teleporting.
	 * The returned quaternion may change its value after {@link setRelativeRotation} is invoked.
	 *
	 * @return relative rotation the ship cluster will apply when teleporting
	 */
	public final Quaterniondc getRelativeRotation() {
		return this.rotation;
	}

	public final void setRelativeRotation(final Quaterniondc rotation) {
		this.rotation.set(rotation);
	}

	/**
	 * This event will only fire when ships are moving from planet to space
	 */
	public static class PlanetToSpace extends PreTravelEvent {
		public PlanetToSpace(
			final ResourceKey<Level> oldLevel,
			final Vector3dc oldPos,
			final ResourceKey<Level> newLevel,
			final Vector3d newPos,
			final Quaterniond rotation
		) {
			super(oldLevel, oldPos, newLevel, newPos, rotation);
		}

		public final ResourceKey<Level> getPlanet() {
			return this.getOldLevel();
		}

		public final ResourceKey<Level> getSpace() {
			return this.getNewLevel();
		}
	}

	/**
	 * This event will only fire when ships are moving from space to planet
	 */
	public static class SpaceToPlanet extends PreTravelEvent {
		public SpaceToPlanet(
			final ResourceKey<Level> oldLevel,
			final Vector3dc oldPos,
			final ResourceKey<Level> newLevel,
			final Vector3d newPos,
			final Quaterniond rotation
		) {
			super(oldLevel, oldPos, newLevel, newPos, rotation);
		}

		public final ResourceKey<Level> getPlanet() {
			return this.getNewLevel();
		}

		public final ResourceKey<Level> getSpace() {
			return this.getOldLevel();
		}
	}
}
