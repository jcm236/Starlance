package net.jcm.vsch.pipe;

import net.jcm.vsch.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class NodeLevel {
	private Level level;

	public NodeLevel(final Level level) {
		this.level = level;
	}
 
	public final Level getLevel() {
		return this.level;
	}

	public PipeNode getNode(final NodePos pos) {
		// TODO
		return null;
	}

	public List<Pair<NodePos, PipeNode>> getNodes(final BlockPos pos) {
		final ArrayList<Pair<NodePos, PipeNode>> nodes = new ArrayList<>();
		// TODO
		nodes.trimToSize();
		return nodes;
	}

	public void setNode(final NodePos pos, final PipeNode node) {
		// TODO
	}
}
