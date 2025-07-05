package net.jcm.vsch.api.pipe;

import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public abstract class AbstractCustomNode<T extends AbstractCustomNode<T>> extends PipeNode<T> {
	protected AbstractCustomNode(final NodeLevel level, final NodePos pos) {
		super(level, pos, Type.CUSTOM);
	}

	public abstract ResourceLocation getId();
}
