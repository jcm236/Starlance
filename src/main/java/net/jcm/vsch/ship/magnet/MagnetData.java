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

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.PhysShip;

public class MagnetData {
	public static final ForceCalculator EMPTY_FORCE = (s, a, b) -> {};
	public volatile Vector3f facing;
	public volatile boolean isGenerator;
	public volatile ForceCalculator forceCalculator = EMPTY_FORCE;

	public MagnetData(Vector3f facing, boolean isGenerator) {
		this.facing = facing;
		this.isGenerator = isGenerator;
	}

	@FunctionalInterface
	public interface ForceCalculator {
		void calc(PhysShip physShip, Vector3d force1, Vector3d force2);
	}
}
