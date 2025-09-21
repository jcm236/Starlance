package net.jcm.vsch.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3d;
import org.joml.Matrix4dc;
import org.joml.primitives.AABBd;

import java.util.ArrayList;
import java.util.List;

public final class CollisionUtil {
	private CollisionUtil() {}

	public static Vector3d checkCollision(
		final Vector3d movement,
		final CollisionGetter level,
		final Entity entity,
		final AABBd box,
		final Matrix4dc voxel2box,
		final Matrix4dc box2voxel
	) {
		final AABBd box1 = box;
		final AABBd box2 = box1.transform(box2voxel, new AABBd());
		for (final VoxelShape shape : level.getBlockCollisions(entity, toAABB(box2))) {
			if (shape.isEmpty()) {
				continue;
			}
			checkShapeCollision(movement, box1, box2, shape, voxel2box);
		}
		return movement;
	}

	private static void checkShapeCollision(
		final Vector3d movement,
		final AABBd box,
		final AABBd boxInVoxel,
		final VoxelShape shape,
		final Matrix4dc voxel2box
	) {
		final AABBd voxel = toAABBd(shape.bounds());
		if (!boxInVoxel.intersectsAABB(voxel)) {
			return;
		}
		if (!voxel.transform(voxel2box).intersectsAABB(box)) {
			return;
		}
		final AABBd voxelInBox = new AABBd();
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
			voxel
				.setMin(minX, minY, minZ)
				.setMax(maxX, maxY, maxZ);
			if (!boxInVoxel.intersectsAABB(voxel)) {
				return;
			}
			voxel.transform(voxel2box, voxelInBox);
			if (!box.intersectsAABB(voxelInBox)) {
				return;
			}
			checkAABBCollision(movement, box, boxInVoxel, voxelInBox, voxel, voxel2box);
		});
	}

	private static void checkAABBCollision(
		final Vector3d movement,
		final AABBd box1,
		final AABBd box2,
		final AABBd voxel1,
		final AABBd voxel2,
		final Matrix4dc voxel2box
	) {
		final List<Vector3d> vecs = new ArrayList<>(6);
		if (box1.minX < voxel1.minX) {
			if (box1.maxX < voxel1.maxX) {
				vecs.add(new Vector3d(voxel1.minX - box1.maxX, 0, 0));
			}
		} else if (box1.maxX > voxel1.maxX) {
			vecs.add(new Vector3d(voxel1.maxX - box1.minX, 0, 0));
		}
		if (box1.minY < voxel1.minY) {
			if (box1.maxY < voxel1.maxY) {
				vecs.add(new Vector3d(0, voxel1.minY - box1.maxY, 0));
			}
		} else if (box1.maxY > voxel1.maxY) {
			vecs.add(new Vector3d(0, voxel1.maxY - box1.minY, 0));
		}
		if (box1.minZ < voxel1.minZ) {
			if (box1.maxZ < voxel1.maxZ) {
				vecs.add(new Vector3d(0, 0, voxel1.minZ - box1.maxZ));
			}
		} else if (box1.maxZ > voxel1.maxZ) {
			vecs.add(new Vector3d(0, 0, voxel1.maxZ - box1.minZ));
		}
		if (box2.minX < voxel2.minX) {
			if (box2.maxX < voxel2.maxX) {
				vecs.add(voxel2box.transformDirection(new Vector3d(voxel2.minX - box2.maxX, 0, 0)));
			}
		} else if (box2.maxX > voxel2.maxX) {
			vecs.add(voxel2box.transformDirection(new Vector3d(voxel2.maxX - box2.minX, 0, 0)));
		}
		if (box2.minY < voxel2.minY) {
			if (box2.maxY < voxel2.maxY) {
				vecs.add(voxel2box.transformDirection(new Vector3d(0, voxel2.minY - box2.maxY, 0)));
			}
		} else if (box2.maxY > voxel2.maxY) {
			vecs.add(voxel2box.transformDirection(new Vector3d(0, voxel2.maxY - box2.minY, 0)));
		}
		if (box2.minZ < voxel2.minZ) {
			if (box2.maxZ < voxel2.maxZ) {
				vecs.add(voxel2box.transformDirection(new Vector3d(0, 0, voxel2.minZ - box2.maxZ)));
			}
		} else if (box2.maxZ > voxel2.maxZ) {
			vecs.add(voxel2box.transformDirection(new Vector3d(0, 0, voxel2.maxZ - box2.minZ)));
		}
		if (vecs.isEmpty()) {
			return;
		}
		Vector3d minVec = null;
		double minLen = 0;
		final Vector3d normal = new Vector3d();
		for (final Vector3d v : vecs) {
			final double l = v.length();
			if (l < 1e-7) {
				continue;
			}
			final double proj = movement.dot(v.div(l, normal));
			if (proj > 0) {
				continue;
			}
			final double corr = Math.min(l, -proj);
			if (minVec == null || corr < minLen) {
				minVec = normal;
				minLen = corr;
			}
		}
		if (minVec != null) {
			movement.add(minVec.mul(minLen));
		}
	}

	private static AABBd toAABBd(final AABB box) {
		return new AABBd(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	private static AABB toAABB(final AABBd box) {
		return new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}
}
