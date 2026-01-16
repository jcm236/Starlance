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
package net.jcm.vsch.util.wapi;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.accessor.ILevelAccessor;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Vector3d;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LevelData {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);
	private static final double SQRT1_2 = Math.sqrt(1.0 / 2);

	private final ResourceKey<Level> dimension;
	private final String type;
	private double gravity = 1;
	private double friction = 0.98;

	private ResourceKey<Level> upperDimension = null;
	private double atmosphereY = 0;

	private final Map<ResourceKey<Level>, PlanetData> lowerDimensions = new HashMap<>(0);

	private LevelData(final ResourceKey<Level> dimension, final String type) {
		this.dimension = dimension;
		this.type = type;
	}

	public static LevelData get(final Level level) {
		if (level instanceof final ILevelAccessor levelAccessor) {
			return levelAccessor.starlance$getLevelData();
		}
		return get0(level);
	}

	public static LevelData get0(final Level level) {
		return get1(CosmosModVariables.WorldVariables.get(level), level.dimension(), new HashMap<>());
	}

	@SuppressWarnings("removal")
	private static LevelData get1(
		final CosmosModVariables.WorldVariables worldVars,
		final ResourceKey<Level> dimension,
		final Map<ResourceKey<Level>, LevelData> parsing
	) {
		final LevelData parsingData = parsing.get(dimension);
		if (parsingData != null) {
			return parsingData;
		}

		final String dimensionStr = dimension.location().toString();

		final LevelData data = new LevelData(dimension, worldVars.dimension_type.getString(dimensionStr));
		parsing.put(dimension, data);

		if (worldVars.gravity_data.contains(dimensionStr)) {
			data.gravity = worldVars.gravity_data.getDouble(dimensionStr);
		}
		if (worldVars.friction_data.contains(dimensionStr)) {
			data.friction = worldVars.friction_data.getDouble(dimensionStr);
		}

		final CompoundTag atmoData = worldVars.atmospheric_collision_data_map.getCompound(dimensionStr);
		if (atmoData.contains("travel_to")) {
			data.upperDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(atmoData.getString("travel_to")));
			data.atmosphereY = atmoData.getDouble("atmosphere_y");
		}

		final ListTag collisionDatas = worldVars.collision_data_map.getList(dimensionStr, Tag.TAG_COMPOUND);
		for (int i = 0; i < collisionDatas.size(); i++) {
			final CompoundTag collisionData = collisionDatas.getCompound(i);
			final String planetDimension = collisionData.getString("travel_to");
			if (planetDimension.isEmpty()) {
				LOGGER.error("[starlance]: Planet {} in {} has no travel_to dimension. Please report this! Planet data: {}", collisionData.getString("object_name"), dimensionStr, collisionData);
				continue;
			}
			final ResourceKey<Level> planetDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(planetDimension));
			if (data.lowerDimensions.containsKey(planetDim)) {
				LOGGER.error("[starlance]: Dimension {} has two planets! Only one is allowed.", planetDim.location());
				continue;
			}
			final PlanetData planet = PlanetData.create(get1(worldVars, planetDim, parsing), (id, updater) -> {
				// TODO: use an efficent way to sync changed data to client, and save the changed data on server side.
				// Currently unused.
			}, collisionData);
			data.lowerDimensions.put(planetDim, planet);
		}

		return data;
	}

	public ResourceKey<Level> getDimension() {
		return this.dimension;
	}

	public String getType() {
		return this.type;
	}

	public boolean isSpace() {
		return LevelType.SPACE.equals(this.type);
	}

	public boolean isPlanet() {
		return LevelType.PLANET.equals(this.type);
	}

	public double getGravity() {
		return this.gravity;
	}

	public double getFriction() {
		return this.friction;
	}

	public double getAtmosphereY() {
		return this.atmosphereY;
	}

	public ResourceKey<Level> getUpperDimension() {
		return this.upperDimension;
	}

	public boolean hasPlanets() {
		return !this.lowerDimensions.isEmpty();
	}

	public Collection<PlanetData> getPlanets() {
		return this.lowerDimensions.values();
	}

	public PlanetData getPlanet(final ResourceKey<Level> level) {
		return this.lowerDimensions.get(level);
	}

	public ClosestPlanetData getNearestPlanet(final Vec3 pos) {
		ClosestPlanetData nearestPlanet = null;
		for (final PlanetData planet : this.lowerDimensions.values()) {
			final Vec3 planetPos = planet.getPosition();
			final Vector3d relDir = new Vector3d(pos.x - planetPos.x, pos.y - planetPos.y, pos.z - planetPos.z);
			planet.getRotation().transformInverse(relDir);

			final double dx = Math.abs(relDir.x);
			final double dy = Math.abs(relDir.y);
			final double dz = Math.abs(relDir.z);
			double farthestDist = dy;
			Direction farthestAxis = relDir.y < 0 ? Direction.DOWN : Direction.UP;
			if (dx > Math.abs(farthestDist)) {
				farthestDist = dx;
				farthestAxis = relDir.x < 0 ? Direction.WEST : Direction.EAST;
			}
			if (dz > Math.abs(farthestDist)) {
				farthestDist = dz;
				farthestAxis = relDir.z < 0 ? Direction.NORTH : Direction.SOUTH;
			}
			if (nearestPlanet == null || farthestDist < nearestPlanet.distance()) {
				nearestPlanet = new ClosestPlanetData(planet, Math.max(farthestDist - planet.getSize() / 2, 0), farthestAxis);
			}
		}
		return nearestPlanet;
	}

	public record ClosestPlanetData(PlanetData planet, double distance, Direction direction) {}
}
