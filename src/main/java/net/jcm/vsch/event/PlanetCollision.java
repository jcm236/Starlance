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
package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.event.PreTravelEvent;
import net.jcm.vsch.config.ShipLandingMode;
import net.jcm.vsch.config.VSCHCommonConfig;
import net.jcm.vsch.config.VSCHServerConfig;
import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.util.TeleportationHandler;
import net.jcm.vsch.util.VSCHUtils;
import net.jcm.vsch.util.wapi.LevelData;
import net.lointain.cosmos.network.CosmosModVariables;
import net.lointain.cosmos.world.inventory.LandingSelectorMenu;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public class PlanetCollision {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);
	private static final double CLOSE_RANGE = 16;
	private static final double OUTER_RANGE = 128;
	private static final double WARN_RANGE = OUTER_RANGE + 128;
	private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("0.0");

	public static void planetCollisionTick(final ServerLevel level) {
		final LevelData levelData = LevelData.get(level);
		if (!levelData.hasPlanets()) {
			return;
		}
		final String dimension = level.dimension().location().toString();

		final ShipLandingMode landingMode = VSCHServerConfig.SHIP_LANDING_MODE.get();
		final int landingAccuracy = VSCHServerConfig.SHIP_LANDING_ACCURACY.get();
		final int firstLandingAccuracy = VSCHServerConfig.SHIP_FIRST_LANDING_SPAWN_RANGE.get();

		final Map<ResourceKey<Level>, TeleportationHandler> handlers = new HashMap<>();
		final Map<ServerPlayer, LevelData.ClosestPlanetData> gravityDistances = new HashMap<>();

		final List<ServerPlayer> players = level.getPlayers((player) -> player.getClass() == ServerPlayer.class && !player.isSpectator());
		for (final ServerPlayer player : players) {
			final LevelData.ClosestPlanetData nearestPlanetData = levelData.getNearestPlanet(player.position());
			if (nearestPlanetData.distance() < WARN_RANGE) {
				gravityDistances.put(player, nearestPlanetData);
			}
		}

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

			final LevelData.ClosestPlanetData nearestPlanetData = levelData.getNearestPlanet(shipCenter);
			final ResourceKey<Level> targetDimension = nearestPlanetData.planet().getLevelData().getDimension();
			final ServerLevel targetLevel = level.getServer().getLevel(targetDimension);
			if (targetLevel == null) {
				continue;
			}

			{
				final TeleportationHandler handler = handlers.get(targetDimension);
				// TODO: hasShip will always returns false, since ships won't get added until next tick.
				// It is not a critical problem, because TeleportHandler internally will ignore duplicated ships.
				if (handler != null && handler.hasShip(ship)) {
					continue;
				}
			}

			final ShipLandingAttachment landingAttachment = ShipLandingAttachment.get(ship);
			final double distance = nearestPlanetData.distance();
			if (distance > OUTER_RANGE) {
				final ServerPlayer commander = landingAttachment.commander;
				if (commander != null && commander.containerMenu instanceof ShipLandingSelectorMenu) {
					setPlayerCapCheckCollision(commander, true);
					commander.doCloseContainer();
				}
				landingAttachment.launching = false;
				if (commander != null && distance < WARN_RANGE) {
					gravityDistances.put(commander, nearestPlanetData);
				}
				continue;
			}

			if (landingAttachment.launching && distance > 0) {
				continue;
			}
			if (!landingAttachment.frozen && ship.isStatic()) {
				// Ignore static ships to allow build ring around planets? (why will people do that)
				continue;
			}

			if (landingMode.canOpenMenu() && (landingAttachment.commander == null || !landingAttachment.commander.isAlive())) {
				final ServerPlayer nearestPlayer = getShipNearestPlayer(ship, level);
				if (nearestPlayer == null) {
					landingAttachment.commander = null;
					if (!landingMode.canUseHistory()) {
						continue;
					}
				} else {
					setPlayerCapCheckCollision(nearestPlayer, false);
					landingAttachment.commander = nearestPlayer;
				}
			}
			final ServerPlayer commander = landingAttachment.commander;

			final ChunkPos newChunkPos;
			final boolean isFirstLand;

			if (commander != null) {
				if (!ship.isStatic() && distance <= CLOSE_RANGE) {
					landingAttachment.freezeShip(ship);
				}

				// If they don't have the menu already open,
				if (!(commander.containerMenu instanceof ShipLandingSelectorMenu)) {
					// Open the menu and disable normal CH collision for them:
					LOGGER.debug("[starlance]: opened menu instead of CH");

					final BlockPos bpos = commander.blockPosition();
					NetworkHooks.openScreen(commander, new MenuProvider() {
						@Override
						public Component getDisplayName() {
							return Component.literal("LandingSelector");
						}

						@Override
						public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
							return new ShipLandingSelectorMenu(id, inventory, ship, bpos);
						}
					}, bpos);
				}
				// Otherwise, we just skip them since the playerMenuTick will take care of them.
				newChunkPos = playerMenuTick(commander, ship, level);
				isFirstLand = false;
				if (newChunkPos == null) {
					continue;
				}
			} else {
				final ChunkPos historyPos = landingAttachment.getLaunchPosition(targetDimension);
				if (historyPos == null) {
					newChunkPos = ChunkPos.ZERO;
					isFirstLand = true;
				} else {
					newChunkPos = historyPos;
					isFirstLand = false;
				}
			}

			final int accuracy = isFirstLand ? firstLandingAccuracy : landingAccuracy;

			final Vector2i addPos = VSCHUtils.randomPosOnSqaureRing(level.random, accuracy, new Vector2i());
			final Vector3d newPos = new Vector3d(
				SectionPos.sectionToBlockCoord(newChunkPos.x + addPos.x),
				nearestPlanetData.planet().getLevelData().getAtmosphereY(),
				SectionPos.sectionToBlockCoord(newChunkPos.z + addPos.y)
			);
			final Quaterniond rotation = new Quaterniond(nearestPlanetData.direction().getRotation());
			nearestPlanetData.planet().getRotation().mul(rotation, rotation).conjugate();

			MinecraftForge.EVENT_BUS.post(new PreTravelEvent.SpaceToPlanet(level.dimension(), ship.getTransform().getPositionInWorld(), targetDimension, newPos, rotation));

			LOGGER.info("[starlance]: Handling teleport {} ({}) to {} {} {} {}", ship.getSlug(), ship.getId(), targetDimension.location(), newPos.x, newPos.y, newPos.z);
			final TeleportationHandler handler = handlers.computeIfAbsent(
				targetDimension,
				(targetDim1) -> new TeleportationHandler(level, targetLevel, true)
			);
			handler.addShip(ship, newPos, rotation);
		}

		for (final TeleportationHandler handler : handlers.values()) {
			handler.afterShipsAdded().thenAcceptAsync((void_) -> {
				for (final LoadedServerShip ship : handler.getPendingShips()) {
					final ShipLandingAttachment attachment = ShipLandingAttachment.get(ship);
					attachment.frozen = false;
					attachment.setLanding();
				}
				handler.finalizeTeleport();
			}, level.getServer());
		}

		if (VSCHCommonConfig.SHOW_ENTERING_PLANET_HINT.get()) {
			for (final Map.Entry<ServerPlayer, LevelData.ClosestPlanetData> entry : gravityDistances.entrySet()) {
				final ServerPlayer player = entry.getKey();
				final LevelData.ClosestPlanetData planetData = entry.getValue();
				// TODO: use localized name?
				final String name = planetData.planet().getLevelData().getDimension().location().toString();
				final double distance = Math.max(planetData.distance() - OUTER_RANGE, 0);
				player.displayClientMessage(Component.translatable("vsch.message.planet.entering", name, DISTANCE_FORMAT.format(distance)), true);
			}
		}
	}

	private static ChunkPos playerMenuTick(
		final ServerPlayer player,
		final ServerShip ship,
		final ServerLevel level
	) {
		if (!(player.containerMenu instanceof ShipLandingSelectorMenu shipMenu)) {
			return null;
		}
		if (shipMenu.shipId != ship.getId()) {
			return null;
		}
		final CosmosModVariables.PlayerVariables vars = VSCHUtils.getPlayerCap(player);
		if (vars == null || vars.landing_coords.equals("^") || vars.landing_coords.equals("=")) {
			return null;
		}
		final int xStart = vars.landing_coords.indexOf("*");
		final int zStart = vars.landing_coords.indexOf("|");
		final int zEnd = vars.landing_coords.indexOf("~");
		if (xStart == -1 || zStart == -1 || zEnd == -1) {
			return null;
		}
		final double posX = Double.parseDouble(vars.landing_coords.substring(xStart + 1, zStart));
		final double posZ = Double.parseDouble(vars.landing_coords.substring(zStart + 1, zEnd));
		vars.landing_coords = "^";
		vars.check_collision = true;
		vars.syncPlayerVariables(player);

		return new ChunkPos(SectionPos.blockToSectionCoord(posX), SectionPos.blockToSectionCoord(posZ));
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

	private static void setPlayerCapCheckCollision(final ServerPlayer player, final boolean checkCollision) {
		final CosmosModVariables.PlayerVariables playerVars = VSCHUtils.getPlayerCap(player);
		if (playerVars == null) {
			return;
		}
		playerVars.check_collision = checkCollision;
		playerVars.syncPlayerVariables(player);
	}

	private static final class ShipLandingSelectorMenu extends LandingSelectorMenu {
		private final long shipId;

		public ShipLandingSelectorMenu(final int id, final Inventory inv, final ServerShip ship, final BlockPos pos) {
			super(id, inv, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
			this.shipId = ship.getId();
		}
	}
}
