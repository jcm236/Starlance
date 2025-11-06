package net.jcm.vsch.util.wapi.server;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.util.wapi.LevelData;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServerLevelData extends LevelData {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	private final MinecraftServer server;
	private final LevelDataStorage storage;
	private final Set<ResourceKey<Level>> originalPlanets = new HashSet<>();

	private final CompoundTag planetsTag;

	private ServerLevelData(final ResourceKey<Level> dimension, final String type, final MinecraftServer server, final LevelDataStorage storage) {
		super(dimension, type);
		this.server = server;
		this.storage = storage;
		this.planetsTag = this.storage.data.getCompound("planets");
		if (!this.storage.data.contains("planets")) {
			this.storage.data.put("planets", this.planetsTag);
			this.storage.setDirty();
		}
	}

	public static ServerLevelData get(final ServerLevel level) {
		return get1(CosmosModVariables.WorldVariables.get(level), level);
	}

	@SuppressWarnings("removal")
	private static ServerLevelData get1(
		final CosmosModVariables.WorldVariables worldVars,
		final ServerLevel level,
		final Map<ResourceKey<Level>, ServerLevelData> parsing
	) {
		final ResourceKey<Level> dimension = level.dimension();
		final ServerLevelData parsingData = parsing.get(dimension);
		if (parsingData != null) {
			return parsingData;
		}

		final String dimensionStr = dimension.location().toString();
		final String dimType = worldVars.dimension_type.getString(dimensionStr);
		final ServerLevelData data = new ServerLevelData(dimension, dimType, level.getServer(), LevelDataStorage.get(level));
		parsing.put(dimension, data);

		data.load(worldVars, parsing);

		return data;
	}

	@Override
	public void setGravity(final double gravity) {
		super.setGravity(gravity);
		this.storage.data.putDouble("gravity", gravity);
		this.storage.setDirty();
	}

	@Override
	public void setFriction(final double friction) {
		super.setFriction(friction);
		this.storage.data.putDouble("friction", friction);
		this.storage.setDirty();
	}

	@Override
	public void setAtmosphereY(final double atmosphereY) {
		super.setAtmosphereY(atmosphereY);
		this.storage.data.putDouble("atmosphereY", atmosphereY);
		this.storage.setDirty();
	}

	@Override
	protected void setUpperDimension(final ResourceKey<Level> level) {
		super.setUpperDimension(level);
		this.storage.data.putString("upperDimension", level == null ? "" : level.location().toString());
		this.storage.setDirty();
	}

	public Collection<ServerPlanetData> getPlanets() {
		return (Collection<ServerPlanetData>) (super.getPlanets());
	}

	@Override
	public ServerPlanetData getPlanet(final ResourceKey<Level> level) {
		return (ServerPlanetData) (super.getPlanet(level));
	}

	public ServerPlanetData createPlanet(final ServerLevelData levelData) {
		if (this.lowerDimensions.containsKey(levelData.getDimension())) {
			return null;
		}
		final String dimension = levelData.getDimension().location().toString();
		final CompoundTag planetTag = new CompoundTag();
		this.planetsTag.put(dimension, planetTag);
		final ServerPlanetData planet = ServerPlanetData.create(levelData, this.server, null, planetTag);
		this.lowerDimensions.put(dimension, planet);
		levelData.setUpperDimension(this.getDimension());
		this.storage.setDirty();
		return planet;
	}

	public boolean removePlanet(final ResourceKey<Level> level) {
		final String dimension = level.location().toString();
		this.planetsTag.remove(dimension);
		this.storage.setDirty();
		final PlanetData planet = this.lowerDimensions.remove(level);
		if (planet == null) {
			return false;
		}
		planet.getLevelData().setUpperDimension(null);
		if (this.originalPlanets.contains(level)) {
			final ListTag removedPlanets = this.storage.data.getList("removedPlanets");
			if (removedPlanets.stream().map(Tag::getAsString).allMatch((s) -> !s.equals(dimension))) {
				removedPlanets.add(StringTag.valueOf(dimension));
			}
			this.storage.data.put("removedPlanets", removedPlanets);
		}
		return true;
	}

	private void load(final CosmosModVariables.WorldVariables worldVars, final Map<ResourceKey<Level>, LevelData> parsing) {
		if (worldVars.gravity_data.contains(dimensionStr)) {
			this.gravity = worldVars.gravity_data.getDouble(dimensionStr);
		}
		if (worldVars.friction_data.contains(dimensionStr)) {
			this.friction = worldVars.friction_data.getDouble(dimensionStr);
		}

		final CompoundTag atmoData = worldVars.atmospheric_collision_data_map.getCompound(dimensionStr);
		if (atmoData.contains("travel_to")) {
			this.upperDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(atmoData.getString("travel_to")));
			this.atmosphereY = atmoData.getDouble("atmosphere_y");
		}

		if (this.storage.data.contains("gravity")) {
			this.gravity = this.storage.data.getDouble("gravity");
		}
		if (this.storage.data.contains("friction")) {
			this.friction = this.storage.data.getDouble("friction");
		}
		if (this.storage.data.contains("upperDimension")) {
			final String upperDimension = this.storage.data.getString("upperDimension");
			this.upperDimension = upperDimension.isEmpty() ? null : ResourceKey.create(Registries.DIMENSION, upperDimension);
		}
		if (this.storage.data.contains("atmosphereY")) {
			this.atmosphereY = data.getDouble("atmosphereY");
		}

		final Set<String> removedPlanets = Set.copyOf(this.storage.data.getList("removedPlanets").stream().map(Tag::getAsString).toList());
		final ListTag collisionDatas = worldVars.collision_data_map.getList(dimensionStr, Tag.TAG_COMPOUND);
		for (int i = 0; i < collisionDatas.size(); i++) {
			final CompoundTag collisionData = collisionDatas.getCompound(i);
			final String planetDimension = collisionData.getString("travel_to");
			final String planetName = collisionData.getString("object_name");
			if (planetDimension.isEmpty()) {
				LOGGER.error("[starlance]: Planet {} in {} has no travel_to dimension. Please report this! Planet data: {}", planetName, dimensionStr, collisionData);
				continue;
			}
			final ResourceKey<Level> planetDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(planetDimension));
			this.originalPlanets.add(planetDim);
			if (removedPlanets.contains(planetDimension)) {
				continue;
			}
			if (this.lowerDimensions.containsKey(planetDim)) {
				LOGGER.error("[starlance]: Dimension {} has two planets! Only one is allowed.", planetDimension);
				continue;
			}
			final ServerLevel planetLevel = this.server.getLevel(planetDim);
			if (planetLevel == null) {
				LOGGER.error("[starlance]: Planet {}'s dimension {} does not exist!", planetName, planetDimension);
				continue;
			}
			final CompoundTag planetTag = this.planetsTag.getCompound(planetDimension);
			// planetTag will be a new empty instance if dimension does not exist
			if (!this.planetsTag.contains(planetDimension)) {
				this.planetsTag.put(planetDimension, planetTag);
				this.storage.setDirty();
			}
			final PlanetData planet = ServerPlanetData.create(get1(worldVars, planetLevel, parsing), server, collisionData, planetTag);
			this.lowerDimensions.put(planetDim, planet);
		}
	}

	private final class LevelDataStorage extends SavedData {
		private final CompoundTag data;

		private LevelDataStorage() {
			this(new CompoundTag());
		}

		private LevelDataStorage(final CompoundTag data) {
			this.data = data;
		}

		private static LevelDataStorage get(final ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(LevelDataStorage::new, LevelDataStorage::new, "starlance_LevelData");
		}

		@Override
		public CompoundTag save(final CompoundTag data) {
			return this.data;
		}
	}
}
