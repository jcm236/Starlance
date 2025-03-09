package net.jcm.vsch.blocks.custom.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.jcm.vsch.blocks.entity.laser.LaserDetectProcessorBlockEntity;

public class LaserDetectProcessorBlock extends LaserCannonBlock<LaserDetectProcessorBlockEntity> {
	public LaserDetectProcessorBlock(BlockBehaviour.Properties properties) {
		super(properties, LaserDetectProcessorBlockEntity::new);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LaserDetectProcessorBlockEntity be ? be.getAnalogOutput() : 0;
	}
}
