package net.jcm.vsch.pipe;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.pipe.FlowDirection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.api.resource.ModelTextures;
import net.jcm.vsch.api.resource.TextureLocation;
import net.jcm.vsch.items.pipe.OmniNodeItem;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.EnumMap;

public class OmniNode extends PipeNode<OmniNode> {
	private static final ModelTextures MODEL;

	static {
		final ResourceLocation resource = new ResourceLocation(VSCHMod.MODID, "block/pipe/omni_node");
		final TextureLocation texture1 = new TextureLocation(resource, 0, 1);
		final TextureLocation texture2 = new TextureLocation(resource, 0, 0);
		MODEL = new ModelTextures(texture1, texture2, texture1, texture2, texture1, texture2);
	}

	public OmniNode(final NodeLevel level, final NodePos pos) {
		super(level, pos, Type.OMNI);
	}

	@Override
	public ItemStack asItemStack() {
		return new ItemStack(OmniNodeItem.getByColor(this.getColor()), 1);
	}

	@Override
	public ModelTextures getModel() {
		return MODEL;
	}

	@Override
	public boolean canConnect(final Direction dir) {
		return true;
	}

	@Override
	public FlowDirection getAccessFlowDirection(final Direction dir) {
		return FlowDirection.BOTH;
	}

	@Override
	public FlowDirection getFlowDirection(final Direction dir) {
		return FlowDirection.BOTH;
	}

	@Override
	protected int getWaterFlowRate() {
		// TODO: adjust the value
		return 6400;
	}

	@Override
	public int energyFlowAmount(final Direction dir) {
		return 8 * 1024;
	}
}
