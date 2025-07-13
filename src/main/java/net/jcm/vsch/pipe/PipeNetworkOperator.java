package net.jcm.vsch.pipe;

import net.jcm.vsch.accessor.IChunkMapAccessor;
import net.jcm.vsch.api.pipe.FlowDirection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.api.pipe.capability.NodeEnergyPort;
import net.jcm.vsch.api.pipe.capability.NodeFluidPort;
import net.jcm.vsch.api.pipe.capability.NodePort;
import net.jcm.vsch.pipe.level.NodeGetter;
import net.jcm.vsch.pipe.level.NodeLevel;
import net.jcm.vsch.util.Pair;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PipeNetworkOperator {
	private static final Object ENERGY_FLOW_ID = new Object();

	private final NodeLevel level;
	private final Set<NodePos> conflicted = new HashSet<>();
	private int tick = 0;

	public PipeNetworkOperator(final NodeLevel level) {
		this.level = level;
	}

	public void onNodeRemove(final PipeNode node) {
		final NodePos nodePos = node.getPos();
		for (final NodePos otherPos : node.relation.connections.keySet()) {
			final PipeNode other = this.level.getNode(otherPos);
			if (other != null) {
				other.relation.connections.remove(nodePos);
			}
		}
	}

	public void onNodeJoin(final PipeNode node) {
		final NodePos pos = node.getPos();
		pos.streamPossibleToConnect()
			.map(this.level::getNode)
			.filter(Objects::nonNull)
			.forEach((other) -> {
				final NodePos otherPos = other.getPos();
				final Direction[] connectPath = pos.connectPathTo(otherPos);
				final Direction nodeOutDir = connectPath[0];
				final Direction otherOutDir = connectPath[connectPath.length - 1].getOpposite();
				if (!node.canConnect(nodeOutDir) || !other.canConnect(otherOutDir)) {
					return;
				}
				this.connectNodes(node, nodeOutDir, other, otherOutDir);
			});
	}

	public void onTick() {
		this.tick++;
		final int tick = this.tick;
		for (final PipeNode node : (Iterable<PipeNode>)(this.streamServerNodes()::iterator)) {
			if (!node.relation.checkAndUpdateTick(tick)) {
				continue;
			}
			final NodePos nodePos = node.getPos();
			for (final NodePort port : node.relation.getPorts(this.level, nodePos)) {
				if (!port.getFlowDirection().canFlowOut()) {
					continue;
				}
				if (port instanceof NodeFluidPort fluidPort) {
					this.streamFluidFrom(nodePos, fluidPort);
				} else if (port instanceof NodeEnergyPort energyPort) {
					// TODO
				}
			}
		}
		this.conflicted.forEach(this.level::breakNode);
		this.conflicted.clear();
	}

	public Stream<PipeNode> streamServerNodes() {
		if (!(this.level.getLevel() instanceof ServerLevel serverLevel)) {
			return Stream.empty();
		}
		return StreamSupport.stream(((IChunkMapAccessor)(serverLevel.getChunkSource().chunkMap)).vsch$getChunks().spliterator(), false)
			.map(ChunkHolder::getTickingChunk)
			.filter(Objects::nonNull)
			.map(NodeGetter.class::cast)
			.flatMap(NodeGetter::streamNodes);
	}

	public Set<NodePos> getConnections(final PipeNode node) {
		return Collections.unmodifiableSet(node.relation.connections.keySet());
	}

	public boolean connectNodes(final PipeNode node1, final Direction dirToNode2, final PipeNode node2, final Direction dirToNode1) {
		final boolean updated1 = node1.relation.connections.put(node2.getPos(), dirToNode2) != dirToNode2;
		final boolean updated2 = node2.relation.connections.put(node1.getPos(), dirToNode1) != dirToNode1;
		return updated1 || updated2;
	}

	public boolean disconnectNodes(final PipeNode node1, final PipeNode node2) {
		final boolean removed1 = node1.relation.connections.remove(node2.getPos()) != null;
		final boolean removed2 = node2.relation.connections.remove(node1.getPos()) != null;
		return removed1 || removed2;
	}

	private void streamFluidFrom(final NodePos from, final NodeFluidPort fromPort) {
		final double r = 2.0 / 16;
		final double constQ = Math.PI * r * r * r * r / 8;

		final double fromPressure = fromPort.getPressure();
		if (fromPressure <= 0) {
			return;
		}
		final FluidStack stack = fromPort.peekFluid();
		if (stack.isEmpty()) {
			return;
		}
		final int maxAmount = stack.getAmount();
		final int viscosity = stack.getFluid().getFluidType().getViscosity();

		double totalFlowRate = 0;
		final List<Pair.RefDouble<NodeFluidPort>> pendingOut = new ArrayList<>();
		final Set<NodePos> streamed = new HashSet<>();
		final Queue<Pair.RefDouble<NodePos>> queue = new ArrayDeque<>();
		streamed.add(from);
		queue.add(new Pair.RefDouble<>(from, 0));
		while (true) {
			final Pair.RefDouble<NodePos> posDist = queue.poll();
			if (posDist == null) {
				break;
			}
			final NodePos pos = posDist.left();
			final double dist = posDist.right();
			final PipeNode node = this.level.getNode(pos);
			if (!node.relation.setStreaming(stack.getFluid())) {
				this.conflicted.add(pos);
				continue;
			}

			for (final NodePort port : node.relation.getPorts(this.level, pos)) {
				if (!(port instanceof NodeFluidPort fluidPort)) {
					continue;
				}
				final double pressureDiff = fromPressure - fluidPort.getPressure();
				if (!fluidPort.getFlowDirection().canFlowIn() || pressureDiff <= 0) {
					continue;
				}
				if (fluidPort.pushFluid(stack, true) <= 0) {
					continue;
				}
				final double vd = viscosity * dist;
				final double flowRate = vd == 0 ? maxAmount : Math.min(maxAmount, constQ * pressureDiff / vd);
				totalFlowRate += flowRate;
				pendingOut.add(new Pair.RefDouble<>(fluidPort, flowRate));
			}

			for (final Map.Entry<NodePos, Direction> entry : node.relation.connections.entrySet()) {
				final NodePos flowing = entry.getKey();
				if (streamed.contains(flowing)) {
					continue;
				}
				final PipeNode flowingNode = this.level.getNode(flowing);
				final Direction outDir = entry.getValue();
				if (flowingNode == null || !flowingNode.getFlowDirection(outDir).canFlowOut()) {
					continue;
				}
				streamed.add(flowing);
				queue.add(new Pair.RefDouble<>(flowing, dist + from.manhattanDistTo(flowing)));
			}
		}

		final double flowRateScale = totalFlowRate > maxAmount ? maxAmount / totalFlowRate : 1;

		for (final Pair.RefDouble<NodeFluidPort> portRate : pendingOut) {
			final NodeFluidPort fluidPort = portRate.left();
			final int flowRate = (int) (portRate.right() * flowRateScale);
			final FluidStack pulled = fromPort.pullFluid(flowRate, true);
			final int pushed = fluidPort.pushFluid(pulled, false);
			fromPort.pullFluid(pushed, false);
		}
	}

	public static final class RelationHolder {
		private final Map<NodePos, Direction> connections = new HashMap<>();
		private int lastTick = 0;
		private List<NodePort> portsCache = null;
		private Object recentlyStreamed = null;

		private boolean checkAndUpdateTick(final int tick) {
			if (this.lastTick == tick) {
				return false;
			}
			this.lastTick = tick;
			this.portsCache = null;
			this.recentlyStreamed = null;
			return true;
		}

		private void resetCache() {
			this.portsCache = null;
		}

		private List<NodePort> getPorts(final NodeLevel level, final NodePos pos) {
			if (this.portsCache == null) {
				this.portsCache = pos.streamTouchingBlocks(level.getLevel())
					.map((blockPos) -> level.getNodePort(blockPos, pos.asRelative(blockPos)))
					.filter(LazyOptional::isPresent)
					.map((lazyPort) -> lazyPort.orElseThrow(IllegalStateException::new))
					.filter((port) -> port.getFlowDirection() != FlowDirection.NONE)
					.toList();
			}
			return this.portsCache;
		}

		private boolean setStreaming(final Object id) {
			if (this.recentlyStreamed != null && !this.recentlyStreamed.equals(id)) {
				return false;
			}
			this.recentlyStreamed = id;
			return true;
		}
	}
}
