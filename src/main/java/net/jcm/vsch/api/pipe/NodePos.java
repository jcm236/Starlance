package net.jcm.vsch.api.pipe;

import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record NodePos(
	// The block position the node relative to
	BlockPos blockPos,
	// Which axis of the block the node is on (origin is on the lower corner)
	Direction.Axis axis,
	// The index (aka distance) from the origin, in range of [0, 7]
	int index
) {
	public static final int INDEX_BOUND = 8;
	public static final int UNIQUE_INDEX_BOUND = 1 + 3 * (INDEX_BOUND - 1);
	private static final double NODE_SIZE = 1.0 / INDEX_BOUND;

	public int uniqueIndex() {
		if (this.index == 0) {
			return 0;
		}
		return this.axis.ordinal() * 7 + this.index;
	}

	public static NodePos originOf(final BlockPos blockPos) {
		return new NodePos(blockPos, Direction.Axis.X, 0);
	}

	public static NodePos fromUniqueIndex(final BlockPos blockPos, final int uniqueIndex) {
		if (uniqueIndex == 0) {
			return NodePos.originOf(blockPos);
		}
		final int axisInd = (uniqueIndex - 1) / 7;
		final Direction.Axis axis = Direction.Axis.VALUES[axisInd];
		return new NodePos(blockPos, axis, uniqueIndex - axisInd * 7);
	}

	public static NodePos fromHitResult(final Level level, final BlockPos blockPos, Vec3 pos, final double size) {
		if (!VSGameUtilsKt.isBlockInShipyard(level, pos)) {
			final Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockPos);
			if (ship != null) {
				final Vector3d worldPos = ship.getWorldToShip().transformPosition(new Vector3d(pos.x, pos.y, pos.z));
				pos = new Vec3(worldPos.x, worldPos.y, worldPos.z);
			}
		}
		return fromVec3(pos, size);
	}

	public static NodePos fromVec3(final Vec3 pos, final double size) {
		final double r = size / 2;
		final Vec3 adjusted = pos.add(r, r, r);
		final BlockPos blockPos = BlockPos.containing(adjusted);
		final double x = adjusted.x - blockPos.getX(), y = adjusted.y - blockPos.getY(), z = adjusted.z - blockPos.getZ();
		final boolean xIn = x <= size, yIn = y <= size, zIn = z <= size;
		Direction.Axis axis = null;
		if (xIn) {
			if (yIn) {
				if (zIn) {
					return NodePos.originOf(blockPos);
				}
				axis = Direction.Axis.Z;
			} else if (zIn) {
				axis = Direction.Axis.Y;
			}
		} else if (yIn && zIn) {
			axis = Direction.Axis.X;
		}
		if (axis == null) {
			return null;
		}
		return new NodePos(blockPos, axis, (int)((int)(axis.choose(x, y, z) / size) * (size / NODE_SIZE)));
	}

	public boolean isOrigin() {
		return this.index == 0;
	}

	public boolean isOnAxis(final Direction.Axis axis) {
		return this.index == 0 || this.axis == axis;
	}

	@Override
	public boolean equals(final Object otherObj) {
		return this == otherObj || otherObj instanceof NodePos other &&
			this.blockPos.equals(other.blockPos) &&
			(
				this.isOrigin() && other.isOrigin() ||
				this.axis == other.axis && this.index == other.index
			);
	}

	@Override
	public int hashCode() {
		return this.blockPos.hashCode() + this.uniqueIndex() * 31;
	}

	public Vec3 getCenter() {
		if (this.isOrigin()) {
			return Vec3.atLowerCornerOf(this.blockPos);
		}
		return new Vec3(
			this.blockPos.getX() + this.axis.choose(this.index, 0, 0) / (double)(INDEX_BOUND),
			this.blockPos.getY() + this.axis.choose(0, this.index, 0) / (double)(INDEX_BOUND),
			this.blockPos.getZ() + this.axis.choose(0, 0, this.index) / (double)(INDEX_BOUND)
		);
	}

	public double manhattanDistTo(final NodePos other) {
		final Vec3 center = this.getCenter();
		final Vec3 otherCenter = other.getCenter();
		return Math.abs(center.x - otherCenter.x) + Math.abs(center.y - otherCenter.y) + Math.abs(center.z - otherCenter.z);
	}

	public AABB getAABB(final double size) {
		final double r = size / 2;
		final Vec3 center = this.getCenter();
		return new AABB(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r);
	}

	public boolean canAnchoredIn(final Level level, final double size) {
		final AABB box = this.getAABB(size);
		for (final VoxelShape shape : level.getBlockCollisions(null, box)) {
			if (!shape.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public void writeTo(final FriendlyByteBuf buf) {
		buf.writeBlockPos(this.blockPos);
		buf.writeByte(this.uniqueIndex());
	}

	public static NodePos readFrom(final FriendlyByteBuf buf) {
		final BlockPos blockPos = buf.readBlockPos();
		final int index = buf.readByte();
		return NodePos.fromUniqueIndex(blockPos, index);
	}

	public static Stream<NodePos> streamNodePosOn(final BlockPos pos) {
		return Stream.concat(
			Stream.of(
				NodePos.originOf(pos),
				NodePos.originOf(pos.offset(0, 0, 1)),
				NodePos.originOf(pos.offset(0, 1, 0)),
				NodePos.originOf(pos.offset(0, 1, 1)),
				NodePos.originOf(pos.offset(1, 0, 0)),
				NodePos.originOf(pos.offset(1, 0, 1)),
				NodePos.originOf(pos.offset(1, 1, 0)),
				NodePos.originOf(pos.offset(1, 1, 1))
			),
			Stream.of(Direction.Axis.values()).flatMap((axis) -> IntStream.range(1, INDEX_BOUND)
				.filter((i) -> i % 2 == 0)
				.mapToObj((i) -> Stream.of(
					new NodePos(pos, axis, i),
					new NodePos(pos.offset(axis.choose(0, 0, 0), axis.choose(0, 0, 1), axis.choose(1, 1, 0)), axis, i),
					new NodePos(pos.offset(axis.choose(0, 1, 1), axis.choose(1, 0, 0), axis.choose(0, 0, 0)), axis, i),
					new NodePos(pos.offset(axis.choose(0, 1, 1), axis.choose(1, 0, 1), axis.choose(1, 1, 0)), axis, i)
				))
				.flatMap(Function.identity())
			)
		);
	}

	public static Stream<NodePos> streamPlaceHint(final NodeLevel level, final BlockPos pos) {
		return streamNodePosOn(pos)
			.filter((p) -> level.getNode(p) == null)
			.filter((p) -> p.canAnchoredIn(level.getLevel(), 4.0 / 16));
	}

	public Stream<NodePos> streamPossibleToConnect() {
		if (this.isOrigin()) {
			return Direction.stream()
				.flatMap((dir) -> {
					final Direction.Axis dirAxis = dir.getAxis();
					final BlockPos relative = this.blockPos.relative(dir);
					return streamAxisesExclude(dirAxis)
						.flatMap((axis) -> {
							final BlockPos relNeg = relative.relative(axis, -1);
							return IntStream.range(1, INDEX_BOUND)
								.mapToObj((i) -> Stream.of(new NodePos(relative, axis, i), new NodePos(relNeg, axis, i)))
								.flatMap(Function.identity());
						});
				});
		}

		final Direction.Axis thisAxis = this.axis;

		return Direction.stream()
			.flatMap((dir) -> {
				final BlockPos relative = this.blockPos.relative(dir);
				final Direction.Axis dirAxis = dir.getAxis();
				if (dirAxis == thisAxis) {
					return streamAxisesExclude(thisAxis)
						.flatMap((axis) -> {
							final BlockPos rel2 = relative.relative(axis, -1);
							return IntStream.range(1, INDEX_BOUND)
								.mapToObj((i) -> Stream.of(new NodePos(relative, axis, i), new NodePos(rel2, axis, i)))
								.flatMap(Function.identity());
						});
				}
				final BlockPos relative2 = relative.relative(thisAxis, 1);
				return Stream.concat(
					Stream.of(new NodePos(relative, thisAxis, 0)),
					IntStream.range(0, INDEX_BOUND).mapToObj((i) -> new NodePos(relative, thisAxis, i))
				);
			});
	}

	public Direction[] connectPathTo(final NodePos other) {
		final boolean selfIsOrigin = this.isOrigin();
		final boolean otherIsOrigin = other.isOrigin();

		final int xDiff = (other.blockPos.getX() - this.blockPos.getX()) * INDEX_BOUND +
			(other.axis.choose(other.index, 0, 0) - this.axis.choose(this.index, 0, 0));
		final int yDiff = (other.blockPos.getY() - this.blockPos.getY()) * INDEX_BOUND +
			(other.axis.choose(0, other.index, 0) - this.axis.choose(0, this.index, 0));
		final int zDiff = (other.blockPos.getZ() - this.blockPos.getZ()) * INDEX_BOUND +
			(other.axis.choose(0, 0, other.index) - this.axis.choose(0, 0, this.index));
		final int alignCount = (xDiff == 0 ? 1 : 0) + (yDiff == 0 ? 1 : 0) + (zDiff == 0 ? 1 : 0);
		if (alignCount == 0) {
			throw new IllegalArgumentException("Impossible connection: node positions must align on at least one plane");
		}
		if (alignCount == 3) {
			throw new IllegalArgumentException("Cannot connect to self");
		}
		final Direction xDir = Direction.fromAxisAndDirection(
			Direction.Axis.X,
			xDiff > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE
		);
		final Direction yDir = Direction.fromAxisAndDirection(
			Direction.Axis.Y,
			yDiff > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE
		);
		final Direction zDir = Direction.fromAxisAndDirection(
			Direction.Axis.Z,
			zDiff > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE
		);
		if (selfIsOrigin) {
			if (otherIsOrigin) {
				throw new IllegalArgumentException("Impossible connection: origin node cannot connect to another origin node");
			}
			if (alignCount == 2) {
				throw new IllegalArgumentException("Impossible connection: origin node cannot connect to another node that on the same line");
			}
			if (xDiff == 0) {
				switch (other.axis) {
					case Y:
						return new Direction[]{yDir, zDir};
					case Z:
						return new Direction[]{zDir, yDir};
				}
			} else if (yDiff == 0) {
				switch (other.axis) {
					case X:
						return new Direction[]{xDir, zDir};
					case Z:
						return new Direction[]{zDir, xDir};
				}
			} else if (zDiff == 0) {
				switch (other.axis) {
					case X:
						return new Direction[]{xDir, yDir};
					case Y:
						return new Direction[]{yDir, xDir};
				}
			}
			throw new IllegalStateException("unreachable");
		}
		if (otherIsOrigin) {
			if (alignCount == 2) {
				throw new IllegalArgumentException("Impossible connection: origin node cannot connect to another node that on the same line");
			}
			if (xDiff == 0) {
				switch (this.axis) {
					case Y:
						return new Direction[]{zDir, yDir};
					case Z:
						return new Direction[]{yDir, zDir};
				}
			} else if (yDiff == 0) {
				switch (this.axis) {
					case X:
						return new Direction[]{zDir, xDir};
					case Z:
						return new Direction[]{xDir, zDir};
				}
			} else if (zDiff == 0) {
				switch (this.axis) {
					case X:
						return new Direction[]{yDir, xDir};
					case Y:
						return new Direction[]{xDir, yDir};
				}
			}
			throw new IllegalStateException("unreachable");
		}
		if (alignCount == 2) {
			if (xDiff != 0) {
				return new Direction[]{xDir};
			}
			if (yDiff != 0) {
				return new Direction[]{yDir};
			}
			if (zDiff != 0) {
				return new Direction[]{zDir};
			}
			throw new IllegalStateException("unreachable");
		}
		if (alignCount == 1) {
			if (this.axis == other.axis) {
				if (xDiff == 0) {
					switch (this.axis) {
						case Y:
							return new Direction[]{zDir, yDir, zDir};
						case Z:
							return new Direction[]{yDir, zDir, yDir};
					}
				} else if (yDiff == 0) {
					switch (this.axis) {
						case X:
							return new Direction[]{zDir, xDir, zDir};
						case Z:
							return new Direction[]{xDir, zDir, xDir};
					}
				} else if (zDiff == 0) {
					switch (this.axis) {
						case X:
							return new Direction[]{yDir, xDir, yDir};
						case Y:
							return new Direction[]{xDir, yDir, xDir};
					}
				}
				throw new IllegalStateException("unreachable");
			}
			if (xDiff == 0) {
				switch (this.axis) {
					case Y:
						return new Direction[]{zDir, yDir};
					case Z:
						return new Direction[]{yDir, zDir};
				}
			} else if (yDiff == 0) {
				switch (this.axis) {
					case X:
						return new Direction[]{zDir, xDir};
					case Z:
						return new Direction[]{xDir, zDir};
				}
			} else if (zDiff == 0) {
				switch (this.axis) {
					case X:
						return new Direction[]{yDir, xDir};
					case Y:
						return new Direction[]{xDir, yDir};
				}
			}
			throw new IllegalStateException("unreachable");
		}
		throw new IllegalStateException("unreachable");
	}

	private static Stream<Direction.Axis> streamAxisesExclude(final Direction.Axis exclude) {
		final Direction.Axis[] axises = new Direction.Axis[2];
		int index = 0;
		for (final Direction.Axis axis : Direction.Axis.VALUES) {
			if (axis != exclude) {
				axises[index++] = axis;
			}
		}
		return Stream.of(axises);
	}

	public RelativeNodePos asRelative(final BlockPos blockPos) {
		if (this.blockPos.equals(blockPos)) {
			return new RelativeNodePos(
				this.axis.choose(this.index, 0, 0),
				this.axis.choose(0, this.index, 0),
				this.axis.choose(0, 0, this.index)
			);
		}
		final int xIndex = this.blockPos.getX() == blockPos.getX() ? 0 : this.blockPos.getX() == blockPos.getX() + 1 ? INDEX_BOUND : -1;
		final int yIndex = this.blockPos.getY() == blockPos.getY() ? 0 : this.blockPos.getY() == blockPos.getY() + 1 ? INDEX_BOUND : -1;
		final int zIndex = this.blockPos.getZ() == blockPos.getZ() ? 0 : this.blockPos.getZ() == blockPos.getZ() + 1 ? INDEX_BOUND : -1;
		if (xIndex != -1 && yIndex != -1 && zIndex != -1) {
			if (this.isOrigin()) {
				return new RelativeNodePos(xIndex, yIndex, zIndex);
			}
			switch (this.axis) {
				case X -> {
					if (xIndex == 0) {
						return new RelativeNodePos(this.index, yIndex, zIndex);
					}
				}
				case Y -> {
					if (yIndex == 0) {
						return new RelativeNodePos(xIndex, this.index, zIndex);
					}
				}
				case Z -> {
					if (zIndex == 0) {
						return new RelativeNodePos(xIndex, yIndex, this.index);
					}
				}
			}
		}
		throw new IllegalArgumentException("Invalid base block pos " + blockPos + " to find relative of " + this.toString());
	}

	public Stream<BlockPos> streamTouchingBlocks(final Level level) {
		final double size = 4.0 / 16;
		final double r = size / 2;
		final Vec3 centerPos = this.getCenter();
		return Stream.of(
			new Vec3(centerPos.x + r, centerPos.y + r, centerPos.z + r),
			new Vec3(centerPos.x - r, centerPos.y + r, centerPos.z + r),
			new Vec3(centerPos.x + r, centerPos.y + r, centerPos.z - r),
			new Vec3(centerPos.x - r, centerPos.y + r, centerPos.z - r),
			new Vec3(centerPos.x + r, centerPos.y - r, centerPos.z + r),
			new Vec3(centerPos.x - r, centerPos.y - r, centerPos.z + r),
			new Vec3(centerPos.x + r, centerPos.y - r, centerPos.z - r),
			new Vec3(centerPos.x - r, centerPos.y - r, centerPos.z - r)
		)
			.filter((corner) -> {
				final double EPSILON = 1e-6;
				final double x = centerPos.x + (corner.x > centerPos.x ? EPSILON : -EPSILON);
				final double y = centerPos.y + (corner.y > centerPos.y ? EPSILON : -EPSILON);
				final double z = centerPos.z + (corner.z > centerPos.z ? EPSILON : -EPSILON);
				final AABB box = new AABB(x, y, z, corner.x, corner.y, corner.z);
				for (final VoxelShape shape : level.getBlockCollisions(null, box)) {
					if (!shape.isEmpty()) {
						return true;
					}
				}
				return false;
			})
			.map(BlockPos::containing)
			.distinct();
	}
}
