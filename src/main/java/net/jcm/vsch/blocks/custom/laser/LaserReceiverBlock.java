package net.jcm.vsch.blocks.custom.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.jcm.vsch.blocks.entity.laser.LaserReceiverBlockEntity;

import java.util.function.BiFunction;

public class LaserReceiverBlock<T extends LaserReceiverBlockEntity> extends LaserCannonBlock<T> {
	public LaserReceiverBlock(BlockBehaviour.Properties properties, BiFunction<BlockPos, BlockState, T> blockEntityFactory) {
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

	@Override
	public InteractionResult use(
			BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand,
			BlockHitResult hit
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

		if (!(level.getBlockEntity(pos) instanceof LaserReceiverBlockEntity receiver)) {
			return InteractionResult.PASS;
		}

		final int color = item.getDyeColor().getTextColor();
		receiver.setColor((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff);
		return InteractionResult.SUCCESS;
	}
}
