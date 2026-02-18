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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PlanetData {
	private final LevelData levelData;
	private final BiConsumer<String, Consumer<CompoundTag>> dataUpdater;
	private Vec3 position;
	private double size;
	private Quaterniondc rotation;

	private PlanetData(final LevelData levelData, final BiConsumer<String, Consumer<CompoundTag>> dataUpdater) {
		this.levelData = levelData;
		this.dataUpdater = dataUpdater;
	}

	public static PlanetData create(final LevelData levelData, final BiConsumer<String, Consumer<CompoundTag>> dataUpdater, final CompoundTag collisionData) {
		final PlanetData data = new PlanetData(levelData, dataUpdater);
		data.position = new Vec3(collisionData.getDouble("x"), collisionData.getDouble("y"), collisionData.getDouble("z"));
		data.size = collisionData.getDouble("scale");
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
		dataUpdater.accept("position", (collisionData) -> {
			collisionData.putDouble("x", position.x);
			collisionData.putDouble("y", position.y);
			collisionData.putDouble("z", position.z);
		});
	}

	public double getSize() {
		return this.size;
	}

	public Quaterniondc getRotation() {
		return this.rotation;
	}

	public void setRotation(final Quaterniondc rotation) {
		this.rotation = rotation;
		dataUpdater.accept("rotation", (collisionData) -> {
			final Vector3d angles = rotation.getEulerAnglesYXZ(new Vector3d());
			collisionData.putDouble("yaw", Math.toDegrees(angles.y));
			collisionData.putDouble("pitch", Math.toDegrees(angles.x));
			collisionData.putDouble("roll", Math.toDegrees(angles.z));
		});
	}
}
