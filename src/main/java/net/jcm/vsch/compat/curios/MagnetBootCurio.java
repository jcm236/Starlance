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
package net.jcm.vsch.compat.curios;

import net.jcm.vsch.items.custom.MagnetBootItem;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public final class MagnetBootCurio implements ICurio {
	private final MagnetBootItem item;
	private final ItemStack stack;

	public MagnetBootCurio(final MagnetBootItem item, final ItemStack stack) {
		this.item = item;
		this.stack = stack;
	}

	@Override
	public ItemStack getStack() {
		return this.stack;
	}

	@SuppressWarnings("removal")
	@Override
	public void curioTick(final SlotContext context) {
		final LivingEntity owner = context.getWearer();
		if (owner.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof MagnetBootItem) {
			return;
		}
		this.item.onInventoryTick(this.stack, owner.level(), owner);
	}
}
