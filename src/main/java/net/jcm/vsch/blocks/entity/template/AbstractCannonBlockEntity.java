package net.jcm.vsch.blocks.entity.template;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;

import java.util.EnumMap;

public abstract class AbstractCannonBlockEntity extends AbstractCCBlockEntity implements ParticleBlockEntity {
	protected final Direction facing;
	protected final EnumMap<Direction, AbstractCannonBlockEntity> neighbors = new EnumMap<>(Direction.class);
	protected int redstone = 0;

	protected AbstractCannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		this.facing = state.getValue(DirectionalBlock.FACING);
	}

	@Override
	public void load(CompoundTag data) {
		super.load(data);
		this.redstone = data.getByte("Redstone");
	}

	@Override
	public void saveAdditional(CompoundTag data) {
		super.saveAdditional(data);
		data.putByte("Redstone", (byte) (this.redstone));
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void markUpdated() {
		this.setChanged();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
	}

	public void neighborChanged(Block block, BlockPos neighborPos, boolean moving) {
		if (!(this.getLevel() instanceof ServerLevel level)) {
			return;
		}
		final BlockPos pos = this.getBlockPos();
		final int newRedstone = level.getBestNeighborSignal(pos);
		if (newRedstone != this.redstone) {
			final int oldSignal = this.redstone;
			this.redstone = newRedstone;
			this.signalChanged(oldSignal, newRedstone);
		}
		final Direction dir = Direction.fromDelta(neighborPos.getX() - pos.getX(), neighborPos.getY() - pos.getY(), neighborPos.getZ() - pos.getZ());
		final BlockEntity be = level.getBlockEntity(neighborPos);
		final AbstractCannonBlockEntity cbe = (be instanceof AbstractCannonBlockEntity acbe && this.isValidPart(dir, acbe)) ? acbe : null;
		if (this.neighbors.get(dir) != cbe) {
			this.neighbors.put(dir, cbe);
			this.partChanged(dir, cbe);
		}
	}

	public AbstractCannonBlockEntity getNeighbor(Direction dir) {
		return this.neighbors.get(dir);
	}

	public abstract boolean isValidPart(Direction dir, AbstractCannonBlockEntity be);

	public abstract void partChanged(Direction dir, AbstractCannonBlockEntity be);

	public void signalChanged(int oldSignal, int newSignal) {}
}
