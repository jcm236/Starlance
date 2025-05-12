package net.jcm.vsch.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.util.AABBdUtilKt;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.entity.handling.VSEntityHandler;
import org.valkyrienskies.mod.common.entity.handling.VSEntityManager;
import org.valkyrienskies.mod.common.entity.handling.WorldEntityHandler;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.jcm.vsch.VSCHMod;
import net.lointain.cosmos.network.CosmosModVariables;
import net.lointain.cosmos.network.CosmosModVariables.WorldVariables;
import net.lointain.cosmos.procedures.DistanceOrderProviderProcedure;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The main class where all handy utility functions used by VSCH are stored.
 */
public class VSCHUtils {

	public static final Logger logger = LogManager.getLogger(VSCHMod.MODID);

	/**
	 * Converts a VS dimension id string of
	 * <code>'minecraft:dimension:namespace:dimension_name'</code> to a normal
	 * dimension id string of <code>'namespace:dimension_name'</code>
	 * 
	 * @param VSdimensionString The VS format dimension id string
	 * @return The converted dimension id string
	 * @author Brickyboy
	 * @see #dimToVSDim(String)
	 */
	public static String VSDimToDim(String VSdimensionString) {
		// Transform VS's 'minecraft:dimension:namespace:dimension_name' into
		// 'namespace:dimension_name'
		String[] dimSplit = VSdimensionString.split(":");
		return dimSplit[2] + ":" + dimSplit[3];
	}

	/**
	 * Converts a normal dimension id string of
	 * <code>'namespace:dimension_name'</code> to a VS dimension id string
	 * <code>'minecraft:dimension:namespace:dimension_name'</code>
	 * 
	 * @param dimensionString The normal format dimension id string
	 * @return The converted VS dimension id string
	 * @author Brickyboy
	 * @see #VSDimToDim(String)
	 */
	public static String dimToVSDim(String dimensionString) {
		return "minecraft:dimension:" + dimensionString;
	}


	/**
	 * Takes in a
	 * {@link org.valkyrienskies.core.api.ships.properties.ShipTransform
	 * ShipTransform} and its ship {@link org.joml.primitives.AABBic AABBic} (its
	 * <b>shipyard</b> {@link org.joml.primitives.AABBic AABBic}) and returns a
	 * world-based {@link org.joml.primitives.AABBd AABBd} using the transform <br>
	 * <br>
	 * Basically the same as
	 * {@link org.valkyrienskies.core.api.ships.Ship#getWorldAABB()
	 * Ship#getWorldAABB()} but can take in a specified transform and ship AABBic
	 * 
	 * @param transform The ship transform to use
	 * @param shipAABB  The <b>shipyard</b> AABBic of the ship
	 * @author Brickyboy
	 * @return The world based AABBd
	 */
	public static AABBd transformToAABBd(ShipTransform transform, AABBic shipAABB) {
		if (shipAABB == null) {
			logger.warn("[Starlance] Ship AABB was null, returning empty AABBd, this may break things");
			return new AABBd();
		}
		// From AABBic (Int, constant) to AABBd (Double)
		AABBd shipAABBd = AABBdUtilKt.toAABBd(shipAABB, new AABBd());
		// Turn the shipyard AABBd to the world AABBd using the transform

		return shipAABBd.transform(transform.getShipToWorld());
	}

	/**
	 * Converts a VS dimension id string and returns a
	 * {@link net.minecraft.server.level.ServerLevel ServerLevel} in that dimension.
	 * 
	 * @param server            The {@link net.minecraft.server.MinecraftServer
	 *                          MinecraftServer} object used to get the level
	 * @param VSdimensionString The VS dimension id string for the dimension level
	 *                          you want
	 * @return A {@link net.minecraft.server.level.ServerLevel ServerLevel} object
	 *         in the VS dimension given
	 * @see #dimToVSDim(String)
	 * TODO Add error handling for if string isn't an existing dimension
	 */
	public static ServerLevel VSDimToLevel(MinecraftServer server, String VSdimensionString) {
		// Split 'minecraft:dimension:namespace:dimension_name' into [minecraft,
		// dimension, namespace, dimension_name]
		String[] splitText = VSdimensionString.split(":");

		// Make a resource location from 'namespace' and 'dimension_name'
		ResourceLocation rl = new ResourceLocation(splitText[splitText.length - 2], splitText[splitText.length - 1]);

		// Turn that RL into a Registry Key and use that to get a Level
		return server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
	}

