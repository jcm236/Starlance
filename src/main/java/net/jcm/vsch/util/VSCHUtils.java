package net.jcm.vsch.util;

import net.jcm.vsch.VSCHMod;
import net.lointain.cosmos.network.CosmosModVariables;
import net.lointain.cosmos.network.CosmosModVariables.WorldVariables;
import net.lointain.cosmos.procedures.DistanceOrderProviderProcedure;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.entity.handling.VSEntityHandler;
import org.valkyrienskies.mod.common.entity.handling.VSEntityManager;
import org.valkyrienskies.mod.common.entity.handling.WorldEntityHandler;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The main class where all handy utility functions used by VSCH are stored.
 */
public class VSCHUtils {

	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	/**
	 * Converts a VS dimension id string of
	 * <code>'minecraft:dimension:namespace:dimension_name'</code> to a normal
	 * dimension id string of <code>'namespace:dimension_name'</code>
	 * 
	 * @param dimension The VS format dimension id string
	 * @return The converted dimension id string
	 * @author Brickyboy
	 * @see #dimToVSDim(String)
	 */
	public static String vsDimToDim(String dimension) {
		// Transform VS's 'minecraft:dimension:namespace:dimension_name' into
		// 'namespace:dimension_name'
		final String[] parts = dimension.split(":");
		if (parts.length != 4) {
			throw new IllegalArgumentException("Unexpected dimension ID: " + dimension);
		}
		return parts[2] + ":" + parts[3];
	}

