package net.jcm.vsch.util.wapi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PlanetData {
	private final LevelData levelData;
	private Vector3dc position;
	private Vector3dc positionO;
	private Quaterniondc rotation;
	private Quaterniondc rotationO;
	private double size;

	public PlanetData(final LevelData levelData, final Vector3dc position, final Quaterniondc rotation, final double size) {
		this.levelData = levelData;
		this.server = server;
		this.position = position;
		this.positionO = position;
		this.rotation = rotation;
		this.rotationO = rotation;
		this.size = size;
	}

	public LevelData getLevelData() {
		return this.levelData;
	}

	public Vector3dc getPosition() {
		return this.position;
	}

	public void setPosition(final Vector3dc position) {
		this.position = position;
	}

	public Vector3dc getPositionO(final float partialTicks) {
		if (this.positionO == this.position) {
			return this.position;
		}
		return this.positionO.lerp(this.position, partialTicks, new Vector3d());
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

	public void writeTo(final FriendlyByteBuf buf) {
		buf.writeResourceKey(this.levelData.getDimension());
		buf.
	}
}
