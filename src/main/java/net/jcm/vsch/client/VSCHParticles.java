package net.jcm.vsch.client;

import net.jcm.vsch.client.particle.AirThrustParticle;
import net.jcm.vsch.client.particle.SmokeParticle;
import net.jcm.vsch.client.particle.ThrustParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
	value = Dist.CLIENT,
	bus = Mod.EventBusSubscriber.Bus.MOD
)
public class VSCHParticles {
	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(VSCHParticleTypes.THRUST.get(), ThrustParticle::provider);
		event.registerSpriteSet(VSCHParticleTypes.THRUST_BLUE.get(), ThrustParticle::provider);
		event.registerSpriteSet(VSCHParticleTypes.THRUST_AIR.get(), AirThrustParticle::provider);
		event.registerSpriteSet(VSCHParticleTypes.THRUST_SMOKE.get(), SmokeParticle::provider);
	}
}
