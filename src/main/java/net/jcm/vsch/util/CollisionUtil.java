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
package net.jcm.vsch.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class CollisionUtil {
	private CollisionUtil() {}

	public static boolean willCollide(
		final CollisionGetter level,
		final Entity entity,
		final AABBd box,
		final Matrix4dc box2voxel,
		final Matrix4dc voxel2box,
		final Vector3d movement
	) {
		final Vector3d movementInVoxel = box2voxel.transformDirection(movement, new Vector3d());
		final AABBd box1 = box.translate(movement, new AABBd());
		final AABBd box2 = box.transform(box2voxel, new AABBd()).translate(movementInVoxel);
		final boolean[] intersect = new boolean[1];
		for (final VoxelShape shape : level.getBlockCollisions(entity, toAABB(box2))) {
			if (shape.isEmpty()) {
				continue;
			}
			final AABBd voxel = toAABBd(shape.bounds());
			if (!box2.intersectsAABB(voxel)) {
				continue;
			}
			if (!voxel.transform(voxel2box).intersectsAABB(box1)) {
				continue;
			}
			final AABBd voxelInBox = new AABBd();
			shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
				if (intersect[0]) {
					return;
				}
				voxel
					.setMin(minX, minY, minZ)
					.setMax(maxX, maxY, maxZ);
				if (!box2.intersectsAABB(voxel)) {
					return;
				}
				voxel.transform(voxel2box, voxelInBox);
				if (!box1.intersectsAABB(voxelInBox)) {
					return;
				}
				intersect[0] = true;
			});
			if (intersect[0]) {
				return true;
			}
		}
		return false;
	}

	public static boolean willCollideAny(
		final Level level,
		final Entity entity,
		final AABBd box,
		final Matrix4dc box2world,
		final Vector3d movement
	) {
		final Matrix4dc world2box = box2world.invert(new Matrix4d());
		if (willCollide(level, entity, box, box2world, world2box, movement)) {
			return true;
		}

		final Matrix4d entityToShip = new Matrix4d();
		final Matrix4d shipToEntity = new Matrix4d();
		final AABBd checkBox = box.transform(box2world, new AABBd());
		final String dimId = VSGameUtilsKt.getDimensionId(level);
		for (final LoadedShip ship : VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips()) {
			if (!ship.getChunkClaimDimension().equals(dimId)) {
				continue;
			}
			if (!ship.getWorldAABB().intersectsAABB(checkBox)) {
				continue;
			}
			entityToShip.set(ship.getWorldToShip()).mul(box2world);
			shipToEntity.set(world2box).mul(ship.getShipToWorld());
			if (willCollide(level, entity, box, entityToShip, shipToEntity, movement)) {
				return true;
			}
		}
		return false;
	}

	public static BlockPos findSupportingBlockNoOrientation(final Entity entity, final double downExtend) {
		final Vec3 position = entity.position();
		final EntityDimensions dimensions = entity.getDimensions(entity.getPose());
		final AABBd box = new AABBd(
			-dimensions.width / 2, -downExtend, -dimensions.width / 2,
			dimensions.width / 2, dimensions.height, dimensions.width / 2
		);
		final Matrix4dc box2world = new Matrix4d().translation(position.x, position.y, position.z);
		return findSupportingBlock(entity.level(), entity, box, new Vector3d(), box2world);
	}

	public static BlockPos findSupportingBlock(
		final Level level,
		final Entity entity,
		final AABBd box,
		final Vector3dc feetPos,
		final Matrix4dc box2world
	) {
		BlockPos closestBlock = null;
		double closestDistance = Double.POSITIVE_INFINITY;

		final AABBd worldBox = box.transform(box2world, new AABBd());
		final Vector3d feetPos2 = new Vector3d();
		{
			final BlockPos block = level.findSupportingBlock(entity, toAABB(worldBox)).orElse(null);
			if (block != null) {
				box2world.transformPosition(feetPos, feetPos2);
				final double dist = block.distToCenterSqr(feetPos2.x, feetPos2.y, feetPos2.z);
				// TODO: maybe check the actual face to face distance instead?
				closestDistance = dist;
				closestBlock = block;
			}
		}

		final Matrix4d entityToShip = new Matrix4d();
		final AABBd box2 = new AABBd();
		final String dimId = VSGameUtilsKt.getDimensionId(level);
		for (final LoadedShip ship : VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips()) {
			if (!ship.getChunkClaimDimension().equals(dimId)) {
				continue;
			}
			if (!ship.getWorldAABB().intersectsAABB(worldBox)) {
				continue;
			}
			entityToShip.set(ship.getWorldToShip()).mul(box2world);
			final BlockPos block = level.findSupportingBlock(entity, toAABB(box.transform(entityToShip, box2))).orElse(null);
			if (block == null) {
				continue;
			}
			entityToShip.transformPosition(feetPos, feetPos2);
			final double dist = block.distToCenterSqr(feetPos2.x, feetPos2.y, feetPos2.z);
			if (dist >= closestDistance) {
				continue;
			}
			closestDistance = dist;
			closestBlock = block;
		}
		return closestBlock;
	}

	public static AABBd expandTowards(final AABBd box, final Vector3dc vec) {
		return expandTowards(box, vec.x(), vec.y(), vec.z());
	}

	public static AABBd expandTowards(final AABBd box, final double x, final double y, final double z) {
		if (x < 0) {
			box.minX += x;
		} else {
			box.maxX += x;
		}
		if (y < 0) {
			box.minY += y;
		} else {
			box.maxY += y;
		}
		if (z < 0) {
			box.minZ += z;
		} else {
			box.maxZ += z;
		}
		return box;
	}

	private static AABBd toAABBd(final AABB box) {
		return new AABBd(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	private static AABB toAABB(final AABBd box) {
		return new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}
}
