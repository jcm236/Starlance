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
package net.jcm.vsch.spacemods.cosmic.events;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.spacemods.cosmic.wapi.LevelData;

import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

/**
 * The class for gravity related functions
 */
public class Gravity {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);
	private static final Double NEG1D = Double.valueOf(-1);

	/**
	 * Update the gravity for the target dimension as defined in the datapacks.
	 *
	 * @param level The loading {@link ServerLevel ServerLevel}.
	 */
	public static void updateFor(final ServerLevel level) {
		final String dimId = level.dimension().location().toString();
		final LevelData data = LevelData.get(level);
		final double gravity = -10 * data.getGravity();
		try {
			// Note: client dimension can also be updated. However, currently nothing is used.
			VSGameUtilsKt.getShipObjectWorld(level).updateDimension(
				VSGameUtilsKt.getDimensionId(level),
				new Vector3d(0, gravity, 0),
				null,
				data.isSpace() ? NEG1D : null
			);
		} catch (Exception e) {
			LOGGER.error("[starlance]: Failed to set gravity for dimension " + dimId, e);
			return;
		}
		LOGGER.info("[starlance]: Set gravity for dimension " + dimId + " to " + gravity);
	}
}