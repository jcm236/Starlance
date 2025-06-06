package net.jcm.vsch.util.rot;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.List;

/*
 * This code has been translated from kotlin and used from the Tournament source code
 * https://github.com/alex-s168/VS_tournament_continued/blob/main/common/src/main/kotlin/org/valkyrienskies/tournament/util/RotShapes.kt
 * 
 * All credit goes to Constantdust (who Alex tells me wrote this section of tournament)
 */
public class RotShapes {
	public static RotShape cube() {
		return box(0.1, 0.1, 0.1, 15.9, 15.9, 15.9);
	}

	public static RotShape solid() {
		return box(0, 0, 0, 16, 16, 16);
	}

	public static RotShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new Box(x1, y1, z1, x2, y2, z2);
	}

	public static RotShape or(RotShape... shapes) {
		return new Union(Arrays.asList(shapes));
	}

	private static class Box implements RotShape {
		private final double x1, y1, z1, x2, y2, z2;

		private Box(double x1, double y1, double z1, double x2, double y2, double z2) {
			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			this.x2 = x2;
			this.y2 = y2;
			this.z2 = z2;
		}

		@Override
		public RotShape rotate90() {
			return new Box(16 - z1, y1, x1, 16 - z2, y2, x2);
		}

		@Override
		public RotShape xrotate90() {
			return new Box(x1, 16 - z1, y1, x2, 16 - z2, y2);
		}

		@Override
		public VoxelShape makeMcShape() {
			return Shapes.box(
				Math.min(x1, x2) / 16,
				Math.min(y1, y2) / 16,
				Math.min(z1, z2) / 16,
				Math.max(x1, x2) / 16,
				Math.max(y1, y2) / 16,
				Math.max(z1, z2) / 16);
		}
	}

	private static class Union implements RotShape {
		private final List<RotShape> shapes;

		private Union(List<RotShape> shapes) {
			this.shapes = shapes;
		}

		@Override
		public RotShape rotate90() {
			return new Union(shapes.stream().map(RotShape::rotate90).toList());
		}

		@Override
		public RotShape xrotate90() {
			return new Union(shapes.stream().map(RotShape::xrotate90).toList());
		}

		@Override
		public VoxelShape makeMcShape() {
			VoxelShape aggregate = Shapes.empty();
			for (final RotShape sh : shapes) {
			  aggregate = Shapes.or(aggregate, sh.makeMcShape());
			}
			return aggregate;
		}
	}
}

