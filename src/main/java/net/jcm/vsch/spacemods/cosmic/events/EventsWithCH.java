/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
package net.jcm.vsch.spacemods.cosmic.events;

import net.jcm.vsch.config.VSCHCommonConfig;
import net.jcm.vsch.util.EmptyChunkAccess;

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

import org.valkyrienskies.core.impl.config.VSCoreConfig;

/**
 * This class will only be registered to the forge event bus if cosmic horizons is loaded
 */
public class EventsWithCH {
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
			}
			case END -> {
				if (serverLevel.getPlayers(player -> true, 1).isEmpty()) {
					// skip if the no player is in the world
					// TODO: maybe we'll have automated ships in the future and this needs to be removed?
					return;
				}
				AtmosphericCollision.atmosphericCollisionTick(serverLevel);
				PlanetCollision.planetCollisionTick(serverLevel);
			}
		}
	}

	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof final ServerLevel level) {
            Gravity.updateFor(level);
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
