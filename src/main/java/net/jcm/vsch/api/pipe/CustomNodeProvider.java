package net.jcm.vsch.api.pipe;

import net.minecraft.world.item.DyeColor;

@FunctionalInterface
public interface CustomNodeProvider {
	AbstractCustomNode getNode(DyeColor color);
}
