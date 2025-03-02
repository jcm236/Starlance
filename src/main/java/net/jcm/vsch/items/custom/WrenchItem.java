package net.jcm.vsch.items.custom;

import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

		HitResult hitResult = entity.pick(5.0, 0.0F, false);
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockHitResult blockHit = (BlockHitResult) hitResult;
		BlockPos blockPos = blockHit.getBlockPos();
		BlockEntity blockEntity = level.getBlockEntity(blockPos);

		if (blockEntity instanceof AbstractThrusterBlockEntity thruster) {
			player.displayClientMessage(
				Component.translatable("vsch.message.mode")
					.append(Component.translatable("vsch." + thruster.getThrusterMode().toString().toLowerCase())),
				true
			);
		}
	}
}
