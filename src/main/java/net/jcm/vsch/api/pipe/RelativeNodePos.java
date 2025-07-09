package net.jcm.vsch.api.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record RelativeNodePos(
	int x,
	int y,
	int z
) {
	public NodePos asAbsolute(BlockPos blockPos) {
		int x = this.x, y = this.y, z = this.z;
		if (x == NodePos.INDEX_BOUND) {
			x = 0;
			blockPos = blockPos.offset(1, 0, 0);
		}
		if (y == NodePos.INDEX_BOUND) {
			y = 0;
			blockPos = blockPos.offset(0, 1, 0);
		}
		if (z == NodePos.INDEX_BOUND) {
			z = 0;
			blockPos = blockPos.offset(0, 0, 1);
		}
		if (x == 0) {
			if (y == 0) {
				return new NodePos(blockPos, Direction.Axis.Z, z);
			} else if (z == 0) {
				return new NodePos(blockPos, Direction.Axis.Y, y);
			}
		} else if (y == 0 && z == 0) {
			return new NodePos(blockPos, Direction.Axis.X, x);
		}
		throw new IllegalStateException("Invalid relative node pos: " + this.toString());
	}
}
