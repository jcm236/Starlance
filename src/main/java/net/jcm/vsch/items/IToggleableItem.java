package net.jcm.vsch.items;

import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.network.c2s.ToggleItemPacketC2S;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IToggleableItem {
	void onToggle(Player owner, int slot, ItemStack stack);

	static boolean toggleSlot(final Player player, final int slot) {
		final ItemStack stack = player.getInventory().getItem(slot);
		if (stack.isEmpty()) {
			return false;
		}
		if (!(stack.getItem() instanceof final IToggleableItem item)) {
			return false;
		}
		item.onToggle(player, slot, stack);
		if (player.level().isClientSide) {
			VSCHNetwork.sendToServer(new ToggleItemPacketC2S(slot));
		}
		return true;
	}
}