	public static ServerLevel dimToLevel(MinecraftServer server, String dimensionString) {
		return server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionString)));
	}

	/**
	 * NOT USED ANYMORE, ALWAYS RETURNS TRUE. To be removed at a later date
  *
	 * Performs multiple checks on an entity to see if it can/should be moved
	 * through dimensions with a VS ship. Checks if the entity:
	 * <ul>
	 * <li>Has a
	 * {@link org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider
	 * IEntityDraggingInformationProvider}</li>
	 * <li>Is not riding another entity</li>
	 * <li>Is being dragged by a ship (that still exists)</li>
	 * <li>Can change dimension. Not sure what this does but its a vanilla function:
	 * Entity.canChangeDimensions()</li>
	 * </ul>
	 * 
	 * @param entity The entity to check
	 * @return True if the entity passed all checks, otherwise False
	 */
	@Deprecated
	public static boolean CanEntityBeTaken(Entity entity) {
		return true;
	}

	/**
	 * See
	 * {@link #DimensionTeleportShip(Ship, ServerLevel, String, double, double, double)
	 * DimensionTeleportShip} for documentation. This overload simply takes in a
	 * Vec3 instead of 3 doubles.
	 */
	@Deprecated
	public static void DimensionTeleportShip(Ship ship, ServerLevel level, String newDim, Vec3 newPos) {
		DimensionTeleportShip(ship, level, newDim, newPos.x, newPos.y, newPos.z);
	}

	/**
	 * This function took us like 3 days to make. You better appreciate it. <br>
	 * <br>
	 * It will teleport the given ship, using the level, to the
	 * dimension with id of newDim at x, y, z. <br>
	 * <br>
	 * But most importantly, it will also teleport any players or entities that are
	 * currently being dragged by the ship to the new dimension, and their correct
	 * position relative to the ship that was moved.
	 * 
	 * @param ship   The ship to move
	 * @param level  The ships current level
	 * @param newDim Normal dimension id string format (not VS format)
	 * @param x x position in world to tp to
	 * @param y y position in world to tp to
	 * @param z z position in world to tp to
	 * @deprecated Use {@link net.jcm.vsch.util.TeleportationHandler} instead
	 */
	@Deprecated
	public static void DimensionTeleportShip(Ship ship, ServerLevel level, String newDim, double x, double y, double z) {
		logger.fatal("Deprecated function VSCHUtils.DimensionTeleportShip used. Please use TeleportUtils.DimensionTeleportShip instead.");
	}

	/**
	 * Gets the nearest (if available) planet to the position in the dimensionId.
	 * 
	 * @param world       A LevelAccessor for getting Cosmos world variables
	 * @param position    The position to get the nearest planet from
	 * @param dimensionId The (normal format) dimension id to get planets from
	 * @return A CompoundTag of the nearest planets data, or null if it couldn't be found
	 */
	@Nullable
	public static CompoundTag getNearestPlanet(LevelAccessor world, Vec3 position, String dimensionId) {
		WorldVariables worldVars = CosmosModVariables.WorldVariables.get(world);

		// No data at all, skip it
		if (!worldVars.collision_data_map.contains(dimensionId)) {
			return null;
		}

		Tag collision_data_map = worldVars.collision_data_map.get(dimensionId);

		ListTag listtag = new ListTag();
		if (collision_data_map instanceof ListTag _listTag) {
			listtag = _listTag.copy();
		}

		// No collidable planets, skip it
		if (listtag.isEmpty()) {
			return null;
		}

		List<Object> Target_List = DistanceOrderProviderProcedure.execute(worldVars.global_collision_position_map, 1, dimensionId, position);

		Object firstTargetIndex = Target_List.get(0);

		// Not sure why all this double stuff, but I'll leave it for now
		double firstTargetIndexD = 0.0;
		if (firstTargetIndex instanceof Number _doubleValue) {
			firstTargetIndexD = _doubleValue.doubleValue();
		}

		Tag targetObject = listtag.get((int) (firstTargetIndexD));

		try {
			CompoundTag compTag = TagParser.parseTag(targetObject.getAsString());
			return compTag;
		} catch (CommandSyntaxException e) {
			logger.error("[VSCH]: Failed to parse nearest planet tag");
			return null;
		}
	}

	/**
	 * Determines if a Vec3 position is colliding with / inside a planet. If the
	 * needed data from planetData is missing, that data will default to 0.0
	 * 
	 * @param planetData A CompoundTag (nbt) of the planets data.
	 * @param position   The position to check
	 * @return A boolean, true if the position is inside the planet, false otherwise.
	 * @author DEA__TH, Brickyboy
	 * @see #getNearestPlanet(LevelAccessor, Vec3, String)
	 */
	public static boolean isCollidingWithPlanet(@Nonnull CompoundTag planetData, Vec3 position) {
		// getDouble returns 0.0D if not found, which is fine
		double yaw = planetData.getDouble("yaw");
		double pitch = planetData.getDouble("pitch");
		double roll = planetData.getDouble("roll");
		double scale = planetData.getDouble("scale");

		Vec3 cubepos = new Vec3(planetData.getDouble("x"), planetData.getDouble("y"), planetData.getDouble("z"));
		Vec3 distanceToPos = (position.subtract(cubepos));

		// I do NOT understand this, so I'm not gonna bother trying to change it...
		// looks fine enough
		Vec3 rotatedXAxis = ((new Vec3(1, 0, 0)).zRot(-Mth.DEG_TO_RAD * (float) (-roll))).yRot(Mth.DEG_TO_RAD * (float) (-yaw));
		Vec3 rotatedYAxis = ((new Vec3(0, 1, 0)).zRot(-Mth.DEG_TO_RAD * (float) (-roll))).xRot(-Mth.DEG_TO_RAD * (float) pitch);
		Vec3 rotatedZAxis = ((new Vec3(0, 0, 1)).xRot(-Mth.DEG_TO_RAD * (float) pitch)).yRot(Mth.DEG_TO_RAD * (float) (-yaw));

		double distanceSqrX = (rotatedXAxis.scale((distanceToPos.dot(rotatedXAxis)))).lengthSqr();
		double distanceSqrY = (rotatedYAxis.scale((distanceToPos.dot(rotatedYAxis)))).lengthSqr();
		double distanceSqrZ = (rotatedZAxis.scale((distanceToPos.dot(rotatedZAxis)))).lengthSqr();
		double range = (scale * scale) / 4;
		return (distanceSqrX <= range && distanceSqrY <= range && distanceSqrZ <= range);
	}

	/**
	 * DEPRECATED, will crash on cosmic horizons 0.7.2+
	 *
	 * Gets all landing locations available from a planet and gives them to the entry_world global variable.
	 * @param world The LevelAccessor to get the cosmos world variables from
	 * @param target_planet The CompoundTag of the planet you are entering
	 */
