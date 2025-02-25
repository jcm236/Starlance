package net.jcm.vsch.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MagnetBlockEntity extends BlockEntity {

	public MagnetBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.MAGNET_BLOCK_ENTITY.get(), pos, state);
	}

	public void tick() {

	}
}
