package net.jcm.vsch.items.custom;

import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.blocks.custom.template.WrenchableBlock;
import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
		final Level level = ctx.getLevel();
		final Player player = ctx.getPlayer();
		final BlockPos blockPos = ctx.getClickedPos();
		final Vec3 pos = ctx.getClickLocation();
		if (ctx.getHand() == InteractionHand.OFF_HAND) {
			if (player == null || !ctx.isSecondaryUseActive()) {
				return super.useOn(ctx);
			}
			final NodeLevel nodeLevel = NodeLevel.get(level);
			final NodePos nodePos = NodePos.fromHitResult(level, blockPos, pos, 4.0 / 16);
			if (nodePos == null) {
				return super.useOn(ctx);
			}
			final PipeNode node = nodeLevel.getNode(nodePos);
			if (node == null) {
				return InteractionResult.FAIL;
			}
			nodeLevel.setNode(nodePos, null);
			if (!level.isClientSide && !player.getAbilities().instabuild) {
				final ItemStack nodeStack = node.asItemStack();
				player.getInventory().placeItemBackInInventory(nodeStack);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		} else if (level instanceof ServerLevel serverLevel) {
			final BlockState block = serverLevel.getBlockState(blockPos);
			final BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
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
