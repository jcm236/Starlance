package net.jcm.vsch.pipe;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PipeNetwork {
	private final Map<NodePos, Set<NodePos>> nodeConnects = new HashMap<>();
	private final Map<NodePos, Set<NodePos>> nodeFlows = new HashMap<>();

	public Set<NodePos> getNodes() {
		return this.nodeConnects.keySet();
	}

	public boolean hasNode(final NodePos node) {
		return this.nodeConnects.containsKey(node);
	}

	public boolean addNode(final NodePos node) {
		if (this.nodeConnects.containsKey(node)) {
			return false;
		}
		this.nodeConnects.put(node, new HashSet<>());
		this.nodeFlows.put(node, new HashSet<>());
		return true;
	}

	public boolean removeNode(final NodePos node) {
		final Set<NodePos> connected = this.nodeConnects.remove(node);
		if (connected == null) {
			return false;
		}
		this.nodeFlows.remove(node);
		for (final NodePos other : connected) {
			this.nodeConnects.get(other).remove(node);
			this.nodeFlows.get(other).remove(node);
		}
		return true;
	}

	public Set<NodePos> getConnections(final NodePos node) {
		return Collections.unmodifiableSet(this.nodeConnects.get(node));
	}

	public boolean connectNodes(final NodePos node1, final NodePos node2) {
		return this.nodeConnects.get(node1).add(node2) && this.nodeConnects.get(node2).add(node1);
	}

	public boolean disconnectNodes(final NodePos node1, final NodePos node2) {
		if (!this.nodeConnects.get(node1).remove(node2)) {
			return false;
		}
		this.nodeConnects.get(node2).remove(node1);
		this.nodeFlows.get(node1).remove(node2);
		this.nodeFlows.get(node2).remove(node1);
		return true;
	}

	public Set<NodePos> getFlows(final NodePos node) {
		return Collections.unmodifiableSet(this.nodeFlows.get(node));
	}

	public boolean canFlow(final NodePos from, final NodePos to) {
		return this.nodeFlows.get(from).contains(to);
	}

	public boolean addFlow(final NodePos from, final NodePos to) {
		return this.nodeFlows.get(from).add(to);
	}

	public boolean removeFlows(final NodePos node) {
		final Set<NodePos> connected = this.nodeConnects.get(node);
		if (connected == null) {
			return false;
		}
		final Set<NodePos> flowing = this.nodeFlows.get(node);
		boolean ok = !flowing.isEmpty();
		flowing.clear();
		for (final NodePos other : connected) {
			ok |= this.nodeFlows.get(other).remove(node);
		}
		return ok;
	}
}
