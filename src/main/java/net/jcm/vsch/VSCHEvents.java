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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class VSCHEvents {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		for (final ServerLevel level : event.getServer().getAllLevels()) {
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
	public static void onServerStart(final ServerStartedEvent event) {
		GravityInducer.gravityDataTag = CosmosModVariables.WorldVariables.get(event.getServer().overworld()).gravity_data;
	}

	// For next vs update
	//	@SubscribeEvent
	//	public static void shipLoad(VSEvents.ShipLoadEvent event) {
	////		Gravity.setAll(event.getServer().overworld());
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

	public static void onBlockUpdate(final Level level, final BlockPos blockPos) {
		if (level.isClientSide) {
			return;
		}
		final NodeLevel nodeLevel = NodeLevel.get(level);
		for (final Pair<NodePos, PipeNode> node : nodeLevel.getNodesOn(blockPos)) {
			final NodePos pos = node.left();
			if (pos.canAnchoredIn(level, 4.0 / 16)) {
				continue;
			}
			nodeLevel.setNode(pos, null);
			final ItemStack stack = node.right().asItemStack();
			if (stack.isEmpty()) {
				continue;
			}
			final Vec3 center = pos.getCenter();
			level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, stack));
		}
	}
}
