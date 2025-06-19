package net.jcm.vsch;

import net.jcm.vsch.particle.VSCHParticles;
import net.jcm.vsch.particle.custom.LaserHitParticle;
import net.lointain.cosmos.network.CosmosModVariables;
import net.jcm.vsch.event.GravityInducer;
import net.jcm.vsch.event.PlanetCollision;
import net.jcm.vsch.event.AtmosphericCollision;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class VSCHEvents {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		for (ServerLevel level : event.getServer().getAllLevels()) {
			if (level.getPlayers(player -> true, 1).isEmpty()) {
				// skip if the no player is in the world
				// TODO: maybe we'll have automated ships in the future and this need to be removed?
				continue;
			}
			AtmosphericCollision.atmosphericCollisionTick(level);
			PlanetCollision.planetCollisionTick(level);
		}
	}

	@SubscribeEvent
	public static void onServerStart(ServerStartedEvent event) {
		GravityInducer.gravityDataTag = CosmosModVariables.WorldVariables.get(event.getServer().overworld()).gravity_data;
	}

	// For next vs update
	//	@SubscribeEvent
	//	public static void shipLoad(VSEvents.ShipLoadEvent event) {
	////		Gravity.setAll(event.getServer().overworld());
	//	}

}
