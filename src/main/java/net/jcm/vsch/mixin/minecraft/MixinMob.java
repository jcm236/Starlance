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
package net.jcm.vsch.mixin.minecraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMob extends Entity {
	private MixinMob() {
		super(null, null);
	}

	@Shadow
	public abstract Iterable<ItemStack> getArmorSlots();

	@Inject(method = "baseTick()V", at = @At(value = "HEAD"))
	private void tickArmors(final CallbackInfo cb) {
		final Level level = this.level();
		int i = 0;
		for (final ItemStack stack : this.getArmorSlots()) {
			if (!stack.isEmpty()) {
				final Item item = stack.getItem();
				if (item != null) {
					item.inventoryTick(stack, level, this, i, false);
				}
			}
			i++;
		}
	}
}
