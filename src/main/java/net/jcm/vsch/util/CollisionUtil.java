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

import java.util.ArrayList;
import java.util.List;

public final class CollisionUtil {
	private static final double EPS = 1e-6;
	private static final double EPS2 = 1.0 / 64;

	private static final AxisSet AXES = new AxisSet(
		new Vector3d(-1, 0, 0), new Vector3d(1, 0, 0),
		new Vector3d(0, -1, 0), new Vector3d(0, 1, 0),
		new Vector3d(0, 0, -1), new Vector3d(0, 0, 1)
	);

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

	public static Vector3d checkCollision(
		final Vector3d movement,
		final CollisionGetter level,
		final Entity entity,
		final AABBd box,
		final Matrix4dc voxel2box,
		final Matrix4dc box2voxel
	) {
		final Vector3d movementInVoxel = box2voxel.transformDirection(movement, new Vector3d());
		final AABBd box1A = box;
		final AABBd box1B = expandTowards(new AABBd(box1A), movement);
		final AABBd box2A = box.transform(box2voxel, new AABBd());
		final AABBd box2B = expandTowards(new AABBd(box2A), movementInVoxel);
		final AxisSet voxelAxes = AXES.transform(voxel2box);
		final Vector3d correction = new Vector3d();
		for (final VoxelShape shape : level.getBlockCollisions(entity, toAABB(box2B))) {
			if (shape.isEmpty()) {
				continue;
			}
			checkShapeCollision(movement, movementInVoxel, box1A, box1B, box2A, box2B, shape, voxel2box, voxelAxes, correction);
		}
		if (correction.lengthSquared() > EPS) {
			movement.add(correction);
		}
		return movement;
	}

