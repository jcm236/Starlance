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
package net.jcm.vsch.spacemods.ad_astra.events;

import earth.terrarium.adastra.common.config.AdAstraConfig;
import earth.terrarium.adastra.common.menus.PlanetsMenu;
import earth.terrarium.adastra.common.menus.base.PlanetsMenuProvider;
import earth.terrarium.adastra.common.registry.ModSoundEvents;
import earth.terrarium.botarium.common.menu.MenuHooks;
import io.netty.buffer.Unpooled;
import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.event.PreTravelEvent;
import net.jcm.vsch.config.ShipLandingMode;
import net.jcm.vsch.config.VSCHCommonConfig;
import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.spacemods.cosmic.wapi.LevelData;
import net.jcm.vsch.util.TeleportationHandler;
import net.jcm.vsch.util.VSCHUtils;
import net.lointain.cosmos.network.CosmosModVariables;
import net.lointain.cosmos.world.inventory.LandingSelectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaterniond;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtmosphericCollision {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	public static void atmosphericCollisionTick(final ServerLevel level) {

		final String dimension = level.dimension().location().toString();

		final Map<ResourceKey<Level>, TeleportationHandler> handlers = new HashMap<>();

		final List<LoadedServerShip> ships = VSCHUtils.getLoadedShipsInLevel(level);
		ships.sort((a, b) -> {
			final AABBdc aBox = a.getWorldAABB();
			final AABBdc bBox = b.getWorldAABB();
			final double n =
				(aBox.maxX() - aBox.minX()) * (aBox.maxY() - aBox.minY()) * (aBox.maxZ() - aBox.minZ())
				- (bBox.maxX() - bBox.minX()) * (bBox.maxY() - bBox.minY()) * (bBox.maxZ() - bBox.minZ());
			if (n < 0) {
				return 1;
			}
			if (n > 0) {
				return -1;
			}
			return Long.compare(a.getId(), b.getId());
		});

		for (final LoadedServerShip ship : ships) {
            final Vec3 shipCenter = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB().center(new Vector3d()));

            ShipLandingAttachment attachment = ShipLandingAttachment.get(ship);
            if (shipCenter.y >= AdAstraConfig.atmosphereLeave) {
                final ServerPlayer nearestPlayer = getShipNearestPlayer(ship, level);
                if (nearestPlayer == null) continue;
                if (attachment.landing) continue;

                if (!(nearestPlayer.containerMenu instanceof PlanetsMenu)) {
                    openPlanetsScreen(nearestPlayer);
                }
            } else {
                attachment.landing = false;
            }
        }
	}

    /**
     * Not a util function because its very specific to planetCollisionTick
     * Gets the nearest player that is inside the ships AABB and previous AABB.
     * @param ship
     * @param level
     * @return the nearest player found, or null
     */
    private static ServerPlayer getShipNearestPlayer(final Ship ship, final ServerLevel level) {
        final AABBdc shipBox = ship.getWorldAABB();
        final AABB currentWorldAABB = VectorConversionsMCKt.toMinecraft(shipBox).inflate(10);
        final Vec3 center = VectorConversionsMCKt.toMinecraft(shipBox.center(new Vector3d()));

        final List<ServerPlayer> players = level.getPlayers(
                (player) -> player.getClass() == ServerPlayer.class && !player.isSpectator() && player.getBoundingBox().intersects(currentWorldAABB)
        );

        ServerPlayer nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        for (final ServerPlayer player : players) {
            final Entity root = player.getRootVehicle();
            final Ship rootShip = VSGameUtilsKt.getShipManagingPos(level, root.blockPosition());
            if (rootShip != null && rootShip.getId() != ship.getId()) {
                continue;
            }
            final double distance = player.distanceToSqr(center);
            if (distance < nearestDistance) {
                nearestPlayer = player;
                nearestDistance = distance;
            }
        }
        return nearestPlayer;
    }

    public static void openPlanetsScreen(ServerPlayer player) {
        MenuHooks.openMenu(player, new PlanetsMenuProvider());
        var packet = new ClientboundStopSoundPacket(BuiltInRegistries.SOUND_EVENT
                .getKey(ModSoundEvents.ROCKET.get()), SoundSource.AMBIENT);
        player.connection.send(packet);
    }
}
