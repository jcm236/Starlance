package net.jcm.vsch.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

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

	public int getUniqueIndex() {
		if (this.index == 0) {
			return 0;
		}
		return this.axis.ordinal() * 7 + this.index;
	}

	public static NodePos fromUniqueIndex(final BlockPos blockPos, final int uniqueIndex) {
		if (uniqueIndex == 0) {
			return new NodePos(blockPos, Direction.Axis.X, 0);
		}
		final int axisInd = (uniqueIndex - 1) / 7;
		final Direction.Axis axis = Direction.Axis.VALUES[axisInd];
		return new NodePos(blockPos, axis, uniqueIndex - axisInd * 7);
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
		return this.blockPos.hashCode() + this.getUniqueIndex() * 31;
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
}
