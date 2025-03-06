package net.jcm.vsch.items.custom;

import net.jcm.vsch.blocks.custom.template.AbstractThrusterBlock;
import net.jcm.vsch.ship.ThrusterData.ThrusterMode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.valkyrienskies.mod.common.VSClientGameUtils;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class WrenchItem extends Item {

	public WrenchItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {

		if (!isSelected) {
			return;
		}

		if (!(entity instanceof Player player)) {
			return;
		}

		HitResult hitResult = entity.pick(5.0, 0.0F, false);

		if (hitResult.getType() == HitResult.Type.BLOCK) {
			BlockHitResult blockHit = (BlockHitResult) hitResult;
			BlockPos blockPos = blockHit.getBlockPos();
			BlockState blockState = level.getBlockState(blockPos);

			if (VSGameUtilsKt.isBlockInShipyard(level, blockPos)) {
				// Does it have a thruster mode property
				if (blockState.hasProperty(AbstractThrusterBlock.MODE)) {
					// Send actionbar of its state
					player.displayClientMessage(Component.translatable("vsch.message.mode").append(Component.translatable("vsch." + blockState.getValue(AbstractThrusterBlock.MODE).toString().toLowerCase())), true);
				}
			}
		}

	}
}
