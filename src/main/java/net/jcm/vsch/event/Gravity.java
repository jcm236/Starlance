package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.core.api.util.AerodynamicUtils;

/**
 * The class for gravity related functions
 */
public class Gravity {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	/**
	 * Update the gravity for the target dimension as defined in the datapacks.
	 *
	 * @param level The loading {@link ServerLevel ServerLevel}.
	 */
	public static void updateFor(final ServerLevel level) {
		final String dimId = level.dimension().location().toString();
		final CompoundTag gravityData = CosmosModVariables.WorldVariables.get(level).gravity_data;
		final double gravity = -10 * gravityData.getDouble(dimId);
		try {
			VSGameUtilsKt.getShipObjectWorld(level).updateDimension(
				VSGameUtilsKt.getDimensionId(level),
				new Vector3d(0, gravity, 0),
				AerodynamicUtils.DEFAULT_SEA_LEVEL,
				// If 0 gravity, set atmosphere to -1
				gravity == 0 ? -1d : AerodynamicUtils.DEFAULT_MAX
			);
		} catch (Exception e) {
			LOGGER.error("[starlance]: Failed to set gravity for dimension " + dimId, e);
			return;
		}
		LOGGER.info("[starlance]: Set gravity for dimension " + dimId + " to " + gravity);
		// VSGameUtilsKt.getShipObjectWorld((ServerLevel) world).removeDimension("minecraft:dimension:cosmos:solar_system");
		// VSGameUtilsKt.getShipObjectWorld((ServerLevel) world).addDimension("minecraft:dimension:cosmos:solar_system", VSGameUtilsKt.getYRange(world.getServer().overworld()),new Vector3d(0,0,0));
	}
}