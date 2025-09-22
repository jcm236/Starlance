package net.jcm.vsch.items;

import net.jcm.vsch.compat.CompatMods;
import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.network.c2s.ToggleItemPacketC2S;
import net.jcm.vsch.util.VSCHUtils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public interface IToggleableItem {
	void onToggle(Player owner, ItemStack stack);

	static boolean toggleSlot(final Player player, final int slot) {
		final ItemStack stack = player.getInventory().getItem(slot);
		if (stack.isEmpty()) {
			return false;
		}
		if (!(stack.getItem() instanceof final IToggleableItem item)) {
			return false;
		}
		item.onToggle(player, stack);
		if (player.level().isClientSide) {
			VSCHNetwork.sendToServer(new ToggleItemPacketC2S(slot));
		}
		return true;
	}

	static boolean toggleCuriosSlot(final Player player, final String id, final Predicate<ItemStack> tester) {
		return VSCHUtils.testCuriosItems(player, id, (stack, slot) -> {
			if (!(stack.getItem() instanceof final IToggleableItem item)) {
				return false;
			}
			if (!tester.test(stack)) {
				return false;
			}
			item.onToggle(player, stack);
			if (player.level().isClientSide) {
				VSCHNetwork.sendToServer(new ToggleItemPacketC2S(id, slot));
			}
			return true;
		});
	}
}
