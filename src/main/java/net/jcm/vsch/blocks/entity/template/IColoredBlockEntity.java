package net.jcm.vsch.blocks.entity.template;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public interface IColoredBlockEntity {
	int[] getColor();

	void setColor(int r, int g, int b);

	static InteractionResult onUse(
		BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand,
		BlockHitResult hit,
		boolean consume
	) {
		if (!(level instanceof ServerLevel)) {
			return InteractionResult.PASS;
		}

		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		final ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof DyeItem item)) {
			return InteractionResult.PASS;
		}

		if (!(level.getBlockEntity(pos) instanceof IColoredBlockEntity be)) {
			return InteractionResult.PASS;
		}

		final DyeColor dyeColor = item.getDyeColor();
		if (consume && !player.isCreative()) {
			stack.shrink(1);
		}
		final int color = dyeColor.getTextColor();
		be.setColor((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff);
		return InteractionResult.SUCCESS;
	}
}
