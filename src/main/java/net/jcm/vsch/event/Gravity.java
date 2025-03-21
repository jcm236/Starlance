package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.util.VSCHUtils;
import net.lointain.cosmos.network.CosmosModVariables;
import net.minecraft.nbt.FloatTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Objects;


/**
 * The class for gravity related functions
 */
public class Gravity {
	public static final Logger logger = LogManager.getLogger(VSCHMod.MODID);

	/**
	 * Sets the Gravity of all dimensions as defined in the datapacks.
	 * @param world A {@link net.minecraft.world.level.LevelAccessor LevelAccessor} used to access the world variables.
	 * @author Jcm
	 */
	public static void setAll(LevelAccessor world){
		for (String keyiterator : CosmosModVariables.WorldVariables.get(world).gravity_data.getAllKeys()) {
			float gravity = CosmosModVariables.WorldVariables.get(world).gravity_data.getFloat(keyiterator);
			try {
//				VSGameUtilsKt.getShipObjectWorld((ServerLevel) world).updateDimension(VSCHUtils.dimToVSDim(keyiterator),new Vector3d(0,-10*gravity,0));
				logger.info("[CH]: Set gravity for dimension " + keyiterator + " to " + (-10 * gravity));
			} catch (Exception e) {
				logger.info("[CH]: Failed to set gravity for dimension " + keyiterator + " due to error " + e);
			}

		}
		//VSGameUtilsKt.getShipObjectWorld((ServerLevel) world).removeDimension("minecraft:dimension:cosmos:solar_system");
		//VSGameUtilsKt.getShipObjectWorld((ServerLevel) world).addDimension("minecraft:dimension:cosmos:solar_system", VSGameUtilsKt.getYRange(world.getServer().overworld()),new Vector3d(0,0,0));
	}
}