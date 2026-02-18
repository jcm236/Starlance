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

import net.minecraft.util.StringRepresentable;

public final class ThrusterData {

	public enum ThrusterMode implements StringRepresentable  {
		POSITION("position"),
		GLOBAL("global");

		private final String name;

		// Constructor that takes a string parameter
		private ThrusterMode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}


		public ThrusterMode toggle() {
			return this == POSITION ? GLOBAL : POSITION;
		}
	}

	public final Vector3d dir;
	public volatile double throttle;
	public volatile ThrusterMode mode;

	public ThrusterData(Vector3d dir, double throttle, ThrusterMode mode) {
		this.dir = dir;
		this.throttle = throttle;
		this.mode = mode;
	}

	public String toString() {
		return "Direction: " + this.dir + " Throttle: " + this.throttle + " Mode: " + this.mode;
	}
}
