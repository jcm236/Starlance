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

		if (!CosmosModVariables.WorldVariables.get(level).collision_data_map.contains(dimension)) {
			return;
		}

		for (Ship ship : VSCHUtils.getLoadedShipsInLevel(level)) {
			AABB currentAABB = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB());
			Vec3 shipCenter = currentAABB.getCenter();

			CompoundTag nearestPlanet = VSCHUtils.getNearestPlanet(level, shipCenter, dimension);
			if (nearestPlanet == null) {
				continue;
			}

			Player nearestPlayer = getShipPlayer(ship, level);
			if (nearestPlayer == null) {
				continue;
			}

			// Only continue rest of code if this ship is colliding with a planet
			if (!VSCHUtils.isCollidingWithPlanet(nearestPlanet, shipCenter)) {
				playerMenuTick(nearestPlayer, ship, level, nearestPlanet);
				continue;
			}

			CosmosModVariables.PlayerVariables vars = VSCHUtils.getOrSetPlayerCap(nearestPlayer);
			vars.check_collision = false;
			vars.syncPlayerVariables(nearestPlayer);

			// If they don't have the menu already open,
			if (!(nearestPlayer.containerMenu instanceof LandingSelectorMenu)) {
				// Open the menu and disable normal CH collision for them:
				LOGGER.info("[Starlance]: opened menu instead of CH");

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

	public static void playerMenuTick(Player player, Ship ship, ServerLevel level, CompoundTag planet) {
		if (!(player.containerMenu instanceof LandingSelectorMenu)) {
			return;
		}
		CosmosModVariables.PlayerVariables vars = VSCHUtils.getOrSetPlayerCap(player);

		// System.out.println(vars.landing_coords);
		if (vars.landing_coords.equals("^") || vars.landing_coords.equals("=")) {
			return;
		}
		String targetDim = planet.getString("travel_to");
		final double atmoY = CosmosModVariables.WorldVariables.get(level).atmospheric_collision_data_map.getCompound(targetDim).getDouble("atmosphere_y");
		double posX = Double.parseDouble(vars.landing_coords.substring(vars.landing_coords.indexOf("*") + 1, vars.landing_coords.indexOf("|")));
		double posZ = Double.parseDouble(vars.landing_coords.substring(vars.landing_coords.indexOf("|") + 1, vars.landing_coords.indexOf("~")));
		double posY = atmoY - 10;


		if (targetDim.isEmpty()) {
			LOGGER.error("Planet has no travel_to dimension. Please report this");
			// We should in theory never get here if I've done my null checks correctly when getting the antennas in the first place
			return;
		}
		LOGGER.info("Handling teleport {} ({}) to {} {} {} {}", ship.getSlug(), ship.getId(), targetDim, posX, posY, posZ);
		TeleportationHandler handler = new TeleportationHandler(VSCHUtils.dimToLevel(ValkyrienSkiesMod.getCurrentServer(), targetDim), level, true);
		handler.handleTeleport(ship, new Vector3d(posX, posY, posZ));

		vars.landing_coords = "^";
		vars.check_collision = true;
		vars.syncPlayerVariables(player);
	}

	/**
	 * Not a util function because its very specific to planetCollisionTick
	 * Gets a random OR nearest (I'm not sure which) player that is inside the ships AABB and previous AABB.
	 * @param ship
	 * @param level
	 * @return the player found, or null
	 */
	private static Player getShipPlayer(Ship ship, ServerLevel level) {
		// Get the AABB of the last tick and the AABB of the current tick
		AABB prevWorldAABB = VectorConversionsMCKt.toMinecraft(VSCHUtils.transformToAABBd(ship.getPrevTickTransform(), ship.getShipAABB())).inflate(10);
		AABB currentWorldAABB = VectorConversionsMCKt.toMinecraft(ship.getWorldAABB()).inflate(10);

		// Combine the AABB's into one big one
		AABB totalAABB = currentWorldAABB.minmax(prevWorldAABB);

		List<Player> players = level.getEntities(EntityTypeTest.forClass(Player.class), totalAABB, EntitySelector.NO_SPECTATORS);
		// Maybe nearest player
		Player nearestPlayer = players.size() > 0 ? players.get(0) : null;
		return nearestPlayer;
	}
}
