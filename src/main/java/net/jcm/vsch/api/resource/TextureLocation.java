package net.jcm.vsch.api.resource;

import net.minecraft.resources.ResourceLocation;

public record TextureLocation(ResourceLocation location, int offsetX, int offsetY) {}