	/**
	 * Converts a normal dimension id string of
	 * <code>'namespace:dimension_name'</code> to a VS dimension id string
	 * <code>'minecraft:dimension:namespace:dimension_name'</code>
	 * 
	 * @param dimension The normal format dimension id string
	 * @return The converted VS dimension id string
	 * @author Brickyboy
	 * @see #vsDimToDim(String)
	 */
	public static String dimToVSDim(String dimension) {
		return "minecraft:dimension:" + dimension;
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
	public static AABBd transformToAABBd(final ShipTransform transform, final AABBic shipAABB) {
		// From AABBic (Int, constant) to AABBd (Double)
		AABBd shipAABBd = AABBdUtilKt.toAABBd(shipAABB, new AABBd());
		// Turn the shipyard AABBd to the world AABBd using the transform
		return shipAABBd.transform(transform.getShipToWorld());
	}

	/**
	 * Get {@link net.minecraft.server.level.ServerLevel ServerLevel} from a VS dimension ID.
	 * 
	 * @param dimension The dimension ID string in format registry_namespace:registry_name:dimension_namespace:dimension_name
	 * @return A {@link net.minecraft.server.level.ServerLevel ServerLevel} instance with the dimension ID given
	 */
	public static ServerLevel registeryDimToLevel(final String dimension) {
		// Split 'minecraft:dimension:namespace:dimension_name' into [minecraft, dimension, namespace, dimension_name]
		final String[] parts = dimension.split(":");
		if (parts.length != 4) {
			throw new IllegalArgumentException("Unexpected dimension ID: " + dimension);
		}
		final ResourceKey levelId = ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation(parts[0], parts[1])), new ResourceLocation(parts[2], parts[3]));
		return ValkyrienSkiesMod.getCurrentServer().getLevel(levelId);
	}

	public static ServerLevel dimToLevel(final String dimensionString) {
		return ValkyrienSkiesMod.getCurrentServer().getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionString)));
	}

	public static boolean isSpaceLevel(final Level level) {
		if (level.isClientSide) {
			return false;
		}
		final CosmosModVariables.WorldVariables worldVars = CosmosModVariables.WorldVariables.get(level);
		return worldVars.dimension_type.getString(level.dimension().location().toString()).equals("space");
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
	public static CompoundTag getNearestPlanet(final LevelAccessor world, final Vec3 position, final String dimensionId) {
		final WorldVariables worldVars = CosmosModVariables.WorldVariables.get(world);
		final ListTag collisionDatas = worldVars.collision_data_map.getList(dimensionId, Tag.TAG_COMPOUND);
		// No collidable planets, skip it
		if (collisionDatas.isEmpty()) {
			return null;
		}

		final List<Object> targetList = DistanceOrderProviderProcedure.execute(worldVars.global_collision_position_map, 1, dimensionId, position);
		final int firstTargetIndex = ((Number) (targetList.get(0))).intValue();
		return collisionDatas.getCompound(firstTargetIndex);
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
	public static boolean isCollidingWithPlanet(final @Nonnull CompoundTag planetData, final Vec3 position) {
		// getDouble returns 0.0D if not found, which is fine
		final float
			yaw = planetData.getFloat("yaw"),
			pitch = planetData.getFloat("pitch"),
			roll = planetData.getFloat("roll");
		final double scale = planetData.getFloat("scale");

		final Vec3 cubepos = new Vec3(planetData.getDouble("x"), planetData.getDouble("y"), planetData.getDouble("z"));
		final Vec3 distanceToPos = position.subtract(cubepos);

		// I do NOT understand this, so I'm not gonna bother trying to change it...
		// looks fine enough
		final Vec3 rotatedXAxis = new Vec3(1, 0, 0).zRot(Mth.DEG_TO_RAD * roll).yRot(-Mth.DEG_TO_RAD * yaw);
		final Vec3 rotatedYAxis = new Vec3(0, 1, 0).zRot(Mth.DEG_TO_RAD * roll).xRot(-Mth.DEG_TO_RAD * pitch);
		final Vec3 rotatedZAxis = new Vec3(0, 0, 1).xRot(-Mth.DEG_TO_RAD * pitch).yRot(-Mth.DEG_TO_RAD * yaw);

		final double range = (scale * scale) / 4;
		return
			rotatedXAxis.scale(distanceToPos.dot(rotatedXAxis)).lengthSqr() <= range &&
			rotatedYAxis.scale(distanceToPos.dot(rotatedYAxis)).lengthSqr() <= range &&
			rotatedZAxis.scale(distanceToPos.dot(rotatedZAxis)).lengthSqr() <= range;
	}

	/**
	 * Gets a players Cosmos variables capability.
	 * @param player The player to get the capability of.
	 * @return The player's capability, or null if it does not exists.
	 */
	public static CosmosModVariables.PlayerVariables getPlayerCap(Player player) {
		return player.getCapability(CosmosModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
	}

	/**
	 * Clamps all axis of a Vector3d between -limit and +limit (not abs).
	 * @param force the vector to clamp
	 * @param limit the limit to clamp all axis to
	 * @return the clamped vector
	 */
	public static Vector3d clampVector(Vector3d force, double limit) {
		// Clamp each component of the force vector within the range -limit, +limit
		final double clampedX = Math.max(-limit, Math.min(limit, force.x));
		final double clampedY = Math.max(-limit, Math.min(limit, force.y));
		final double clampedZ = Math.max(-limit, Math.min(limit, force.z));

		// Return a new Vector3d with the clamped values
		return new Vector3d(clampedX, clampedY, clampedZ);
	}

	/**
	 * Vector3d returns the highest axis of a Vector3d
	 * @param vec
	 * @return the value of the highest axis
	 */
	public static double getMax(Vector3d vec) {
		return vec.get(vec.maxComponent());
	}

	/**
	 * Using the ServerLevel, returns the nearest entity of <code>entityType</code> from the <code>sourceEntity</code> in the <code>maxDistance</code>. 
	 * If no entities are found, returns null.  
	 * TODO change this to use a <code>Vec3</code> instead of <code>sourceEntity</code>
	 */
	public static Entity getNearestEntityOfType(ServerLevel level, EntityType<?> entityType, Entity sourceEntity, double maxDistance) {
		// Define the search bounding box
		final AABB searchBox = sourceEntity.getBoundingBox().inflate(maxDistance);
		final List<Entity> entities = level.getEntities((Entity) null, searchBox, entity -> entity.getType() == entityType);

		Entity nearestEntity = null;
		double nearestDistance = Double.MAX_VALUE;

		for (Entity entity : entities) {
			double distance = entity.distanceToSqr(sourceEntity);
			if (distance < nearestDistance) {
				nearestEntity = entity;
				nearestDistance = distance;
			}
		}

		return nearestEntity;
	}

	public static List<LoadedServerShip> getLoadedShipsInLevel(ServerLevel level) {
		final String dimId = VSGameUtilsKt.getDimensionId(level);
		final List<LoadedServerShip> loadedships = new ArrayList<>();
		for (final LoadedServerShip ship : VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips()) {
			if (dimId.equals(ship.getChunkClaimDimension())) {
				loadedships.add(ship);
			}
		}
		return loadedships;
	}

	public static Component getWarningComponent() {
		return Component.translatable(VSCHMod.MODID+".tooltip.may_need_fuel_or_energy").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
	}
}
