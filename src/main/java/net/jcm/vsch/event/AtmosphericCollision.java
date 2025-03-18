package net.jcm.vsch.event;

import net.jcm.vsch.util.TeleportationHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.nbt.CompoundTag;
import net.lointain.cosmos.network.CosmosModVariables;
import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.util.VSCHUtils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;


public class AtmosphericCollision {

	public static final Logger logger = LogManager.getLogger(VSCHMod.MODID);


	/**
	 * Checks all VS ships for the given level, if any of them are above their
	 * dimensions atmosphere (as set in a CH datapack), they will be moved to the
	 * specified origin in the travel to dimension.
	 *
	 * @param level
	 * @param world
	 */
	public static void atmosphericCollisionTick(ServerLevel level, LevelAccessor world) {

		for (Ship ship : VSCHUtils.getAllLoadedShips(level)) {

			// ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);

			// shipWorld.updateDimension(VSGameUtilsKt.getDimensionId(level),null);

			// Atmo collision JSON for overworld:
			// "minecraft:overworld":'{"atmosphere_y":560,"travel_to":"cosmos:solar_sys_d","origin_x":-24100,"origin_y":1000,"origin_z":5100,"overlay_texture_id":"earth_bar","shipbit_y":24,"ship_min_y":120}'

			CosmosModVariables.WorldVariables worldVariables = CosmosModVariables.WorldVariables.get(world);
			CompoundTag atmoDatas = worldVariables.atmospheric_collision_data_map;

			String shipDim = VSCHUtils.VSDimToDim(ship.getChunkClaimDimension());

			// If our current dimension has atmo data (e.g. a space dimension attached)
			if (atmoDatas.contains(shipDim)) {

				CompoundTag atmoData = atmoDatas.getCompound(shipDim);

				// If the ship is above the planets atmo height:
				if (ship.getTransform().getPositionInWorld().y() > atmoData.getDouble("atmosphere_y")) {

					// ----- Get destination x, y, z and dimension ----- //
					//TODO: figure out how to detect ships in the way of us teleporting, and teleport a distance away
					double posX = atmoData.getDouble("origin_x"); // + Mth.nextInt(RandomSource.create(), -10, 10)
					double posY = atmoData.getDouble("origin_y"); // + Mth.nextInt(RandomSource.create(), -5, 5)
					double posZ = atmoData.getDouble("origin_z"); // + Mth.nextInt(RandomSource.create(), -10, 10)

					String gotoDimension = atmoData.getString("travel_to");

					new TeleportationHandler(VSCHUtils.dimToLevel(ValkyrienSkiesMod.getCurrentServer(), gotoDimension), level, false).handleTeleport(ship, new Vector3d(posX, posY, posZ));

				}
			}
		}
	}


}
