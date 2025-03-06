package net.jcm.vsch.blocks.custom.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.jcm.vsch.blocks.entity.laser.LaserReceiverBlockEntity;

import java.util.function.BiFunction;

public class LaserReceiverBlock<T extends LaserReceiverBlockEntity> extends LaserCannonBlock<T> {
	public LaserReceiverBlock(Properties properties, BiFunction<BlockPos, BlockState, T> blockEntityFactory) {
		super(properties, blockEntityFactory);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LaserReceiverBlockEntity be ? be.getAnalogOutput() : 0;
	}
}
