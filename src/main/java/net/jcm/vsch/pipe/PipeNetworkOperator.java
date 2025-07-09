package net.jcm.vsch.pipe;

import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.pipe.level.NodeLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PipeNetworkOperator {
	private final NodeLevel level;
	private final Set<NodePos> conflicted = new HashSet<>();

	public PipeNetworkOperator(final NodeLevel level) {
		this.level = level;
	}

	public void onNodeRemove(final PipeNode node) {
		final NodePos nodePos = node.getPos();
		for (final NodePos otherPos : node.relation.connections) {
			final PipeNode other = this.level.getNode(otherPos);
			if (other != null) {
				other.relation.connections.remove(nodePos);
				other.relation.flows.remove(nodePos);
			}
		}
	}

	public void onNodeJoin(final PipeNode node) {
		//
	}

	public void onTick() {
		//
	}

	public Set<NodePos> getConnections(final PipeNode node) {
		return Collections.unmodifiableSet(node.relation.connections);
	}

	public boolean connectNodes(final PipeNode node1, final PipeNode node2) {
		return node1.relation.connections.add(node2.getPos()) && node2.relation.connections.add(node1.getPos());
	}

	public boolean disconnectNodes(final PipeNode node1, final PipeNode node2) {
		if (!node1.relation.connections.remove(node2.getPos())) {
			return false;
		}
		node2.relation.connections.remove(node1.getPos());
		return true;
	}

	public Set<NodePos> getFlows(final PipeNode node) {
		return Collections.unmodifiableSet(node.relation.flows);
	}

	public boolean canFlow(final PipeNode from, final NodePos to) {
		return from.relation.flows.contains(to);
	}

	public boolean addFlow(final PipeNode from, final NodePos to) {
		return from.relation.flows.add(to);
	}

	public boolean removeFlows(final PipeNode node) {
		boolean ok = !node.relation.flows.isEmpty();
		node.relation.flows.clear();
		for (final NodePos otherPos : node.relation.connections) {
			final PipeNode other = this.level.getNode(otherPos);
			if (other != null) {
				ok |= other.relation.flows.remove(node);
			}
		}
		return ok;
	}

	private void streamFrom(final NodePos from, final Object id) {
		final Set<NodePos> streamed = new HashSet<>();
		final Queue<NodePos> queue = new ArrayDeque<>();
		streamed.add(from);
		queue.add(from);
		while (true) {
			final NodePos pos = queue.poll();
			if (pos == null) {
				break;
			}
			final PipeNode node = this.level.getNode(pos);
			if (!node.relation.setStreaming(id)) {
				this.conflicted.add(pos);
				return;
			}
			pos.streamTouchingBlocks()
				.map((blockPos) -> this.level.getNodePort(blockPos, pos.asRelative(blockPos)))
				.filter(Objects::nonNull)
				.forEach((port) -> {
					//
				});
			for (final NodePos flowing : node.relation.flows) {
				if (streamed.add(flowing)) {
					queue.add(flowing);
				}
			}
		}
	}

	public static final class RelationHolder {
		private final Set<NodePos> connections = new HashSet<>();
		private final Set<NodePos> flows = new HashSet<>();
		private boolean wasOutput = false;
		private Object recentlyStreamed = null;

		public boolean setStreaming(final Object id) {
			if (this.recentlyStreamed != null && !this.recentlyStreamed.equals(id)) {
				return false;
			}
			this.recentlyStreamed = id;
			return true;
		}
	}
}
