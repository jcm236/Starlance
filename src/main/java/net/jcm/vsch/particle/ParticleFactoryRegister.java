package net.jcm.vsch.particle;

import net.jcm.vsch.particle.custom.LaserHitParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ParticleFactoryRegister {
	@SubscribeEvent
	public static void registerFactories(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(VSCHParticles.LASER_HIT_PARTICLE.get(), LaserHitParticle.Provider::new);
	}
}
