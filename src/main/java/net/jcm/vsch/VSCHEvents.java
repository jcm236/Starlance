package net.jcm.vsch;

import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.event.AtmosphericCollision;
import net.jcm.vsch.event.GravityInducer;
import net.jcm.vsch.event.PlanetCollision;
import net.jcm.vsch.pipe.level.NodeLevel;
import net.jcm.vsch.util.EmptyChunkAccess;
import net.jcm.vsch.util.Pair;
import net.lointain.cosmos.network.CosmosModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.function.Predicate;

@Mod.EventBusSubscriber
public class VSCHEvents {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.START) {
			return;
		}
		for (final LoadedServerShip ship : VSGameUtilsKt.getShipObjectWorld(event.getServer()).getLoadedShips()) {
			GravityInducer.getOrCreate(ship);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLevelTick(final TickEvent.LevelTickEvent event) {
		if (!(event.level instanceof ServerLevel serverLevel)) {
			return;
		}
		switch (event.phase) {
			case START -> {
				NodeLevel.get(serverLevel).getNetwork().onTick();
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

	@SubscribeEvent
	public static void onServerStart(final ServerStartedEvent event) {
		GravityInducer.gravityDataTag = CosmosModVariables.WorldVariables.get(event.getServer().overworld()).gravity_data;
	}

	// For next vs update
	//	@SubscribeEvent
	//	public static void shipLoad(VSEvents.ShipLoadEvent event) {
	//		Gravity.setAll(event.getServer().overworld());
	//	}

	@SubscribeEvent
	public static void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof Level level)) {
			return;
		}
		final ChunkPos pos = new ChunkPos(event.getPos());
		if (EmptyChunkAccess.shouldUseEmptyChunk(level, pos.x, pos.z)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onBlockUpdate(final BlockEvent.NeighborNotifyEvent event) {
		if (!(event.getLevel() instanceof Level level)) {
			return;
		}
		onBlockChange(level, event.getPos());
	}

	/**
	 * Do not use BlockEvent.NeighborNotifyEvent here, since it won't trigger for shape update.
	 */
	public static void onBlockChange(final Level level, final BlockPos blockPos) {
		if (level.isClientSide) {
			return;
		}
		final NodeLevel nodeLevel = NodeLevel.get(level);
		nodeLevel.streamNodesOn(blockPos)
			.filter(Predicate.not(PipeNode::canAnchor))
			.map(PipeNode::getPos)
			.forEach(nodeLevel::breakNode);
	}
}
