package net.jcm.vsch;

import net.jcm.vsch.config.VSCHCommonConfig;
import net.jcm.vsch.event.AtmosphericCollision;
import net.jcm.vsch.event.Gravity;
import net.jcm.vsch.event.PlanetCollision;
import net.jcm.vsch.util.EmptyChunkAccess;
import net.jcm.vsch.util.wapi.server.ServerPlanetData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.impl.config.VSCoreConfig;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mod.EventBusSubscriber
public class VSCHEvents {
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		switch (event.phase) {
			case START -> {
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
				ServerPlanetData.onSyncData(serverLevel);
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
	public static void onLevelLoad(final LevelEvent.Load event) {
		if (event.getLevel() instanceof final ServerLevel level) {
			Gravity.updateFor(level);
		}
	}

	@SubscribeEvent
	public static void onLevelUnload(final LevelEvent.Unload event) {
		if (event.getLevel() instanceof final ServerLevel level) {
			ServerPlanetData.onSyncData(level);
		}
	}

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

	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent joinEvent) {
		if (VSCoreConfig.SERVER.getPhysics().getLodDetail() >= 4096) {
			return;
		}

		if (VSCHCommonConfig.DISABLE_LOD_WARNING.get()) {
			return;
		}

		if (joinEvent.getEntity() instanceof ServerPlayer player) {
			player.sendSystemMessage(
				Component.translatable("vsch.lod_warning", VSCoreConfig.SERVER.getPhysics().getLodDetail())
					.withStyle(ChatFormatting.YELLOW)
			);
		}
	}
}
