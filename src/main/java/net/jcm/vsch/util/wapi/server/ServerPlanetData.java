package net.jcm.vsch.util.wapi.server;

import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.util.wapi.LevelData;
import net.jcm.vsch.util.wapi.PlanetData;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerPlanetData extends PlanetData {
	private static final String POS_TAG = "pos";
	private static final String ROT_TAG = "rot";
	private static final String SIZE_TAG = "size";

	private final MinecraftServer server;
	private final CompoundTag data;
	private boolean positionChanged = false;
	private boolean rotationChanged = false;

	public ServerPlanetData(
		final LevelData levelData,
		final Vec3 position,
		final Quaterniondc rotation,
		final double size,
		final MinecraftServer server,
		final CompoundTag data
	) {
		super(levelData, position, rotation, size);
		this.server = server;
		this.data = data;
	}

	public static ServerPlanetData create(final LevelData levelData, final MinecraftServer server, final CompoundTag collisionData, final CompoundTag data) {
		Vec3 position = Vec3.ZERO;
		final Quaterniond rotation = new Quaterniond();
		double size = 1;
		if (collisionData != null) {
			position = new Vec3(collisionData.getDouble("x"), collisionData.getDouble("y"), collisionData.getDouble("z"));
			rotation.rotationYXZ(Math.toRadians(collisionData.getDouble("yaw")), Math.toRadians(collisionData.getDouble("pitch")), Math.toRadians(collisionData.getDouble("roll")));
			size = collisionData.getDouble("scale");
		}
		if (data.contains(POS_TAG)) {
			final ListTag pos = data.getList(POS_TAG);
			position = new Vec3(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
		}
		if (data.contains(ROT_TAG)) {
			final ListTag rot = data.getList(ROT_TAG);
			rotation.set(rot.getDouble(0), rot.getDouble(1), rot.getDouble(2), rot.getDouble(3));
		}
		if (data.contains(SIZE_TAG)) {
			size = data.getDouble(SIZE_TAG);
		}
		return new ServerPlanetData(levelData, position, rotation, size, server, data);
	}

	public MinecraftServer getServer() {
		return this.server;
	}

	@Override
	public void setPosition(final Vec3 position) {
		super.setPosition(position);
		this.positionChanged = true;
	}

	@Override
	public void setRotation(final Quaterniondc rotation) {
		super.setRotation(rotation);
		this.rotationChanged = true;
	}

	public void readFromTag(final CompoundTag data) {
	}

	private void syncData() {
		final Vec3 position = this.getPosition();
		final Quaterniondc rotation = this.getRotation();
		final boolean positionChanged = this.positionChanged, rotationChanged = this.rotationChanged;
		if (positionChanged) {
			this.positionChanged = false;
			final ListTag pos = new ListTag();
			pos.addDouble(position.x);
			pos.addDouble(position.y);
			pos.addDouble(position.z);
			this.data.put(POS_TAG, pos);
		}
		if (rotationChanged) {
			this.rotationChanged = false;
			final ListTag rot = new ListTag();
			rot.addDouble(rotation.x);
			rot.addDouble(rotation.y);
			rot.addDouble(rotation.z);
			rot.addDouble(rotation.w);
			this.data.put(ROT_TAG, rot);
		}
		final PlanetDataUpdateS2C packet;
		if (positionChanged && rotationChanged) {
			packet = new PlanetDataUpdateS2C.PosRot(this.getLevelData().getDimension(), position, rotation);
		} else if (positionChanged) {
			packet = new PlanetDataUpdateS2C.Pos(this.getLevelData().getDimension(), position);
		} else if (rotationChanged) {
			packet = new PlanetDataUpdateS2C.Rot(this.getLevelData().getDimension(), rotation);
		} else {
			return;
		}
		this.getLevelData().setDirty();
		for (final ResourceKey<Level> dimension : this.getWatchingDimensions()) {
			VSCHNetwork.sendToPlayersIn(packet, dimension);
		}
	}

	public static void onLevelTick(final ServerLevel level) {
		for (final PlanetData planet : LevelData.get(level).getPlanets()) {
			planet.syncData();
		}
	}

	public List<ResourceKey<Level>> getWatchingDimensions() {
		final ResourceKey<Level> upperDimension = this.getLevelData().getUpperDimension();
		final ServerLevel upperLevel = upperDimension == null ? null : this.server.getLevel(upperDimension);
		if (upperLevel == null) {
			final ServerLevel currentLevel = this.server.getLevel(this.getLevelData().getDimension());
			if (currentLevel == null) {
				return List.of();
			}
			return List.of(currentLevel);
		}
		final List<PlanetData> planets = LevelData.get(upperLevel).getPlanets();
		final List<ServerPlayer> dims = new ArrayList<>(planets.size() + 1);
		dims.add(upperDimension);
		for (final PlanetData planet : planets) {
			dims.add(planet.getLevelData().getDimension());
		}
		return dims;
	}

	public List<ServerPlayer> getWatchers() {
		return this.getWatchingDimensions().stream()
			.map(this.server::getLevel)
			.filter(Objects::nonNull)
			.flatMap((level) -> level.getPlayers(ServerPlanetData::notFakePlayer).stream())
			.toList();
	}

	private static final boolean notFakePlayer(final ServerLevel player) {
		return player.getClass() == ServerLevel.class;
	}
}
