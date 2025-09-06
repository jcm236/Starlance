package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.event.PreTravelEvent;
import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.util.TeleportationHandler;
import net.jcm.vsch.util.VSCHUtils;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
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
		// Atmo collision JSON for overworld:
		// "minecraft:overworld":'{"atmosphere_y":560,"travel_to":"cosmos:solar_sys_d","origin_x":-24100,"origin_y":1000,"origin_z":5100,"overlay_texture_id":"earth_bar","shipbit_y":24,"ship_min_y":120}'

		final CosmosModVariables.WorldVariables worldVariables = CosmosModVariables.WorldVariables.get(level);
		final CompoundTag atmoDatas = worldVariables.atmospheric_collision_data_map;
		final CompoundTag atmoData = atmoDatas.getCompound(level.dimension().location().toString());

		// Skip current dimension has atmo data (i.e. no space dimension attached)
		if (atmoData.isEmpty()) {
			return;
		}

		final double atmoHeight = atmoData.getDouble("atmosphere_y");
		final double targetX = atmoData.getDouble("origin_x");
		final double targetY = atmoData.getDouble("origin_y");
		final double targetZ = atmoData.getDouble("origin_z");
		final String targetDim = atmoData.getString("travel_to");
		final ServerLevel targetLevel = VSCHUtils.dimToLevel(targetDim);
		if (targetLevel == null) {
			// TODO: enable warn and avoid log spam when dimension does not exist.
			// LOGGER.warn("[starlance]: dimension {} is not exists", targetDim);
			return;
		}

		final TeleportationHandler teleportHandler = new TeleportationHandler(level, targetLevel, false);

		for (final LoadedServerShip ship : VSCHUtils.getLoadedShipsInLevel(level)) {
			if (ship.isStatic() || teleportHandler.hasShip(ship)) {
				continue;
			}
			final Vector3dc shipPos = ship.getTransform().getPositionInWorld();
			final double shipY = shipPos.y();
			final ShipLandingAttachment landingAttachment = ship.getAttachment(ShipLandingAttachment.class);
			if (shipY < atmoHeight - 10) {
				if (landingAttachment != null) {
					ship.saveAttachment(ShipLandingAttachment.class, null);
				}
				continue;
			}
			if (landingAttachment != null && landingAttachment.landing && shipY < atmoHeight + 128) {
				continue;
			}

			// TODO: figure out how to detect ships in the way of us teleporting, and teleport a distance away
			// TODO: map ship loaction around the planet instead of always spawn at same location
			final Vector3d targetPos = new Vector3d(targetX, targetY, targetZ);
			final Quaterniond rotation = new Quaterniond();

			MinecraftForge.EVENT_BUS.post(new PreTravelEvent.PlanetToSpace(level.dimension(), shipPos, targetLevel.dimension(), targetPos, rotation));

			LOGGER.info("[starlance]: Handling teleport {} ({}) to {} {} {} {}", ship.getSlug(), ship.getId(), targetDim, targetPos.x, targetPos.y, targetPos.z);
			teleportHandler.addShip(ship, targetPos, rotation);
		}
		for (final LoadedServerShip ship : teleportHandler.getPendingShips()) {
			ship.saveAttachment(ShipLandingAttachment.class, new ShipLandingAttachment(true, false));
		}
		teleportHandler.finalizeTeleport();
	}
}
