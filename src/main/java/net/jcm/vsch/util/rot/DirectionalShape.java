package net.jcm.vsch.util.rot;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/*
 * This code has been translated from kotlin and used from the Tournament source code
 * https://github.com/alex-s168/VS_tournament_continued/blob/main/common/src/main/kotlin/org/valkyrienskies/tournament/util/RotShapes.kt
 * 
 * All credit goes to Constantdust (who Alex tells me wrote this section of tournament)
 */
public class DirectionalShape {
	private final VoxelShape north;
	private final VoxelShape east;
	private final VoxelShape south;
	private final VoxelShape west;
	private final VoxelShape up;
	private final VoxelShape down;

	private DirectionalShape(RotShape shape) {
		this.north = shape.build();
		this.east = shape.rotate90().build();
		this.south = shape.rotate180().build();
		this.west = shape.rotate270().build();
		this.up = shape.xrotate90().build();
		this.down = shape.xrotate270().build();
	}

	public VoxelShape get(Direction direction) {
		return switch (direction) {
			case NORTH -> this.north;
			case EAST -> this.east;
			case SOUTH -> this.south;
			case WEST -> this.west;
			case UP -> this.up;
			case DOWN -> this.down;
		};
	}

	public static DirectionalShape north(RotShape shape) {
		return new DirectionalShape(shape);
	}

	public static DirectionalShape east(RotShape shape) {
		return new DirectionalShape(shape.rotate270());
	}

	public static DirectionalShape south(RotShape shape) {
		return new DirectionalShape(shape.rotate180());
	}

	public static DirectionalShape west(RotShape shape) {
		return new DirectionalShape(shape.rotate90());
	}

	public static DirectionalShape up(RotShape shape) {
		return new DirectionalShape(shape.xrotate270());
	}

	public static DirectionalShape down(RotShape shape) {
		return new DirectionalShape(shape.xrotate90());
	}
}
