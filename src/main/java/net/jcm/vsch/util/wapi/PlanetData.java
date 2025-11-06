package net.jcm.vsch.util.wapi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PlanetData {
	private final LevelData levelData;
	private Vec3 position;
	private Vec3 positionO;
	private Quaterniondc rotation;
	private Quaterniondc rotationO;
	private double size;

	public PlanetData(final LevelData levelData, final Vec3 position, final Quaterniondc rotation, final double size) {
		this.levelData = levelData;
		this.server = server;
		this.position = position;
		this.positionO = position;
		this.rotation = rotation;
		this.rotationO = rotation;
		this.size = size;
	}

	public static PlanetData create(final LevelData levelData, final MinecraftServer server, final CompoundTag collisionData) {
		final Vec3 position = new Vec3(collisionData.getDouble("x"), collisionData.getDouble("y"), collisionData.getDouble("z"));
		final Quaterniondc rotation = new Quaterniond().rotationYXZ(Math.toRadians(collisionData.getDouble("yaw")), Math.toRadians(collisionData.getDouble("pitch")), Math.toRadians(collisionData.getDouble("roll")));
		final double size = collisionData.getDouble("scale");
		return new ServerPlanetData(levelData, position, rotation, size, server);
	}

	public LevelData getLevelData() {
		return this.levelData;
	}

	public Vec3 getPosition() {
		return this.position;
	}

	public void setPosition(final Vec3 position) {
		this.position = position;
	}

	public Vec3 getPositionO(final float partialTicks) {
		if (this.positionO == this.position) {
			return this.position;
		}
		return this.positionO.lerp(this.position, partialTicks);
	}

	public Quaterniondc getRotation() {
		return this.rotation;
	}

	public void setRotation(final Quaterniondc rotation) {
		this.rotation = rotation;
	}

	public Quaterniondc getRotationO(final float partialTicks) {
		if (this.rotationO == this.rotation) {
			return this.rotation;
		}
		return this.rotationO.slerp(this.rotation, partialTicks, new Quaterniond());
	}

	public double getSize() {
		return this.size;
	}

	public void clientTick() {
		this.positionO = this.position;
		this.rotationO = this.rotation;
	}
}
