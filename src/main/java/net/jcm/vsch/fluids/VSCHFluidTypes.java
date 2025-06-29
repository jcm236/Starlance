package net.jcm.vsch.fluids;

import net.jcm.vsch.VSCHMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHFluidTypes {
	public static final ResourceLocation WATER_STILL_RL = new ResourceLocation("minecraft", "block/water_still");
	public static final ResourceLocation WATER_FLOW_RL = new ResourceLocation("minecraft", "block/water_flow");

	public static final DeferredRegister<FluidType> REGISTERY = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, VSCHMod.MODID);

	public static final RegistryObject<FluidType> HYDROGEN_FLUID_TYPE = REGISTERY.register(
		"hydrogen",
		() -> new GasFluidType(
			WATER_STILL_RL,
			WATER_FLOW_RL,
			0xe0ffffff,
			FluidType.Properties.create()
				.density(71)
				.temperature(20)
				.viscosity(182)
		)
	);

	public static final RegistryObject<FluidType> HYDROGEN_PEROXIDE_FLUID_TYPE = REGISTERY.register(
		"hydrogen_peroxide",
		() -> new GasFluidType(
			WATER_STILL_RL,
			WATER_FLOW_RL,
			0xd8b4f0ff,
			FluidType.Properties.create()
				.density(1450)
				.temperature(300)
				.viscosity(1249)
		)
	);

	public static final RegistryObject<FluidType> OXYGEN_FLUID_TYPE = REGISTERY.register(
		"oxygen",
		() -> new GasFluidType(
			WATER_STILL_RL,
			WATER_FLOW_RL,
			0xe029d9ff,
			FluidType.Properties.create()
				.density(1141)
				.temperature(90)
				.viscosity(1870)
		)
	);

	public static void register(IEventBus eventBus) {
		REGISTERY.register(eventBus);
	}
}
