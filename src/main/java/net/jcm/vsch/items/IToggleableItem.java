/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
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
