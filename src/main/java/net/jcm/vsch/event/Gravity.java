package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.util.VSCHUtils;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

/**
 * The class for gravity related functions
 */
public class Gravity {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	/**
	 * Sets the Gravity of all dimensions as defined in the datapacks.
	 * @param world A {@link net.minecraft.server.level.ServerLevel ServerLevel} used to access the world variables.
	 * @author Jcm
	 */
	public static void setAll(final ServerLevel world){
		final CompoundTag gravityData = CosmosModVariables.WorldVariables.get(world).gravity_data;
		for (final String dimId : gravityData.getAllKeys()) {
			float gravity = gravityData.getFloat(dimId);
			try {
				// VSGameUtilsKt.getShipObjectWorld((ServerLevel) world).updateDimension(VSCHUtils.dimToVSDim(dimId),new Vector3d(0,-10*gravity,0));
				LOGGER.info("[starlance]: Set gravity for dimension " + dimId + " to " + (-10 * gravity));
			} catch (Exception e) {
				LOGGER.info("[starlance]: Failed to set gravity for dimension " + dimId, e);
			}

		}
		//VSGameUtilsKt.getShipObjectWorld((ServerLevel) world).removeDimension("minecraft:dimension:cosmos:solar_system");
		//VSGameUtilsKt.getShipObjectWorld((ServerLevel) world).addDimension("minecraft:dimension:cosmos:solar_system", VSGameUtilsKt.getYRange(world.getServer().overworld()),new Vector3d(0,0,0));
	}
}