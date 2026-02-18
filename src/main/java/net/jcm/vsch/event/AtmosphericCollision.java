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
package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.event.PreTravelEvent;
import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.util.TeleportationHandler;
import net.jcm.vsch.util.VSCHUtils;
import net.jcm.vsch.util.wapi.LevelData;
import net.jcm.vsch.util.wapi.PlanetData;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class AtmosphericCollision {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	/**
	 * Checks all VS ships for the given level, if any of them are above their
	 * dimensions atmosphere (as set in a CH datapack), they will be moved to the
	 * specified origin in the travel to dimension.
	 *
	 * @param level
	 */
	public static void atmosphericCollisionTick(final ServerLevel level) {
		final LevelData levelData = LevelData.get(level);

		final ResourceKey<Level> targetDimension = levelData.getUpperDimension();
		if (targetDimension == null) {
			return;
		}

		final ServerLevel targetLevel = level.getServer().getLevel(targetDimension);
		if (targetLevel == null) {
			return;
		}

		final ResourceKey<Level> dimension = level.dimension();
		final LevelData targetLevelData = LevelData.get(targetLevel);
		final PlanetData planet = targetLevelData.getPlanet(dimension);
		if (planet == null) {
			return;
		}
		final Vec3 planetPos = planet.getPosition();
		final double atmoHeight = levelData.getAtmosphereY();

		final TeleportationHandler teleportHandler = new TeleportationHandler(level, targetLevel, false);

		for (final LoadedServerShip ship : VSCHUtils.getLoadedShipsInLevel(level)) {
			if (ship.isStatic() || teleportHandler.hasShip(ship)) {
				continue;
			}
			final Vector3dc shipPos = ship.getTransform().getPositionInWorld();
			final double shipY = shipPos.y();
			final ShipLandingAttachment landingAttachment = ShipLandingAttachment.get(ship);
			if (shipY + 10 < atmoHeight) {
				landingAttachment.landing = false;
				continue;
			}
			if (landingAttachment.landing && shipY < atmoHeight + 128) {
				continue;
			}

			// TODO: figure out how to detect ships in the way of us teleporting, and teleport a distance away
			// TODO: map ship loaction around the planet instead of always spawn at same location
			final Vector3d targetPos = new Vector3d(0, planet.getSize() / 2 + 120, 0);
			final Quaterniond rotation = new Quaterniond(planet.getRotation());
			rotation.transform(targetPos);
			targetPos.add(planetPos.x, planetPos.y, planetPos.z);

			MinecraftForge.EVENT_BUS.post(new PreTravelEvent.PlanetToSpace(dimension, shipPos, targetDimension, targetPos, rotation));

			LOGGER.info("[starlance]: Handling teleport {} ({}) to {} {} {} {}", ship.getSlug(), ship.getId(), targetDimension.location(), targetPos.x, targetPos.y, targetPos.z);
			teleportHandler.addShip(ship, targetPos, rotation);
		}
		if (!teleportHandler.addedShip()) {
			return;
		}
		teleportHandler.afterShipsAdded().thenAcceptAsync((void_) -> {
			for (final LoadedServerShip ship : teleportHandler.getPendingShips()) {
				final ShipLandingAttachment landingAttachment = ShipLandingAttachment.get(ship);
				final Vector3dc pos = ship.getTransform().getPositionInWorld();
				landingAttachment.setLaunching(dimension, new ChunkPos(SectionPos.blockToSectionCoord(pos.x()), SectionPos.blockToSectionCoord(pos.z())));
			}
			teleportHandler.finalizeTeleport();
		}, level.getServer());
	}
}
