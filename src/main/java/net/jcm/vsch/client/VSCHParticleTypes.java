package net.jcm.vsch.client;

import net.jcm.vsch.VSCHMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class VSCHParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, VSCHMod.MODID);

    public static final RegistryObject<SimpleParticleType> THRUST_AIR = REGISTRY.register("thrust_air", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> THRUST_BLUE = REGISTRY.register("thrust_blue", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> THRUST = REGISTRY.register("thrust", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> THRUST_SMOKE = REGISTRY.register("thrust_smoke", () -> new SimpleParticleType(true));

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }
}
