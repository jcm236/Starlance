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
	private static final ModelTextures PIPE_MODEL_XN;
	private static final ModelTextures PIPE_MODEL_YN;
	private static final ModelTextures PIPE_MODEL_ZN;
	private static final ModelTextures PIPE_MODEL_XP;
	private static final ModelTextures PIPE_MODEL_YP;
	private static final ModelTextures PIPE_MODEL_ZP;

	static {
		final ResourceLocation resource = new ResourceLocation(VSCHMod.MODID, "block/pipe/omni_node");
		final TextureLocation node1 = new TextureLocation(resource, 8, 4);
		final TextureLocation node2 = new TextureLocation(resource, 8, 8);
		final TextureLocation pipeH1 = new TextureLocation(resource, 0, 0);
		final TextureLocation pipeH2 = new TextureLocation(resource, 4, 12);
		final TextureLocation pipeH1R = TextureLocation.fromEnd(resource, 12, 4);
		final TextureLocation pipeH2R = TextureLocation.fromEnd(resource, 16, 16);
		final TextureLocation pipeV1 = new TextureLocation(resource, 0, 4);
		final TextureLocation pipeV2 = new TextureLocation(resource, 12, 0);
		final TextureLocation pipeV1R = TextureLocation.fromEnd(resource, 4, 16);
		final TextureLocation pipeV2R = TextureLocation.fromEnd(resource, 16, 12);
		final TextureLocation end1 = new TextureLocation(resource, 4, 4);
		final TextureLocation end2 = new TextureLocation(resource, 4, 8);
		MODEL = new ModelTextures(node1, node2, node1, node2, node1, node2);
		PIPE_MODEL_XN = new ModelTextures(pipeH1, pipeH2, pipeH1, pipeH2, end1, end2);
		PIPE_MODEL_YN = new ModelTextures(end1, end2, pipeV1R, pipeV2R, pipeV1R, pipeV2R);
		PIPE_MODEL_ZN = new ModelTextures(pipeV1R, pipeV2R, end1, end2, pipeH2, pipeH1);
		PIPE_MODEL_XP = new ModelTextures(pipeH1R, pipeH2R, pipeH1R, pipeH2R, end2, end1);
		PIPE_MODEL_YP = new ModelTextures(end2, end1, pipeV1, pipeV2, pipeV1, pipeV2);
		PIPE_MODEL_ZP = new ModelTextures(pipeV1, pipeV2, end2, end1, pipeH2R, pipeH1R);
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
	public ModelTextures getPipeModel(final Direction direction) {
		return switch (direction) {
			case DOWN -> PIPE_MODEL_YN;
			case UP -> PIPE_MODEL_YP;
			case NORTH -> PIPE_MODEL_ZN;
			case SOUTH -> PIPE_MODEL_ZP;
			case WEST -> PIPE_MODEL_XN;
			case EAST -> PIPE_MODEL_XP;
		};
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
