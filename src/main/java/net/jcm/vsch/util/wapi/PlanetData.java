package net.jcm.vsch.util.wapi;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaterniondc;

public class PlanetData {
	private final LevelData levelData;
	private Vec3 position;
	private double size;
	private Quaterniondc rotation;

	private PlanetData(final LevelData levelData) {
		this.levelData = levelData;
	}

	public static PlanetData create(final LevelData levelData, final CompoundTag collisionData) {
		final PlanetData data = new PlanetData(levelData);
		data.position = new Vec3(collisionData.getDouble("x"), collisionData.getDouble("y"), collisionData.getDouble("z"));
		data.size = collisionData.getDouble("size");
		data.rotation = new Quaterniond().rotationYXZ(Math.toRadians(collisionData.getDouble("yaw")), Math.toRadians(collisionData.getDouble("pitch")), Math.toRadians(collisionData.getDouble("roll")));
		return data;
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

	public double getSize() {
		return this.size;
	}

	public Quaterniondc getRotation() {
		return this.rotation;
	}

	public void setRotation(final Quaterniondc rotation) {
		this.rotation = rotation;
	}
}
