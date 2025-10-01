package net.jcm.vsch.util;

import net.jcm.vsch.util.wapi.LevelData;
import net.jcm.vsch.config.VSCHServerConfig;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class EmptyChunkAccess extends LevelChunk {
	private static final LevelHeightAccessor EMPTY_LEVEL_HEIGHT_ACCESSOR = new LevelHeightAccessor() {
		@Override
		public int getHeight() {
			return 0;
		}

		@Override
		public int getMinBuildHeight() {
			return 0;
		}

		@Override
		public int getMinSection() {
			return 0;
		}

		@Override
		public int getMaxSection() {
			return 0;
		}

		@Override
		public boolean isOutsideBuildHeight(BlockPos pos) {
			return true;
		}

		@Override
		public boolean isOutsideBuildHeight(final int y) {
			return true;
		}
	};

	public EmptyChunkAccess(final Level level, final ChunkPos pos) {
		super(level, pos);
	}

	@Override
	public BlockEntity getBlockEntity(final BlockPos pos) {
		return null;
	}

	@Override
	public BlockState getBlockState(final BlockPos pos) {
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public FluidState getFluidState(final BlockPos pos) {
		return Fluids.EMPTY.defaultFluidState();
	}

	@Override
	public FluidState getFluidState(final int x, final int y, final int z) {
		return Fluids.EMPTY.defaultFluidState();
	}

	@Override	
	public BlockState setBlockState(final BlockPos pos, final BlockState state, final boolean isMoving) {
		return null;
	}

	@Override
	public void setBlockEntity(final BlockEntity be) {}

	@Override
	public void addEntity(final Entity entity) {}

	@Override
	public ChunkStatus getStatus() {
		return ChunkStatus.FULL;
	}

	@Override
	public void removeBlockEntity(final BlockPos pos) {}

	@Override
	public CompoundTag getBlockEntityNbtForSaving(final BlockPos pos) {
		return null;
	}

	@Override
	public TickContainerAccess<Block> getBlockTicks() {
		return BlackholeTickAccess.emptyContainer();
	}

	@Override
	public TickContainerAccess<Fluid> getFluidTicks() {
		return BlackholeTickAccess.emptyContainer();
	}

	@Override
	public ChunkAccess.TicksToSave getTicksForSerialization() {
		return null;
	}

	public static boolean shouldUseEmptyChunk(final Level level, final int x, final int z) {
		if (!VSCHServerConfig.ENABLE_EMPTY_SPACE_CHUNK.get()) {
			return false;
		}
		if (VSGameUtilsKt.isChunkInShipyard(level, x, z)) {
			return false;
		}
		return LevelData.get(level).isSpace();
	}
}
