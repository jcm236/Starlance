package net.jcm.vsch.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.particle.custom.LaserHitParticleOption;

public class VSCHParticles {
	public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
		DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, VSCHMod.MODID);

	public static final RegistryObject<ParticleType<LaserHitParticleOption>> LASER_HIT_PARTICLE =
		PARTICLE_TYPES.register("laser_hit_particle", () -> new ParticleType<LaserHitParticleOption>(true, LaserHitParticleOption.DESERIALIZER) {
			@Override
    	public Codec<LaserHitParticleOption> codec() {
    		return LaserHitParticleOption.codec(LASER_HIT_PARTICLE.get());
    	}
		});

	public static void register(IEventBus eventBus) {
		PARTICLE_TYPES.register(eventBus);
	}
}
