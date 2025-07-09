package net.jcm.vsch.pipe.level;

import net.jcm.vsch.VSCHCapabilities;
import net.jcm.vsch.accessor.ILevelAccessor;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.api.pipe.RelativeNodePos;
import net.jcm.vsch.api.pipe.capability.INodePortProvider;
import net.jcm.vsch.api.pipe.capability.NodePort;
import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.network.s2c.PipeNodeUpdateS2C;
import net.jcm.vsch.pipe.PipeNetworkOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Objects;

public class NodeLevel {
	private final Level level;
	private final PipeNetworkOperator network = new PipeNetworkOperator(this);

	/**
	 * DO NOT initialize, use {@link get} instead.
	 *
	 * @see get
	 */
	public NodeLevel(final Level level) {
		this.level = level;
	}

	public static NodeLevel get(final Level level) {
		return ((ILevelAccessor)(level)).vsch$getNodeLevel();
	}

	public final Level getLevel() {
		return this.level;
	}

	public final PipeNetworkOperator getNetwork() {
		return this.network;
	}

	protected NodeGetter getNodeChunk(final int x, final int z) {
		final LevelChunk chunk = this.level.getChunk(x, z);
		return chunk instanceof NodeGetter getter ? getter : null;
	}

	public PipeNode getNode(final NodePos pos) {
		final BlockPos blockPos = pos.blockPos();
		final int x = blockPos.getX(), y = blockPos.getY(), z = blockPos.getZ();
		final NodeGetter getter = this.getNodeChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
		if (getter == null) {
			return null;
		}
		return getter.getNode(
			SectionPos.sectionRelative(x),
			y,
			SectionPos.sectionRelative(z),
			pos.uniqueIndex()
		);
	}

	public List<PipeNode> getNodesOn(final BlockPos blockPos) {
		return NodePos.streamNodePosOn(blockPos).map(this::getNode)
			.filter(Objects::nonNull)
			.toList();
	}

	public void setNode(final NodePos pos, final PipeNode node) {
		if (node != null && !node.getPos().equals(pos)) {
			throw new IllegalArgumentException("Node position not match");
		}
		final BlockPos blockPos = pos.blockPos();
		final int x = blockPos.getX(), y = blockPos.getY(), z = blockPos.getZ();
		final NodeGetter getter = this.getNodeChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
		if (getter == null) {
			return;
		}
		final PipeNode oldNode = getter.setNode(
			SectionPos.sectionRelative(x),
			y,
			SectionPos.sectionRelative(z),
			pos.uniqueIndex(),
			node
		);
		if (oldNode != null) {
			this.network.onNodeRemove(oldNode);
		}
		if (node != null) {
			this.network.onNodeJoin(node);
		}
		if (this.level instanceof ServerLevel serverLevel) {
			VSCHNetwork.sendToTracking(PipeNodeUpdateS2C.fromNode(pos, node), serverLevel, blockPos);
		}
	}

	public NodePort getNodePort(final BlockPos blockPos, final RelativeNodePos pos) {
		final BlockEntity be = this.level.getBlockEntity(blockPos);
		if (be == null) {
			return null;
		}
		final INodePortProvider provider = be.getCapability(VSCHCapabilities.PORT_PROVIDER).orElse(null);
		if (provider == null) {
			return null;
		}
		return provider.getNodePort(pos);
	}
}
