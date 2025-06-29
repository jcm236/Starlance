package net.jcm.vsch.fluids;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.blocks.VSCHBlocks;
import net.jcm.vsch.items.VSCHItems;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHFluids {

	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, VSCHMod.MODID);

	public static final RegistryObject<FlowingFluid> HYDROGEN = FLUIDS.register(
		"hydrogen",
		() -> new ForgeFlowingFluid.Source(VSCHFluids.HYDROGEN_PROPERTIES)
	);

	public static final RegistryObject<FlowingFluid> HYDROGEN_FLOWING = FLUIDS.register(
		"hydrogen_flowing",
		() -> new ForgeFlowingFluid.Flowing(VSCHFluids.HYDROGEN_PROPERTIES)
	);

	public static final RegistryObject<FlowingFluid> OXYGEN = FLUIDS.register(
		"oxygen",
		() -> new ForgeFlowingFluid.Source(VSCHFluids.OXYGEN_PROPERTIES)
	);

	public static final RegistryObject<FlowingFluid> OXYGEN_FLOWING = FLUIDS.register(
		"oxygen_flowing",
		() -> new ForgeFlowingFluid.Flowing(VSCHFluids.OXYGEN_PROPERTIES)
	);

	public static final ForgeFlowingFluid.Properties HYDROGEN_PROPERTIES =
		new ForgeFlowingFluid.Properties(VSCHFluidTypes.HYDROGEN_FLUID_TYPE, HYDROGEN, HYDROGEN_FLOWING)
			.bucket(VSCHItems.HYDROGEN_BUCKET)
			.block(VSCHBlocks.HYDROGEN_BLOCK);

	public static final ForgeFlowingFluid.Properties OXYGEN_PROPERTIES =
		new ForgeFlowingFluid.Properties(VSCHFluidTypes.OXYGEN_FLUID_TYPE, OXYGEN, OXYGEN_FLOWING)
			.bucket(VSCHItems.OXYGEN_BUCKET)
			.block(VSCHBlocks.OXYGEN_BLOCK);

	public static void register(IEventBus eventBus) {
		FLUIDS.register(eventBus);
	}
}
