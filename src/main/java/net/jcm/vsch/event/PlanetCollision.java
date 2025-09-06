package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.api.event.PreTravelEvent;
import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.util.TeleportationHandler;
import net.jcm.vsch.util.VSCHUtils;
import net.lointain.cosmos.network.CosmosModVariables;
import net.lointain.cosmos.world.inventory.LandingSelectorMenu;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanetCollision {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);
	private static final EntityTypeTest<Entity, ServerPlayer> PLAYER_TESTER = new EntityTypeTest<>() {
		@Override
		public ServerPlayer tryCast(final Entity entity) {
			// We do not want FakePlayer (aka ServerPlayer's subclasses) being involved here
			return entity.getClass() == ServerPlayer.class ? (ServerPlayer)(entity) : null;
		}

		@Override
		public Class<ServerPlayer> getBaseClass() {
			return ServerPlayer.class;
		}
	};
	private static final double OUTER_RANGE = 128;
	private static final double CLOSE_RANGE = 16;

	public static void planetCollisionTick(final ServerLevel level) {
		final Map<String, TeleportationHandler> handlers = new HashMap<>();
		final String dimension = level.dimension().location().toString();
		for (final LoadedServerShip ship : VSCHUtils.getLoadedShipsInLevel(level)) {
			final Vec3 shipCenter = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB().center(new Vector3d()));

			final CompoundTag nearestPlanet = VSCHUtils.getNearestPlanet(level, shipCenter, dimension);
			if (nearestPlanet == null) {
				return;
			}
			ShipLandingAttachment landingAttachment = ship.getAttachment(ShipLandingAttachment.class);
			final VSCHUtils.DistanceInfo distanceInfo = VSCHUtils.getDistanceToPlanet(nearestPlanet, shipCenter);
			final double distance = distanceInfo.distance();
			if (distance > OUTER_RANGE) {
				if (landingAttachment != null) {
					if (landingAttachment.commander != null && landingAttachment.commander.containerMenu instanceof ShipLandingSelectorMenu) {
						landingAttachment.commander.doCloseContainer();
					}
					ship.saveAttachment(ShipLandingAttachment.class, null);
				}
				continue;
			}

			if (landingAttachment != null) {
				if (landingAttachment.launching && distance > 0) {
					continue;
				}
			} else {
				if (ship.isStatic()) {
					// Ignore static ships to allow build ring around planets? (why will people do that)
					continue;
				}
				landingAttachment = new ShipLandingAttachment();
				ship.saveAttachment(ShipLandingAttachment.class, landingAttachment);
			}

			if (landingAttachment.commander == null || !landingAttachment.commander.isAlive()) {
				final ServerPlayer nearestPlayer = getShipNearestPlayer(ship, level);
				if (nearestPlayer == null) {
					// TODO: let ships go through on their own
					continue;
				}
				final CosmosModVariables.PlayerVariables playerVars = VSCHUtils.getPlayerCap(nearestPlayer);
				if (playerVars != null) {
					playerVars.check_collision = false;
					playerVars.syncPlayerVariables(nearestPlayer);
				}
				landingAttachment.commander = nearestPlayer;
			}
			final ServerPlayer commander = landingAttachment.commander;

			if (!ship.isStatic()) {
				landingAttachment.velocity = new Vector3d(ship.getVelocity());
				landingAttachment.omega = new Vector3d(ship.getOmega());
				if (distance <= CLOSE_RANGE) {
					ship.setStatic(true);
				}
			}

			// If they don't have the menu already open,
			if (!(commander.containerMenu instanceof ShipLandingSelectorMenu)) {
				// Open the menu and disable normal CH collision for them:
				LOGGER.info("[starlance]: opened menu instead of CH");

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
			playerMenuTick(commander, ship, level, nearestPlanet, distanceInfo.direction(), handlers);
		}

		for (final TeleportationHandler handler : handlers.values()) {
			for (final LoadedServerShip ship : handler.getPendingShips()) {
				ship.saveAttachment(ShipLandingAttachment.class, new ShipLandingAttachment(false, true));
			}
			handler.finalizeTeleport();
		}
	}

	private static void playerMenuTick(
		final ServerPlayer player,
		final ServerShip ship,
		final ServerLevel level,
		final CompoundTag planet,
		final Direction direction,
		final Map<String, TeleportationHandler> handlers
	) {
		if (!(player.containerMenu instanceof ShipLandingSelectorMenu shipMenu)) {
			return;
		}
		if (shipMenu.shipId != ship.getId()) {
			return;
		}
		final CosmosModVariables.PlayerVariables vars = VSCHUtils.getPlayerCap(player);
		if (vars == null || vars.landing_coords.equals("^") || vars.landing_coords.equals("=")) {
			return;
		}

		final String targetDim = planet.getString("travel_to");
		if (targetDim.isEmpty()) {
			LOGGER.error("[starlance]: Planet in {} has no travel_to dimension. Please report this! Planet data: {}", level.dimension().location(), planet);
			// We should in theory never get here if I've done my null checks correctly when getting the antennas in the first place
			return;
		}

		final ServerLevel targetLevel = VSCHUtils.dimToLevel(targetDim);
		if (targetLevel == null) {
			return;
		}

		final double atmoY = CosmosModVariables.WorldVariables.get(level).atmospheric_collision_data_map.getCompound(targetDim).getDouble("atmosphere_y");
		final double posX = Double.parseDouble(vars.landing_coords.substring(vars.landing_coords.indexOf("*") + 1, vars.landing_coords.indexOf("|")));
		final double posZ = Double.parseDouble(vars.landing_coords.substring(vars.landing_coords.indexOf("|") + 1, vars.landing_coords.indexOf("~")));
		final Vector3d newPos = new Vector3d(posX, atmoY, posZ);
		final Quaterniond rotation = new Quaterniond(direction.getRotation());

		MinecraftForge.EVENT_BUS.post(new PreTravelEvent.SpaceToPlanet(level.dimension(), ship.getTransform().getPositionInWorld(), targetLevel.dimension(), newPos, rotation));

		LOGGER.info("[starlance]: Handling teleport {} ({}) to {} {} {} {}", ship.getSlug(), ship.getId(), targetDim, newPos.x, newPos.y, newPos.z);
		final TeleportationHandler handler = handlers.computeIfAbsent(
			targetDim,
			(dimStr) -> new TeleportationHandler(level, VSCHUtils.dimToLevel(dimStr), true)
		);
		handler.addShip(ship, newPos, rotation);

		vars.landing_coords = "^";
		vars.check_collision = true;
		vars.syncPlayerVariables(player);
	}

	/**
	 * Not a util function because its very specific to planetCollisionTick
	 * Gets the nearest player that is inside the ships AABB and previous AABB.
	 * @param ship
	 * @param level
	 * @return the nearest player found, or null
	 */
	private static ServerPlayer getShipNearestPlayer(final Ship ship, final ServerLevel level) {
		// Get the AABB of the last tick and the AABB of the current tick
		final AABBdc shipBox = ship.getWorldAABB();
		final AABB prevWorldAABB = VectorConversionsMCKt.toMinecraft(VSCHUtils.transformToAABBd(ship.getPrevTickTransform(), ship.getShipAABB())).inflate(8);
		final AABB currentWorldAABB = VectorConversionsMCKt.toMinecraft(shipBox).inflate(10);
		final Vec3 center = VectorConversionsMCKt.toMinecraft(shipBox.center(new Vector3d()));

		// Combine the AABB's into one big one
		final AABB totalAABB = currentWorldAABB.minmax(prevWorldAABB);

		final List<ServerPlayer> players = level.getEntities(PLAYER_TESTER, totalAABB, EntitySelector.NO_SPECTATORS);

		ServerPlayer nearestPlayer = null;
		double nearestDistance = Double.MAX_VALUE;
		for (final ServerPlayer player : players) {
			final double distance = player.distanceToSqr(center);
			if (distance < nearestDistance) {
				nearestPlayer = player;
				nearestDistance = distance;
			}
		}
		return nearestPlayer;
	}

	private static final class ShipLandingSelectorMenu extends LandingSelectorMenu {
		private final long shipId;

		public ShipLandingSelectorMenu(final int id, final Inventory inv, final ServerShip ship, final BlockPos pos) {
			super(id, inv, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
			this.shipId = ship.getId();
		}
	}
}
