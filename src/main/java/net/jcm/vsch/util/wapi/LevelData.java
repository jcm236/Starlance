package net.jcm.vsch.util.wapi;

import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class LevelData {
	private final ResourceKey<Level> dimension;
	private final String type;
	private double gravity = 1;
	private double friction = 0.98;

	private ResourceKey<Level> upperDimension = null;
	private Vec3 upperPosition = null;
	private double atmosphereY = 0;

	private final Map<ResourceKey<Level>, PlanetData> lowerDimensions = new HashMap<>();

	public LevelData(final ResourceKey<Level> dimension, final String type) {
		this.dimension = dimension;
		this.type = type;
	}

	public static LevelData get(final Level level) {
		return get0(CosmosModVariables.WorldVariables.get(level), level.dimension(), new HashMap<>());
	}

	private static LevelData get0(
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

		final CompoundTag atmoData = worldVars.atmospheric_collision_data_map.getCompound(dimensionStr);
		if (atmoData.contains("travel_to")) {
			this.upperDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(atmoData.getString("travel_to")));
			this.upperPosition = new Vec3(atmoData.getDouble("origin_x"), atmoData.getDouble("origin_y"), atmoData.getDouble("origin_z"));
			this.atmosphereY = atmoData.getDouble("atmosphere_y");
		}

		final ListTag collisionDatas = worldVars.collision_data_map.getList(dimensionStr);
		for (int i = 0; i < collisionDatas.size(); i++) {
			final CompoundTag collisionData = collisionDatas.getCompound(i);
			final String planetDimension = collisionData.getString("travel_to");
			if (planetDimension.isEmpty()) {
				LOGGER.error("[starlance]: Planet {} in {} has no travel_to dimension. Please report this! Planet data: {}", collisionData.getString("object_name"), dimensionStr, collisionData);
				continue;
			}
			final ResourceKey<Level> planetDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(planetDimension));
			final PlanetData planet = PlanetData.create(get0(worldVars, planetDim, parsing), collisionData);
			lowerDimensions.put(planetDim, planet);
		}

		return data;
	}

	public ResourceKey<Level> getDimension() {
		return this.dimension;
	}

	public String getType() {
		return this.type;
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

	public Vec3 getUpperPosition() {
		return this.upperPosition;
	}
}
