package net.jcm.vsch.items.pipe;

import net.jcm.vsch.api.pipe.PipeNodeProvider;
import net.jcm.vsch.pipe.OmniNode;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;

public final class OmniNodeItem extends PipeNodeItem<OmniNode> {
	private static final EnumMap<DyeColor, OmniNodeItem> COLOR_MAP = new EnumMap<>(DyeColor.class);

	public OmniNodeItem(final DyeColor color, final Item.Properties props) {
		super(color, props);
		COLOR_MAP.put(color, this);
	}

	public static OmniNodeItem getByColor(final DyeColor color) {
		return COLOR_MAP.get(color);
	}

	@Override
	protected String getDescriptionName() {
		return "omni_node";
	}

	@Override
	public PipeNodeProvider<OmniNode> getPipeNodeProvider(final ItemStack stack) {
		return (level, pos) -> {
			final OmniNode node = new OmniNode(level, pos);
			node.setColor(this.getColor());
			return node;
		};
	}
}
