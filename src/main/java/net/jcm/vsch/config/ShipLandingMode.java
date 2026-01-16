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
package net.jcm.vsch.config;

public enum ShipLandingMode {
	/**
	 * Always use the player menu for ships landing on a planet.
	 * A ship without a player nearby will not land, and may freeze in place until a player arrives
	 * and lands it.
	 */
	PLAYER_MENU(true, false),
	/**
	 * When a ship is near a planet, the saved launch position will be used for landing.
	 * If no history is present, the ship will land at the origin.
	 */
	HISTORY(false, true),
	/**
	 * When a ship is near a planet with a player, {@link #PLAYER_MENU} will be used.
	 * When a ship is near a planet and there is no player nearby, {@link #HISTORY} will be used.
	 */
	AUTO_HISTORY(true, true);

	private final boolean openMenu;
	private final boolean useHistory;

	ShipLandingMode(final boolean openMenu, final boolean useHistory) {
		this.openMenu	= openMenu;
		this.useHistory = useHistory;
	}

	public boolean canOpenMenu() {
		return this.openMenu;
	}

	public boolean canUseHistory() {
		return this.useHistory;
	}
}
