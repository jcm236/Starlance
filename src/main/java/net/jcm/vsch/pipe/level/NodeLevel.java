package net.jcm.vsch.pipe.level;

import net.jcm.vsch.VSCHCapabilities;
import net.jcm.vsch.accessor.ILevelAccessor;
import net.jcm.vsch.api.pipe.FlowDirection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.api.pipe.RelativeNodePos;
import net.jcm.vsch.api.pipe.capability.INodePortProvider;
import net.jcm.vsch.api.pipe.capability.NodeEnergyPort;
import net.jcm.vsch.api.pipe.capability.NodeFluidPort;
import net.jcm.vsch.api.pipe.capability.NodePort;
import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.network.s2c.PipeNodeUpdateS2C;
import net.jcm.vsch.pipe.PipeNetworkOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Objects;
import java.util.stream.Stream;

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
		return ((ILevelAccessor)(level)).starlance$getNodeLevel();
	}

	public final Level getLevel() {
		return this.level;
	}

	public final PipeNetworkOperator getNetwork() {
		return this.network;
	}

	@Override
	public String toString() {
		return "<NodeLevel " + this.level + " " + this.level.dimension().location() + ">";
	}

	protected NodeGetter getNodeChunk(final int x, final int z) {
		final LevelChunk chunk = this.level.getChunkSource().getChunkNow(x, z);
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

	public Stream<PipeNode> streamNodesOn(final BlockPos blockPos) {
		return NodePos.streamNodePosOn(blockPos).map(this::getNode).filter(Objects::nonNull);
	}

	public PipeNode setNode(final NodePos pos, final PipeNode node) {
		if (node != null && !node.getPos().equals(pos)) {
			throw new IllegalArgumentException("Node position not match");
		}
		final BlockPos blockPos = pos.blockPos();
		final int x = blockPos.getX(), y = blockPos.getY(), z = blockPos.getZ();
		final NodeGetter getter = this.getNodeChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
		if (getter == null) {
			return null;
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
		return oldNode;
	}

	public void breakNode(final NodePos pos) {
		this.breakNode(pos, true);
	}

	public void breakNode(final NodePos pos, final boolean drop) {
		final PipeNode node = this.setNode(pos, null);
		if (node == null) {
			return;
		}
		// TODO: play sound
		if (!drop) {
			return;
		}
		final ItemStack stack = node.asItemStack();
		if (stack.isEmpty()) {
			return;
		}
		final Vec3 center = pos.getCenter();
		this.level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, stack));
	}

	public LazyOptional<NodePort> getNodePort(final BlockPos blockPos, final RelativeNodePos pos) {
		final BlockEntity be = this.level.getBlockEntity(blockPos);
		if (be == null) {
			return LazyOptional.empty();
		}
		final LazyOptional<INodePortProvider> lazyProvider = be.getCapability(VSCHCapabilities.PORT_PROVIDER);
		if (!lazyProvider.isPresent()) {
			return this.getNodePortFromBasicCapability(be);
		}
		final LazyOptional<NodePort> lazyPort = lazyProvider.lazyMap((provider) -> provider.getNodePort(pos));
		lazyProvider.addListener((lazyProviderAccess) -> lazyPort.invalidate());
		return lazyPort;
	}

	private LazyOptional<NodePort> getNodePortFromBasicCapability(final BlockEntity be) {
		final LazyOptional<IFluidHandler> fluidCap = be.getCapability(ForgeCapabilities.FLUID_HANDLER);
		final IFluidHandler fluidHandler = fluidCap.orElse(null);
		if (fluidHandler != null && fluidHandler.getTanks() == 1) {
			final LazyOptional<NodeFluidPort> lazyPort = fluidCap.lazyMap(NodeFluidPortImpl::new);
			fluidCap.addListener((fluidCapAccess) -> lazyPort.invalidate());
			return lazyPort.cast();
		}
		final LazyOptional<IEnergyStorage> energyCap = be.getCapability(ForgeCapabilities.ENERGY);
		final IEnergyStorage energyStorage = energyCap.orElse(null);
		if (energyStorage != null) {
			final LazyOptional<NodeEnergyPort> lazyPort = energyCap.lazyMap(NodeEnergyPortImpl::new);
			energyCap.addListener((energyCapAccess) -> lazyPort.invalidate());
			return lazyPort.cast();
		}
		return LazyOptional.empty();
	}

	private final class NodeFluidPortImpl implements NodeFluidPort {
		private final IFluidHandler fluidHandler;

		private NodeFluidPortImpl(final IFluidHandler fluidHandler) {
			this.fluidHandler = fluidHandler;
		}

		@Override
		public double getPressure() {
			final FluidStack stack = this.fluidHandler.getFluidInTank(0);
			if (stack.isEmpty()) {
				return 0;
			}
			final FluidType fluid = stack.getFluid().getFluidType();
			final double amount = stack.getAmount();
			final double cap = this.fluidHandler.getTankCapacity(0);
			// TODO: more realistic pressure
			return amount / 2.0;
		}

		@Override
		public FlowDirection getFlowDirection() {
			return this.fluidHandler.getFluidInTank(0).getAmount() < this.fluidHandler.getTankCapacity(0) ? FlowDirection.BOTH : FlowDirection.OUT;
		}

		@Override
		public FluidStack peekFluid() {
			return this.fluidHandler.getFluidInTank(0);
		}

		@Override
		public int pushFluid(final FluidStack stack, final boolean simulate) {
			return this.fluidHandler.fill(stack, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
		}

		@Override
		public FluidStack pullFluid(final int amount, final boolean simulate) {
			return this.fluidHandler.drain(amount, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
		}
	}

	private final class NodeEnergyPortImpl implements NodeEnergyPort {
		private final IEnergyStorage energyStorage;

		private NodeEnergyPortImpl(final IEnergyStorage energyStorage) {
			this.energyStorage = energyStorage;
		}

		@Override
		public double getPressure() {
			final double energy = this.energyStorage.getEnergyStored();
			if (energy == 0) {
				return 0;
			}
			final double maxEnergy = this.energyStorage.getMaxEnergyStored();
			return energy * energy / maxEnergy;
		}

		@Override
		public FlowDirection getFlowDirection() {
			return this.energyStorage.getEnergyStored() < this.energyStorage.getMaxEnergyStored() ? FlowDirection.BOTH : FlowDirection.OUT;
		}

		@Override
		public int pushEnergy(int amount, boolean simulate) {
			return this.energyStorage.receiveEnergy(amount, simulate);
		}

		@Override
		public int pullEnergy(int amount, boolean simulate) {
			return this.energyStorage.extractEnergy(amount, simulate);
		}
	}
}
