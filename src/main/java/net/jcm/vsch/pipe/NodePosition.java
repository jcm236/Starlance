package net.jcm.vsch.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record NodePosition(
	// The block position the node relative to
	BlockPos blockPos,
	// Which axis of the block the node is on (origin is on the lower corner)
	Direction.Axis axis,
	// The index (aka distance) from the origin, in range of [0, 7]
	int index
) {
	public static final int INDEX_BOUND = 8;

	public boolean isOrigin() {
		return this.index == 0;
	}

	public boolean isOnAxis(final Direction.Axis axis) {
		return this.index == 0 || this.axis == axis;
	}

	public boolean equals(final Object otherObj) {
		return this == otherObj || otherObj instanceof NodePosition other &&
			this.blockPos.equals(other.blockPos) &&
			(
				this.isOrigin() && other.isOrigin() ||
				this.axis == other.axis && this.index == other.index
			);
	}

	public Vec3 getCenter() {
		if (this.isOrigin()) {
			return Vec3.atLowerCornerOf(this.blockPos);
		}
		return new Vec3(
			this.blockPos.getX() + this.axis.choose(this.index, 0, 0) / 8.0,
			this.blockPos.getY() + this.axis.choose(0, this.index, 0) / 8.0,
			this.blockPos.getZ() + this.axis.choose(0, 0, this.index) / 8.0
		);
	}

	public Stream<NodePosition> streamPossibleToConnect() {
		if (this.isOrigin()) {
			return Direction.stream()
				.flatMap((dir) -> {
					final Direction.Axis dirAxis = dir.getAxis();
					final BlockPos relative = this.blockPos.relative(dir);
					return streamAxisesExclude(dirAxis)
						.flatMap((axis) -> {
							final BlockPos relNeg = relative.relative(axis, -1);
							return IntStream.range(1, INDEX_BOUND)
								.mapToObj((i) -> Stream.of(new NodePosition(relative, axis, i), new NodePosition(relNeg, axis, i)))
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
								.mapToObj((i) -> Stream.of(new NodePosition(relative, axis, i), new NodePosition(rel2, axis, i)))
								.flatMap(Function.identity());
						});
				}
				final BlockPos relative2 = relative.relative(thisAxis, 1);
				return Stream.concat(
					Stream.of(new NodePosition(relative, thisAxis, 0)),
					IntStream.range(0, INDEX_BOUND).mapToObj((i) -> new NodePosition(relative, thisAxis, i))
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
