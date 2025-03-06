package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface ILaserAttachment {
	default void beforeProcessLaser(LaserContext ctx, BlockState state, BlockPos pos) {}
}