	private static void checkShapeCollision(
		final Vector3d movement,
		final Vector3d movementInVoxel,
		final AABBd boxA,
		final AABBd boxB,
		final AABBd boxInVoxelA,
		final AABBd boxInVoxelB,
		final VoxelShape shape,
		final Matrix4dc voxel2box,
		final AxisSet voxelAxes,
		final Vector3d correction
	) {
		final AABBd voxel = toAABBd(shape.bounds());
		if (!boxInVoxelB.intersectsAABB(voxel)) {
			return;
		}
		if (!voxel.transform(voxel2box).intersectsAABB(boxB)) {
			return;
		}
		final AABBd voxelInBox = new AABBd();
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
			voxel
				.setMin(minX, minY, minZ)
				.setMax(maxX, maxY, maxZ);
			if (!boxInVoxelB.intersectsAABB(voxel)) {
				return;
			}
			voxel.transform(voxel2box, voxelInBox);
			if (!boxB.intersectsAABB(voxelInBox)) {
				return;
			}
			checkAABBCollision(movement, movementInVoxel, boxA, boxB, boxInVoxelA, boxInVoxelB, voxelInBox, voxel, voxel2box, voxelAxes, correction);
		});
	}

	private static void checkAABBCollision(
		final Vector3d movement,
		final Vector3d movementInVoxel,
		final AABBd box1A,
		final AABBd box1B,
		final AABBd box2A,
		final AABBd box2B,
		final AABBd voxel1,
		final AABBd voxel2,
		final Matrix4dc voxel2box,
		final AxisSet voxelAxes,
		final Vector3d correction
	) {
		final List<TransformData> vecs = new ArrayList<>(6);
		final boolean
			collideX1 = box1A.maxX > voxel1.minX + EPS2 && box1A.minX + EPS2 < voxel1.maxX,
			collideY1 = box1A.maxY > voxel1.minY + EPS2 && box1A.minY + EPS2 < voxel1.maxY,
			collideZ1 = box1A.maxZ > voxel1.minZ + EPS2 && box1A.minZ + EPS2 < voxel1.maxZ,
			collideX2 = box2A.maxX > voxel2.minX + EPS2 && box2A.minX + EPS2 < voxel2.maxX,
			collideY2 = box2A.maxY > voxel2.minY + EPS2 && box2A.minY + EPS2 < voxel2.maxY,
			collideZ2 = box2A.maxZ > voxel2.minZ + EPS2 && box2A.minZ + EPS2 < voxel2.maxZ;
		if (collideY1 && collideZ1) {
			if (box1A.minX < voxel1.minX) {
				if (box1A.maxX < voxel1.maxX) {
					// ( box /// [ XXX ) \\\ voxel ]
					vecs.add(new TransformData(AXES.xn(), box1A.maxX - voxel1.minX, box1B.maxX - voxel1.minX));
				} else {
					// ( box /// [ voxel XXX ] /// )
				}
			} else if (box1A.maxX > voxel1.maxX) {
				// [ voxel \\\ ( XXX ] /// box )
				vecs.add(new TransformData(AXES.xp(), voxel1.maxX - box1A.minX, voxel1.maxX - box1B.minX));
			} else {
				// [voxel \\\ ( box XXX ) \\\ )
			}
		}
		if (collideX1 && collideZ1) {
			if (box1A.minY < voxel1.minY) {
				if (box1A.maxY < voxel1.maxY) {
					vecs.add(new TransformData(AXES.yn(), box1A.maxY - voxel1.minY, box1B.maxY - voxel1.minY));
				}
			} else if (box1A.maxY > voxel1.maxY) {
				vecs.add(new TransformData(AXES.yp(), voxel1.maxY - box1A.minY, voxel1.maxY - box1B.minY));
			}
		}
		if (collideX1 && collideY1) {
			if (box1A.minZ < voxel1.minZ) {
				if (box1A.maxZ < voxel1.maxZ) {
					vecs.add(new TransformData(AXES.zn(), box1A.maxZ - voxel1.minZ, box1B.maxZ - voxel1.minZ));
				}
			} else if (box1A.maxZ > voxel1.maxZ) {
				vecs.add(new TransformData(AXES.zp(), voxel1.maxZ - box1A.minZ, voxel1.maxZ - box1B.minZ));
			}
		}
		if (collideY2 && collideZ2) {
			if (box2A.minX < voxel2.minX) {
				if (box2A.maxX < voxel2.maxX) {
					vecs.add(new TransformData(voxelAxes.xn(), box2A.maxX - voxel2.minX, box2B.maxX - voxel2.minX));
				}
			} else if (box2A.maxX > voxel2.maxX) {
				vecs.add(new TransformData(voxelAxes.xp(), voxel2.maxX - box2A.minX, voxel2.maxX - box2B.minX));
			}
		}
		if (collideX2 && collideZ2) {
			if (box2A.minY < voxel2.minY) {
				if (box2A.maxY < voxel2.maxY) {
					vecs.add(new TransformData(voxelAxes.yn(), box2A.maxY - voxel2.minY, box2B.maxY - voxel2.minY));
				}
			} else if (box2A.maxY > voxel2.maxY) {
				vecs.add(new TransformData(voxelAxes.yp(), voxel2.maxY - box2A.minY, voxel2.maxY - box2B.minY));
			}
		}
		if (collideX2 && collideY2) {
			if (box2A.minZ < voxel2.minZ) {
				if (box2A.maxZ < voxel2.maxZ) {
					vecs.add(new TransformData(voxelAxes.zn(), box2A.maxZ - voxel2.minZ, box2B.maxZ - voxel2.minZ));
				}
			} else if (box2A.maxZ > voxel2.maxZ) {
				vecs.add(new TransformData(voxelAxes.zp(), voxel2.maxZ - box2A.minZ, voxel2.maxZ - box2B.minZ));
			}
		}
		if (vecs.isEmpty()) {
			return;
		}
		double minDist = Double.POSITIVE_INFINITY;
		Vector3d corrVec = new Vector3d();
		for (final TransformData d : vecs) {
			final double proj = movement.dot(d.normal());
			if (proj > 0) {
				continue;
			}
			if (d.embed() < minDist) {
				minDist = d.embed();
				d.normal().mul(Math.min(d.correction(), -proj), corrVec);
			}
		}
		if (corrVec.lengthSquared() > EPS) {
			if (movement.x < -EPS) {
				correction.x = Math.max(correction.x, corrVec.x);
			} else if (movement.x > EPS) {
				correction.x = Math.min(correction.x, corrVec.x);
			} else if (Math.abs(corrVec.x) > Math.abs(correction.x)) {
				correction.x = corrVec.x;
			}
			if (movement.y < -EPS) {
				correction.y = Math.max(correction.y, corrVec.y);
			} else if (movement.y > EPS) {
				correction.y = Math.min(correction.y, corrVec.y);
			} else if (Math.abs(corrVec.y) > Math.abs(correction.y)) {
				correction.y = corrVec.y;
			}
			if (movement.z < -EPS) {
				correction.z = Math.max(correction.z, corrVec.z);
			} else if (movement.z > EPS) {
				correction.z = Math.min(correction.z, corrVec.z);
			} else if (Math.abs(corrVec.z) > Math.abs(correction.z)) {
				correction.z = corrVec.z;
			}
		}
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

	private record TransformData(Vector3dc normal, double embed, double correction) {}

	private record AxisSet(
		Vector3dc xn, Vector3dc xp,
		Vector3dc yn, Vector3dc yp,
		Vector3dc zn, Vector3dc zp
	) {
		public AxisSet transform(final Matrix4dc transform) {
			return new AxisSet(
				transform.transformDirection(this.xn, new Vector3d()),
				transform.transformDirection(this.xp, new Vector3d()),
				transform.transformDirection(this.yn, new Vector3d()),
				transform.transformDirection(this.yp, new Vector3d()),
				transform.transformDirection(this.zn, new Vector3d()),
				transform.transformDirection(this.zp, new Vector3d())
			);
		}
	}
}
