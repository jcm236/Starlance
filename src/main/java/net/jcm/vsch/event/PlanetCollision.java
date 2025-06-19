package net.jcm.vsch.event;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.util.TeleportationHandler;
import net.jcm.vsch.util.VSCHUtils;
import net.lointain.cosmos.network.CosmosModVariables;
import net.lointain.cosmos.world.inventory.LandingSelectorMenu;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

public class PlanetCollision {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	public static void planetCollisionTick(ServerLevel level) {
		final String dimension = level.dimension().location().toString();
		for (Ship ship : VSCHUtils.getLoadedShipsInLevel(level)) {
			final Vec3 shipCenter = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB().center(new Vector3d()));

			final CompoundTag nearestPlanet = VSCHUtils.getNearestPlanet(level, shipCenter, dimension);
			if (nearestPlanet == null) {
				return;
			}
			if (!VSCHUtils.isCollidingWithPlanet(nearestPlanet, shipCenter)) {
				continue;
			}

			final Player nearestPlayer = getShipPlayer(ship, level);
			if (nearestPlayer == null) {
				// TODO: let ships go through on their own
				continue;
			}

			final CosmosModVariables.PlayerVariables playerVars = VSCHUtils.getPlayerCap(nearestPlayer);
			if (playerVars != null) {
				playerVars.check_collision = false;
				playerVars.syncPlayerVariables(nearestPlayer);
			}

			// If they don't have the menu already open,
			if (!(nearestPlayer.containerMenu instanceof LandingSelectorMenu)) {
				// Open the menu and disable normal CH collision for them:
				LOGGER.info("[starlance]: opened menu instead of CH");

				final BlockPos bpos = BlockPos.containing(nearestPlayer.getX(), nearestPlayer.getY(), nearestPlayer.getZ());
				NetworkHooks.openScreen((ServerPlayer) nearestPlayer, new MenuProvider() {
					@Override
					public Component getDisplayName() {
						return Component.literal("LandingSelector");
					}

					@Override
					public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
						return new LandingSelectorMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(bpos));
					}
				}, bpos);

			}
			// Otherwise, we just skip them since the playerMenuTick will take care of them.
			playerMenuTick(nearestPlayer, ship, level, nearestPlanet);
		}
	}

	private static void playerMenuTick(Player player, Ship ship, ServerLevel level, CompoundTag planet) {
		if (!(player.containerMenu instanceof LandingSelectorMenu)) {
			return;
		}
		final CosmosModVariables.PlayerVariables vars = VSCHUtils.getPlayerCap(player);
		if (vars == null || vars.landing_coords.equals("^") || vars.landing_coords.equals("=")) {
			return;
		}

		final String targetDim = planet.getString("travel_to");
		if (targetDim.isEmpty()) {
			LOGGER.error("[starlance]: Planet has no travel_to dimension. Please report this");
			// We should in theory never get here if I've done my null checks correctly when getting the antennas in the first place
			return;
		}

		final double atmoY = CosmosModVariables.WorldVariables.get(level).atmospheric_collision_data_map.getCompound(targetDim).getDouble("atmosphere_y");
		double posX = Double.parseDouble(vars.landing_coords.substring(vars.landing_coords.indexOf("*") + 1, vars.landing_coords.indexOf("|")));
		double posZ = Double.parseDouble(vars.landing_coords.substring(vars.landing_coords.indexOf("|") + 1, vars.landing_coords.indexOf("~")));
		double posY = atmoY - 10;

		LOGGER.info("[starlance]: Handling teleport {} ({}) to {} {} {} {}", ship.getSlug(), ship.getId(), targetDim, posX, posY, posZ);
		final TeleportationHandler handler = new TeleportationHandler(VSCHUtils.dimToLevel(targetDim), level, true);
		handler.handleTeleport(ship, new Vector3d(posX, posY, posZ));

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
	private static Player getShipPlayer(Ship ship, ServerLevel level) {
		// Get the AABB of the last tick and the AABB of the current tick
		final AABB prevWorldAABB = VectorConversionsMCKt.toMinecraft(VSCHUtils.transformToAABBd(ship.getPrevTickTransform(), ship.getShipAABB())).inflate(10);
		final AABB currentWorldAABB = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB()).inflate(10);
		final Vec3 center = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB().center(new Vector3d()));

		// Combine the AABB's into one big one
		final AABB totalAABB = currentWorldAABB.minmax(prevWorldAABB);

		final List<Player> players = level.getEntities(EntityTypeTest.forClass(Player.class), totalAABB, EntitySelector.NO_SPECTATORS);

		Player nearestPlayer = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Player player : players) {
			double distance = player.distanceToSqr(center);
			if (distance < nearestDistance) {
				nearestPlayer = player;
				nearestDistance = distance;
			}
		}
		return nearestPlayer;
	}
}
