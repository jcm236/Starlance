package net.jcm.vsch.blocks.rocketassembler;

import net.jcm.vsch.blocks.entity.VSCHBlockEntities;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.config.VSCHServerConfig;
import com.github.litermc.vtil.block.AbstractAssemblerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import dan200.computercraft.shared.Capabilities;

import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;

public class RocketAssemblerBlockEntity extends AbstractAssemblerBlockEntity implements ParticleBlockEntity {
	private static final int MAX_SIZE = 256 * 16;

	private boolean triggering = false;
	private volatile AssembleResult assembleResult = AssembleResult.SUCCESS;
	private int energyStored = 0;
	private final int energyConsumption = VSCHServerConfig.ASSEMBLER_ENERGY_CONSUMPTION.get();
	private final AABBi box = new AABBi();

	final IEnergyStorage energyStorage = new EnergyStorage();
	private final LazyOptional<IEnergyStorage> lazyEnergyStorage = LazyOptional.of(() -> this.energyStorage);
	private final LazyOptional<Object> lazyPeripheral = LazyOptional.of(() -> {
		final RocketAssemblerPeripheral peripheral = new RocketAssemblerPeripheral(this);
		this.assembleFinishCallback = peripheral::onAssembleFinish;
		return peripheral;
	});
	private Runnable assembleFinishCallback = null;

	public RocketAssemblerBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.ROCKET_ASSEMBLER_BLOCK_ENTITY.get(), pos, state);
	}

	public boolean isAssembleSuccessed() {
		return this.assembleResult.isSuccess();
	}

	public int getEnergyConsumption() {
		return this.energyConsumption;
	}

	public AssembleResult getAssembleResult() {
		return this.assembleResult;
	}

	private void setAssembleResult(final AssembleResult result) {
		if (this.assembleResult == result) {
			return;
		}
		this.assembleResult = result;
		this.setChanged();
		this.getLevel().setBlock(this.getBlockPos(), this.getBlockState().setValue(RocketAssemblerBlock.LED, result.getLED()), Block.UPDATE_ALL);
	}

	@Override
	public int getMaxAssembleBlocks() {
		return VSCHServerConfig.MAX_ASSEMBLE_BLOCKS.get();
	}

	@Override
	public void load(final CompoundTag data) {
		this.energyStored = data.getInt("EnergyStored");
		this.triggering = data.getBoolean("Powered");
		try {
			this.assembleResult = AssembleResult.valueOf(data.getString("AssembleResult"));
		} catch(IllegalArgumentException e) {
			this.assembleResult = AssembleResult.SUCCESS;
		}
	}

	@Override
	public void saveAdditional(final CompoundTag data) {
		data.putInt("EnergyStored", this.energyStored);
		data.putBoolean("Powered", this.triggering);
		this.saveShared(data);
	}

	public void saveShared(final CompoundTag data) {
		data.putString("AssembleResult", this.assembleResult.toString());
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		this.saveShared(data);
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void neighborChanged(Block neighbor, BlockPos neighborPos, boolean moving) {
		final Level level = this.getLevel();
		final BlockPos pos = this.getBlockPos();
		final boolean shouldTrigger = Direction.stream()
			.filter(dir -> dir != this.getBlockState().getValue(DirectionalBlock.FACING))
			.anyMatch(dir -> level.getSignal(pos.relative(dir), dir) > 0);
		if (this.triggering == shouldTrigger) {
			return;
		}
		this.triggering = shouldTrigger;
		if (shouldTrigger) {
			this.startAssemble(null);
		}
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction direction) {
		if (cap == ForgeCapabilities.ENERGY) {
			return this.lazyEnergyStorage.cast();
		}
		if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			return this.lazyPeripheral.cast();
		}
		return super.getCapability(cap, direction);
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		this.serverTick();
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {
		//
	}

	@Override
	public boolean startAssemble(final String slug) {
		if (this.isAssembling()) {
			return false;
		}
		if (this.energyStored < this.energyConsumption) {
			this.finishAssemble(AssembleResult.NO_ENERGY);
			return true;
		}
		return super.startAssemble(slug);
	}

	@Override
	protected void setAssembling(final boolean assembling) {
		super.setAssembling(assembling);
		if (assembling) {
			this.setAssembleResult(AssembleResult.WORKING);
		}
	}

	protected void finishAssemble(final AssembleResult result) {
		this.setAssembleResult(result);
		this.finishAssemble();
	}

	@Override
	protected void finishAssemble() {
		super.finishAssemble();
		if (this.assembleFinishCallback != null) {
			this.assembleFinishCallback.run();
		}
	}

	@Override
	protected void finishAssembleAsSuccess() {
		this.finishAssemble(AssembleResult.SUCCESS);
	}

	@Override
	protected void finishAssembleAsAssembleSelf() {
		this.finishAssemble(AssembleResult.ASSEMBLING_SELF);
	}

	@Override
	protected void finishAssembleAsTooManyBlocks() {
		this.finishAssemble(AssembleResult.TOO_MANY_BLOCKS);
	}

	@Override
	protected void finishAssembleAsNoBlockToAssemble() {
		this.finishAssemble(AssembleResult.NO_BLOCK);
	}

	@Override
	protected void finishAssembleAsConflicts() {
		this.finishAssemble(AssembleResult.OTHER_ASSEMBLING);
	}

	@Override
	protected void addAssemblingBlock(final BlockPos pos) {
		final Level level = this.getLevel();
		if (!level.hasChunkAt(pos.getX(), pos.getZ())) {
			this.finishAssemble(AssembleResult.CHUNK_UNLOADED);
			return;
		}
		final BlockState block = level.getBlockState(pos);
		if (block.isAir()) {
			return;
		}
		if (!this.canAssembleBlock(block)) {
			this.finishAssemble(AssembleResult.UNABLE_ASSEMBLE);
			return;
		}
		this.box.union(pos.getX(), pos.getY(), pos.getZ());
		if (this.box.lengthX() > MAX_SIZE || this.box.lengthY() > MAX_SIZE || this.box.lengthZ() > MAX_SIZE) {
			this.finishAssemble(AssembleResult.SIZE_OVERFLOW);
			return;
		}
		super.addAssemblingBlock(pos);
	}

	protected boolean canAssembleBlock(final BlockState state) {
		final Block block = state.getBlock();
		final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
		if (VSCHServerConfig.getAssembleBlacklistSet().contains(blockId)) {
			return false;
		}
		return true;
	}

	private class EnergyStorage implements IEnergyStorage {
		@Override
		public int receiveEnergy(final int maxReceive, final boolean simulate) {
			final int storedEnergy = this.getEnergyStored();
			final int newEnergy = Math.min(storedEnergy + maxReceive, this.getMaxEnergyStored());
			if (!simulate) {
				RocketAssemblerBlockEntity.this.energyStored = newEnergy;
				RocketAssemblerBlockEntity.this.setChanged();
			}
			return newEnergy - storedEnergy;
		}

		@Override
		public int extractEnergy(final int maxExtract, final boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return RocketAssemblerBlockEntity.this.energyStored;
		}

		@Override
		public int getMaxEnergyStored() {
			return RocketAssemblerBlockEntity.this.getEnergyConsumption();
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	}
}
