package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.INodeLevelChunkSection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.pipe.level.NodeGetter;

import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public abstract class MixinChunkAccess implements BlockGetter, NodeGetter {
	@Unique
	private static final PipeNode[] EMPTY_NODES = new PipeNode[NodePos.UNIQUE_INDEX_BOUND];

	@Shadow
	public abstract LevelChunkSection[] getSections();

	@Shadow
	public abstract LevelChunkSection getSection(int index);

	@Shadow
	public abstract boolean isUnsaved();

	@Shadow
	public abstract void setUnsaved(boolean value);

	@Unique
	private INodeLevelChunkSection getNodeSectionAtBlock(final int y) {
		if (y < this.getMinBuildHeight() || y >= this.getMaxBuildHeight()) {
			return null;
		}
		return this.getSection(this.getSectionIndex(y)) instanceof INodeLevelChunkSection nodeSection ? nodeSection : null;
	}

	@Override
	public PipeNode getNode(final int x, final int y, final int z, final int index) {
		final INodeLevelChunkSection nodeSection = this.getNodeSectionAtBlock(y);
		if (nodeSection == null) {
			return null;
		}
		return nodeSection.vsch$getNode(x, SectionPos.sectionRelative(y), z, index);
	}

	@Override
	public PipeNode[] getNodes(final int x, final int y, final int z) {
		final INodeLevelChunkSection nodeSection = this.getNodeSectionAtBlock(y);
		if (nodeSection == null) {
			return null;
		}
		final PipeNode[] nodes = nodeSection.vsch$getNodes(x, SectionPos.sectionRelative(y), z);
		return nodes == null ? EMPTY_NODES : nodes;
	}

	@Override
	public void setNode(final int x, final int y, final int z, final int index, final PipeNode node) {
		final INodeLevelChunkSection nodeSection = this.getNodeSectionAtBlock(y);
		if (nodeSection == null) {
			return;
		}
		nodeSection.vsch$setNode(x, SectionPos.sectionRelative(y), z, index, node);
		this.setNodesUnsaved();
	}

	@Override
	public boolean hasAnyNode() {
		for (final LevelChunkSection section : this.getSections()) {
			if (section instanceof INodeLevelChunkSection nodeSection && nodeSection.vsch$hasAnyNode()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void writeNodes(final FriendlyByteBuf buf) {
		for (final LevelChunkSection section : this.getSections()) {
			((INodeLevelChunkSection) (section)).vsch$writeNodes(buf);
		}
	}

	@Override
	public void readNodes(final FriendlyByteBuf buf) {
		for (final LevelChunkSection section : this.getSections()) {
			((INodeLevelChunkSection) (section)).vsch$readNodes(buf);
		}
	}

	@Override
	public boolean isNodesUnsaved() {
		return this.isUnsaved();
	}

	@Override
	public void setNodesUnsaved() {
		this.setUnsaved(true);
	}
}
