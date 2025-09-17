package net.jcm.vsch;

import net.jcm.vsch.event.AtmosphericCollision;
import net.jcm.vsch.event.GravityInducer;
import net.jcm.vsch.event.PlanetCollision;
import net.jcm.vsch.util.EmptyChunkAccess;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mod.EventBusSubscriber
public class VSCHEvents {
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		switch (event.phase) {
			case START -> {
				for (final LoadedServerShip ship : VSGameUtilsKt.getShipObjectWorld(event.getServer()).getLoadedShips()) {
					GravityInducer.tickOnShip(ship);
				}
			}
			case END -> {
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLevelTick(final TickEvent.LevelTickEvent event) {
		if (!(event.level instanceof ServerLevel serverLevel)) {
			return;
		}
		switch (event.phase) {
			case START -> {
			}
			case END -> {
				if (serverLevel.getPlayers(player -> true, 1).isEmpty()) {
					// skip if the no player is in the world
					// TODO: maybe we'll have automated ships in the future and this need to be removed?
					return;
				}
				AtmosphericCollision.atmosphericCollisionTick(serverLevel);
				PlanetCollision.planetCollisionTick(serverLevel);
			}
		}
	}

	// For next vs update
	//	@SubscribeEvent
	//	public static void shipLoad(VSEvents.ShipLoadEvent event) {
	////		Gravity.setAll(event.getServer().overworld());
	//	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof Level level)) {
			return;
		}
		final ChunkPos pos = new ChunkPos(event.getPos());
		if (EmptyChunkAccess.shouldUseEmptyChunk(level, pos.x, pos.z)) {
			event.setCanceled(true);
		}
	}
}