//	@Deprecated(forRemoval = true)
//	public static void setEntryLocations(LevelAccessor world, CompoundTag target_planet) {
//		WorldVariables worldVars = CosmosModVariables.WorldVariables.get(world);
//
//		Tag travel_to = target_planet.get("travel_to");
//
//		String travel_to_str = "";
//		if (travel_to instanceof StringTag _stringTag) {
//			travel_to_str = _stringTag.getAsString();
//		}
//
//		Tag locations = (worldVars.antena_locations.get(travel_to_str));
//		if (locations instanceof ListTag _listTag) {
//			worldVars.entry_world =  _listTag;
//			worldVars.syncData(world);
//		} else {
//			logger.error("[VSCH:VSCHUtils:396] Locations were not ListTags, travel_to was possibly empty");
//			return;
//		}
//	}

	/**
	 * Gets a players Cosmos variables capability, or if it doesn't exist, creates a new one.
	 * @param player The player to get the capability of
	 * TODO Investigate: Is it giving the newly made variables cap to the player, or just returning a blank one to us
	 */
	public static CosmosModVariables.PlayerVariables getOrSetPlayerCap(Player player) {
		return player.getCapability(CosmosModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new CosmosModVariables.PlayerVariables());
	}


	/**
	 * Clamps all axis of a Vector3d between -limit and +limit (not abs).
	 * @param force the vector to clamp
	 * @param limit the limit to clamp all axis to
	 * @return the clamped vector
	 */
	public static Vector3d clampVector(Vector3d force, double limit) {
		// Clamp each component of the force vector within the range -limit, +limit
		double clampedX = Math.max(-limit, Math.min(limit, force.x));
		double clampedY = Math.max(-limit, Math.min(limit, force.y));
		double clampedZ = Math.max(-limit, Math.min(limit, force.z));

		// Return a new Vector3d with the clamped values
		return new Vector3d(clampedX, clampedY, clampedZ);
	}

	/**
	 * Vector3d returns the highest axis of a Vector3d
	 * @param vec
	 * @return the value of the highest axis
	 */
	public static double getMax(Vector3d vec) {
		int maxIndex = vec.maxComponent();
		if (maxIndex == 0) {
			return vec.x;
		} else if (maxIndex == 1) {
			return vec.y;
		} else {
			return vec.z;
		}
	}

	/**
  * Using the ServerLevel, returns the nearest entity of <code>entityType</code> from the <code>sourceEntity</code> in the <code>maxDistance</code>. 
  * If no entities are found, returns null.  
  * TODO change this to use a <code>Vec3</code> instead of <code>sourceEntity</code>
  */
	public static Entity getNearestEntityOfType(ServerLevel level, EntityType<?> entityType, Entity sourceEntity, double maxDistance) {
		// Define the search bounding box
		AABB searchBox = sourceEntity.getBoundingBox().inflate(maxDistance);

		List<Entity> entities = level.getEntities(null, searchBox);

		Entity nearestEntity = null;

		// Uhhh this doesn't seem good, but it works I guess?
		double nearestDistance = 100000000000000.0d;

		for (Entity entity : entities) {
			if (entity.getType() == entityType) {
				double distance = entity.distanceToSqr(sourceEntity);
				if (distance < nearestDistance) {
					nearestEntity = entity;
					nearestDistance = distance;
				}
			}
		}

		return nearestEntity;

	}

	public static List<ServerShip> getShipsInLevel(ServerLevel level) {
		final String dimId = VSGameUtilsKt.getDimensionId(level);
		final List<ServerShip> ships = new ArrayList<>();
		for (final ServerShip ship : VSGameUtilsKt.getShipObjectWorld(level).getAllShips()) {
			if (dimId.equals(ship.getChunkClaimDimension())) {
				final String slug = ship.getSlug();
				if (slug == null || !slug.startsWith("+idle+")) {
					ships.add(ship);
				}
			}
		}
		return ships;
	}

	public static List<LoadedServerShip> getLoadedShipsInLevel(ServerLevel level) {
		final String dimId = VSGameUtilsKt.getDimensionId(level);
		final List<LoadedServerShip> loadedships = new ArrayList<>();
		for (final LoadedServerShip ship : VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips()) {
			if (dimId.equals(ship.getChunkClaimDimension())) {
				final String slug = ship.getSlug();
				if (slug == null || !slug.startsWith("+idle+")) {
					loadedships.add(ship);
				}
			}
		}
		return loadedships;
	}
}
