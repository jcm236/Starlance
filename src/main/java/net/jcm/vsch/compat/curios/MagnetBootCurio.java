package net.jcm.vsch.compat.curios;

import net.jcm.vsch.items.custom.MagnetBootItem;

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
		this.item.onInventoryTick(this.stack, owner.level(), owner);
	}
}
