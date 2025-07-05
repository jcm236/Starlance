package net.jcm.vsch.api.resource;

import net.minecraft.core.Direction;

public record ModelTextures(
	TextureLocation down,
	TextureLocation up,
	TextureLocation north,
	TextureLocation south,
	TextureLocation west,
	TextureLocation east
) {
	public TextureLocation getTexture(final Direction dir) {
		return switch (dir) {
			case DOWN -> this.down;
			case UP -> this.up;
			case NORTH -> this.north;
			case SOUTH -> this.south;
			case WEST -> this.west;
			case EAST -> this.east;
		};
	}
}
