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
import earth.terrarium.botarium.common.menu.MenuHooks;
import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.ship.ShipTierAttachment;
import net.jcm.vsch.spacemods.ad_astra.SyncMenuTierS2C;
import net.jcm.vsch.util.TaskUtil;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

public final class AtmosphericCollision {
	public static void atmosphericCollisionTick(final ServerLevel level) {
		final List<LoadedServerShip> ships = VSCHUtils.getLoadedShipsInLevel(level);
		if (ships.isEmpty()) {
			return;
		}

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
			final Vector3d shipCenter = ship.getWorldAABB().center(new Vector3d());

			if (shipCenter.y < AdAstraConfig.atmosphereLeave) {
				if ((shipCenter.y + 10) < AdAstraConfig.atmosphereLeave) {
					final ShipLandingAttachment attachment = ShipLandingAttachment.get(ship);
					attachment.landing = false;
				}
				continue;
			}

			final ServerPlayer nearestPlayer = getShipNearestPlayer(ship, level);
			if (nearestPlayer == null) {
				continue;
			}
			final ShipLandingAttachment attachment = ShipLandingAttachment.get(ship);
			if (attachment.landing) {
				continue;
			}

			final ShipTierAttachment tierAttachment = ShipTierAttachment.get(ship);
			if (!(nearestPlayer.containerMenu instanceof PlanetsMenu)) {
				attachment.freezeShip(ship);
				openPlanetsScreen(tierAttachment.getHighestTier(), nearestPlayer);
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

	public static void openPlanetsScreen(int tier, ServerPlayer player) {
		VSCHNetwork.sendToPlayer(new SyncMenuTierS2C(tier), player);

		TaskUtil.queueTickStart(() -> {
			MenuHooks.openMenu(player, new PlanetsMenuProvider());
		});
	}
}
