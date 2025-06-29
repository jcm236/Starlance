package net.jcm.vsch.fluids;

import net.jcm.vsch.VSCHMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHFluidTypes {
	public static final ResourceLocation WATER_STILL_RL = new ResourceLocation("block/water_still");

	public static final DeferredRegister<FluidType> REGISTERY = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, VSCHMod.MODID);

	public static final RegistryObject<FluidType> HYDROGEN_FLUID_TYPE = REGISTERY.register(
		"hydrogen",
		() -> new BasicGasFluidType(
			WATER_STILL_RL,
			0xe0ffffff,
			FluidType.Properties.create()
		)
	);

	public static final RegistryObject<FluidType> OXYGEN_FLUID_TYPE = REGISTERY.register(
		"oxygen",
		() -> new BasicGasFluidType(
			WATER_STILL_RL,
			0xe08080ff,
			FluidType.Properties.create()
		)
	);

	public static void register(IEventBus eventBus) {
		REGISTERY.register(eventBus);
	}
}
