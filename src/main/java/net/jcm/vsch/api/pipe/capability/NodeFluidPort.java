package net.jcm.vsch.api.pipe.capability;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

public interface NodeFluidPort extends NodePort {
	/**
	 * Check the fluid the port may interact with.
	 * If the raw type of the fluid stack is {@link Fluids#EMPTY}, any fluids may push to the port and no fluids can be pulled.
	 * Otherwise, the port may only accept that type of fluid.
	 *
	 * @return The fluid the port may interact with.
	 */
	FluidStack peekFluid();

	/**
	 * @param stack    The fluid pushing
	 * @param simulate If this is a simulate action
	 * @return The actual amount of fluid pushed
	 */
	int pushFluid(FluidStack stack, boolean simulate);

	/**
	 * @param amount   The maximum amount of the fluid pulling
	 * @param simulate If this is a simulate action
	 * @return The actual fluid pulled
	 */
	FluidStack pullFluid(int amount, boolean simulate);
}
