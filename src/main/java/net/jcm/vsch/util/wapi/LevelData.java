package net.jcm.vsch.util.wapi;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LevelData {
	private static final Map<ResourceKey<Level>, LevelData> CLIENT_DATAS = new HashMap<>();

	private final ResourceKey<Level> dimension;
	private final String type;
	private double gravity = 1;
	private double friction = 0.98;

	private ResourceKey<Level> upperDimension = null;
	private double atmosphereY = Integer.MAX_VALUE;

	protected final Map<ResourceKey<Level>, PlanetData> lowerDimensions = new HashMap<>(0);

	public LevelData(final ResourceKey<Level> dimension, final String type) {
		this.dimension = dimension;
		this.type = type;
	}

	public static LevelData getClientData(final ResourceKey<Level> dimension) {
		return CLIENT_DATAS.get(dimension);
	}

	/**
	 * module-private
	 */
	public static void clearClientDatas() {
		CLIENT_DATAS.clear();
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

	public void setGravity(final double gravity) {
		this.gravity = gravity;
	}

	public double getFriction() {
		return this.friction;
	}

	public void setFriction(final double friction) {
		this.friction = friction;
	}

	public double getAtmosphereY() {
		return this.atmosphereY;
	}

	public void setAtmosphereY(final double atmosphereY) {
		this.atmosphereY = atmosphereY;
	}

	public ResourceKey<Level> getUpperDimension() {
		return this.upperDimension;
	}

	protected void setUpperDimension(final ResourceKey<Level> level) {
		this.upperDimension = level;
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

	public void writeTo(final FriendlyByteBuf buf) {
		buf.writeResourceKey(this.dimension);
		buf.writeString(this.type);
		buf.writeDouble(this.gravity);
		buf.writeDouble(this.friction);
		final boolean hasUpperDim = this.upperDimension != null;
		buf.writeBoolean(hasUpperDim);
		if (hasUpperDim) {
			buf.writeResourceKey(this.upperDimension);
		}
		buf.writeDouble(this.atmosphereY);
		buf.writeVarInt(this.lowerDimensions.size());
	}

	public record ClosestPlanetData(PlanetData planet, double distance, Direction direction) {}
}
