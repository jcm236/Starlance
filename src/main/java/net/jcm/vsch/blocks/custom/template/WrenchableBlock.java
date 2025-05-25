package net.jcm.vsch.blocks.custom.template;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public interface WrenchableBlock {
	InteractionResult onUseWrench(UseOnContext ctx);

	default void onFocusWithWrench(ItemStack stack, Level level, Player player) {}
}
