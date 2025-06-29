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

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,VSCHMod.MODID);

    public static final RegistryObject<FluidType> BASIC_GAS_FLUID_TYPE = register("basic_gas",
            FluidType.Properties.create()
    );



    private static RegistryObject<FluidType> register(String name, FluidType.Properties properties) {
        return FLUID_TYPES.register(name, () -> new BasicGasFluidType(
                WATER_STILL_RL, // Used as the texture in JEI and stuff, important
                0xFFFFFFFF, //TODO: Make this more transparent if possible
                properties
            )
        );
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
    }
}
