package net.jcm.vsch.compat.cc.peripherals;

import dan200.computercraft.shared.peripheral.generic.methods.EnergyMethods;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;

public final class CCGenerics {
	private CCGenerics() {}

	public static final EnergyMethods ENERGY_METHODS = new EnergyMethods();
	public static final FluidMethods FLUID_METHODS = new FluidMethods();
	public static final InventoryMethods INVENTORY_METHODS = new InventoryMethods();
}
