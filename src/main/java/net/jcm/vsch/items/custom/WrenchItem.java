package net.jcm.vsch.items.custom;

import net.jcm.vsch.blocks.custom.template.WrenchableBlock;
import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class WrenchItem extends Item {

	public WrenchItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (!isSelected || !(entity instanceof Player player)) {
			return;
		}

		final HitResult hitResult = player.pick(5.0, 0.0F, false);
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return;
		}

		final BlockHitResult blockHit = (BlockHitResult) hitResult;
		final BlockPos blockPos = blockHit.getBlockPos();
		final BlockState block = level.getBlockState(blockPos);
		final BlockEntity blockEntity = level.getBlockEntity(blockPos);

		if (blockEntity instanceof WrenchableBlock wrenchable) {
			wrenchable.onFocusWithWrench(stack, level, player);
		} else if (block.getBlock() instanceof WrenchableBlock wrenchable) {
			wrenchable.onFocusWithWrench(stack, level, player);
		}
	}

	@Override
	public InteractionResult useOn(final UseOnContext ctx) {
		if (ctx.getLevel() instanceof ServerLevel level) {
			final BlockPos pos = ctx.getClickedPos();
			final BlockState block = level.getBlockState(ctx.getClickedPos());
			final BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof WrenchableBlock wrenchable) {
				return wrenchable.onUseWrench(ctx);
			}
			if (block.getBlock() instanceof WrenchableBlock wrenchable) {
				return wrenchable.onUseWrench(ctx);
			}
		}
		return super.useOn(ctx);
	}
}
