package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.INodeLevelChunkSection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.pipe.level.NodeGetter;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk extends ChunkAccess implements BlockGetter, NodeGetter {
	@Unique
	private static final PipeNode[] EMPTY_NODES = new PipeNode[NodePos.UNIQUE_INDEX_BOUND];

	protected MixinLevelChunk() {
		super(null, null, null, null, 0, null, null);
	}

	@Shadow
	public abstract Level getLevel();

	@Unique
	private INodeLevelChunkSection getNodeSectionAtBlock(final int y) {
		if (this.isOutsideBuildHeight(y)) {
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
	public PipeNode setNode(final int x, final int y, final int z, final int index, final PipeNode node) {
		final INodeLevelChunkSection nodeSection = this.getNodeSectionAtBlock(y);
		if (nodeSection == null) {
			return null;
		}
		final PipeNode oldNode = nodeSection.vsch$setNode(x, SectionPos.sectionRelative(y), z, index, node);
		this.setNodesUnsaved();
		return oldNode;
	}

	@Override
	public Stream<PipeNode> streamNodes() {
		final LevelChunkSection[] sections = this.getSections();
		return IntStream.range(0, sections.length)
			.mapToObj((sectionIndex) -> {
				final LevelChunkSection section = sections[sectionIndex];
				if (!(section instanceof INodeLevelChunkSection nodeSection)) {
					return null;
				}
				if (!nodeSection.vsch$hasAnyNode()) {
					return null;
				}
				return IntStream.range(0, 16 * 16 * 16)
					.mapToObj((index) -> {
						final int x = index & 0xf, y = (index >> 8) & 0xf, z = (index >> 4) & 0xf;
						PipeNode[] nodes = nodeSection.vsch$getNodes(x, y, z);
						if (nodes == null) {
							return null;
						}
						return Arrays.stream(nodes).filter(Objects::nonNull);
					})
					.filter(Objects::nonNull)
					.flatMap(Function.identity());
			})
			.filter(Objects::nonNull)
			.flatMap(Function.identity());
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
		final NodeLevel level = NodeLevel.get(this.getLevel());
		final ChunkPos chunkPos = this.getPos();
		int i = 0;
		for (final LevelChunkSection section : this.getSections()) {
			((INodeLevelChunkSection) (section)).vsch$readNodes(level, SectionPos.of(chunkPos, this.getSectionYFromSectionIndex(i)), buf);
			i++;
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
