package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface ILaserAttachment {
	/**
	 * canPassThroughBlock will be invoked when a laser passing a block.
	 * You can invoke {@link LaserContext#cancel} to immediately cancel further processing.
	 *
	 * @param ctx   The {@link LaserContext} 
	 * @param state The {@link BlockState} of the hitting block
	 * @param level The level of the hitting block
	 * @param pos   The position of the hitting block
	 * @return {@link Boolean#TRUE} if the block is passable,
	 *     {@link Boolean#FALSE} if the block is not passable,
	 *     or {@code null} if unknown, which will invoke next checker.
	 *
	 * @see LaserContext.cancel
	 */
	default Boolean canPassThroughBlock(LaserContext ctx, BlockState state, BlockGetter level, BlockPos pos) {
		return null;
	}

	/**
	 * beforeProcessLaser will be invoked when a laser hits a block.
	 * You can invoke {@link LaserContext#cancel} to cancel further processing,
	 * and {@link LaserContext#getHitResult} will return the HitResult.
	 *
	 * @param ctx   The {@link LaserContext} 
	 * @param state The {@link BlockState} of the hitting block
	 * @param pos   The position of the hitting block
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
