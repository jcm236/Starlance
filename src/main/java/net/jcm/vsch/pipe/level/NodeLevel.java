package net.jcm.vsch.pipe.level;

import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

public class NodeLevel {
	private final Level level;

	public NodeLevel(final Level level) {
		this.level = level;
	}
 
	public final Level getLevel() {
		return this.level;
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

	public List<Pair<NodePos, PipeNode>> getNodes(final BlockPos pos) {
		final ArrayList<Pair<NodePos, PipeNode>> nodes = new ArrayList<>();
		// TODO
		nodes.trimToSize();
		return nodes;
	}

	public void setNode(final NodePos pos, final PipeNode node) {
		final BlockPos blockPos = pos.blockPos();
		final int x = blockPos.getX(), y = blockPos.getY(), z = blockPos.getZ();
		final NodeGetter getter = this.getNodeChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
		if (getter == null) {
			return;
		}
		getter.setNode(
			SectionPos.sectionRelative(x),
			y,
			SectionPos.sectionRelative(z),
			pos.uniqueIndex(),
			node
		);
	}
}
