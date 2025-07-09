package net.jcm.vsch.api.pipe.capability;

import net.minecraft.world.level.material.Fluid;

public interface NodeFluidPort extends NodePort {
	/**
	 * @param fluid    The type of the fluid pushing
	 * @param amount   The amount of the fluid pushing
	 * @param simulate If this is a simulate action
	 * @return The actual amount of fluid pushed
	 */
	int pushFluid(Fluid fluid, int amount, boolean simulate);

	/**
	 * @param fluid    The type of the fluid pulling
	 * @param amount   The amount of the fluid pulling
	 * @param simulate If this is a simulate action
	 * @return The actual amount of fluid pulled
	 */
	int pullFluid(Fluid fluid, int amount, boolean simulate);
}
