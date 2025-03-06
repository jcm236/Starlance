package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface ILaserAttachment {
	/**
	 * beforeProcessLaser will be invoked when a laser finds a block.
	 * You can invoke {@link LaserContext#cancel} to cancel futher processing,
	 * and {@link LaserContext#getHitResult} will return the HitResult.
	 *
	 * @param ctx   The {@link LaserContext} 
	 * @param state The hitting block's {@link BlockState}
	 * @param pos   The hitting block's position
	 *
	 * @see LaserContext.cancel
	 * @see LaserContext.getHitResult
	 */
	default void beforeProcessLaser(LaserContext ctx, BlockState state, BlockPos pos) {}

	/**
	 * afterProcessLaser will be invoked after a laser processed a block.
	 *
	 * @param ctx      The {@link LaserContext} 
	 * @param oldState The block's {@link BlockState} before laser processed
	 * @param pos      The block's position
	 */
	default void afterProcessLaser(LaserContext ctx, BlockState oldState, BlockPos pos) {}
}
