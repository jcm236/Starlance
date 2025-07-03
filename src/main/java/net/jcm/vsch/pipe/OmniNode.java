package net.jcm.vsch.pipe;

import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.items.pipe.OmniNodeItem;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.EnumMap;

public final class OmniNode extends PipeNode<OmniNode> {
	private static final EnumMap<DyeColor, OmniNode> COLOR_MAP = new EnumMap<>(DyeColor.class);

	static {
		for (final DyeColor color : DyeColor.values()) {
			COLOR_MAP.put(color, new OmniNode(color));
		}
	}

	private OmniNode(final DyeColor color) {
		super(color, Type.OMNI);
	}

	public static OmniNode getByColor(final DyeColor color) {
		return COLOR_MAP.get(color);
	}

	@Override
	public OmniNode withColor(final DyeColor color) {
		return getByColor(color);
	}

	@Override
	public ItemStack asItemStack() {
		return new ItemStack(OmniNodeItem.getByColor(this.getColor()), 1);
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
