package net.jcm.vsch.fluids;

import net.jcm.vsch.VSCHMod;
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

    // For some reason JEI uses the fluid type's registry name to display on the fluid itself in JEI
    // So we might want to change this around later, but for now the buckets will cover up that weirdness
    public static final RegistryObject<FlowingFluid> HYDROGEN = FLUIDS.register("hydrogen",
            () -> new ForgeFlowingFluid.Source(VSCHFluids.HYDROGEN_PROPERTIES));

    public static final RegistryObject<FlowingFluid> OXYGEN = FLUIDS.register("oxygen",
            () -> new ForgeFlowingFluid.Source(VSCHFluids.OXYGEN_PROPERTIES));

    public static final ForgeFlowingFluid.Properties HYDROGEN_PROPERTIES = new ForgeFlowingFluid.Properties(
            VSCHFluidTypes.BASIC_GAS_FLUID_TYPE, HYDROGEN, Fluids.FLOWING_WATER.builtInRegistryHolder())
            .bucket(VSCHItems.HYDROGEN_BUCKET);

    public static final ForgeFlowingFluid.Properties OXYGEN_PROPERTIES = new ForgeFlowingFluid.Properties(
            VSCHFluidTypes.BASIC_GAS_FLUID_TYPE, OXYGEN, Fluids.FLOWING_WATER.builtInRegistryHolder())
            .bucket(VSCHItems.OXYGEN_BUCKET);


    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
    }
}
