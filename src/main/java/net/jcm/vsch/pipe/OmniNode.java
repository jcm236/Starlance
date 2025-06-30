package net.jcm.vsch.pipe;

import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.Fluid;

public final class OmniNode extends PipeNode<OmniNode> {
	private static OmniNode[] NODES = new OmniNode[16];

	static {
		for (final DyeColor color : DyeColor.values()) {
			NODES[color.getId()] = new OmniNode(color);
		}
	}

	private OmniNode(final DyeColor color) {
		super(color, Type.OMNI);
	}

	public static OmniNode getByColor(final DyeColor color) {
		return NODES[color.getId()];
	}

	@Override
	public OmniNode withColor(final DyeColor color) {
		return getByColor(color);
	}

	@Override
	public boolean canConnect(final NodeLevel level, final NodePos pos, final Direction dir) {
		return true;
	}

	@Override
	public boolean canFluidFlow(final NodeLevel level, final NodePos pos, final Direction dir, final Fluid fluid) {
		return true;
	}

	@Override
	public void writeAdditional(final FriendlyByteBuf buf) {}
}
