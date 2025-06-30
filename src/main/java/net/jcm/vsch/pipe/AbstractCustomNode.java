package net.jcm.vsch.pipe;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;

public abstract class AbstractCustomNode<T extends AbstractCustomNode<T>> extends PipeNode<T> {
	protected AbstractCustomNode(final DyeColor color) {
		super(color, Type.CUSTOM);
	}

	public abstract void readAdditional(FriendlyByteBuf buf);
}
