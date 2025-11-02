package net.jcm.vsch.api.resource;

import net.minecraft.resources.ResourceLocation;

public record TextureLocation(ResourceLocation location, int offsetX, int offsetY, float scale) {
	public TextureLocation(ResourceLocation location, int offsetX, int offsetY) {
		this(location, offsetX, offsetY, 1f);
	}

	public static TextureLocation fromNonStandardSize(ResourceLocation location, int offsetX, int offsetY, int size) {
		return new TextureLocation(location, offsetX, offsetY, 16f / size);
	}
}
