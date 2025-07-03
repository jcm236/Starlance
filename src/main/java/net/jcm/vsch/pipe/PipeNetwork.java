package net.jcm.vsch.pipe;

import net.jcm.vsch.api.pipe.NodePos;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PipeNetwork {
	private final Map<NodePos, RelationHolder> nodes = new HashMap<>();

	public Set<NodePos> getNodes() {
		return this.nodes.keySet();
	}

	public boolean hasNode(final NodePos node) {
		return this.nodes.containsKey(node);
	}

	public boolean addNode(final NodePos node) {
		if (this.nodes.containsKey(node)) {
			return false;
		}
		this.nodes.put(node, new RelationHolder());
		return true;
	}

	public boolean removeNode(final NodePos node) {
		final RelationHolder holder = this.nodes.remove(node);
		if (holder == null) {
			return false;
		}
		for (final NodePos other : holder.connections) {
			final RelationHolder otherHolder = this.nodes.get(other);
			otherHolder.connections.remove(node);
			otherHolder.flows.remove(node);
		}
		return true;
	}

	public Set<NodePos> getConnections(final NodePos node) {
		return Collections.unmodifiableSet(this.nodes.get(node).connections);
	}

	public boolean connectNodes(final NodePos node1, final NodePos node2) {
		return this.nodes.get(node1).connections.add(node2) && this.nodes.get(node2).connections.add(node1);
	}

	public boolean disconnectNodes(final NodePos node1, final NodePos node2) {
		if (!this.nodes.get(node1).connections.remove(node2)) {
			return false;
		}
		this.nodes.get(node2).connections.remove(node1);
		return true;
	}

	public Set<NodePos> getFlows(final NodePos node) {
		return Collections.unmodifiableSet(this.nodes.get(node).flows);
	}

	public boolean canFlow(final NodePos from, final NodePos to) {
		return this.nodes.get(from).flows.contains(to);
	}

	public boolean addFlow(final NodePos from, final NodePos to) {
		return this.nodes.get(from).flows.add(to);
	}

	public boolean removeFlows(final NodePos node) {
		final RelationHolder holder = this.nodes.get(node);
		if (holder == null) {
			return false;
		}
		boolean ok = !holder.flows.isEmpty();
		holder.flows.clear();
		for (final NodePos other : holder.connections) {
			ok |= this.nodes.get(other).flows.remove(node);
		}
		return ok;
	}

	private static final class RelationHolder {
		final Set<NodePos> connections = new HashSet<>();
		final Set<NodePos> flows = new HashSet<>();
	}
}
