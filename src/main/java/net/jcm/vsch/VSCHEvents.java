package net.jcm.vsch;

import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.event.AsteroidGenerator;
import net.jcm.vsch.event.AtmosphericCollision;
import net.jcm.vsch.event.GravityInducer;
import net.jcm.vsch.event.PlanetCollision;
import net.lointain.cosmos.network.CosmosModVariables;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class VSCHEvents {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		final boolean generateAsteroid = VSCHConfig.GENERATE_ASTEROID.get();
		for (ServerLevel level : event.getServer().getAllLevels()) {
			if (level.getPlayers(player -> true, 1).isEmpty()) {
				// skip if the no player is in the world
				// TODO: maybe we'll have automated ships in the future and this need to be removed?
				continue;
			}
			AtmosphericCollision.atmosphericCollisionTick(level);
			PlanetCollision.planetCollisionTick(level);
			if (generateAsteroid) {
				AsteroidGenerator.tickLevel(level);
			}
		}
	}

	@SubscribeEvent
	public static void onServerStart(ServerStartedEvent event) {
		GravityInducer.all_gravity_data = CosmosModVariables.WorldVariables.get(event.getServer().overworld()).gravity_data;
	}

	// For next vs update
	//	@SubscribeEvent
	//	public static void shipLoad(VSEvents.ShipLoadEvent event) {
	////		Gravity.setAll(event.getServer().overworld());
	//	}

	@SubscribeEvent
	public static void onBlockPlace(PlayerInteractEvent.RightClickBlock event) {
		final Item item = event.getItemStack().getItem();
		if (!(item instanceof BlockItem)) {
			return;
		}
		final Level level = event.getLevel();
		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, event.getPos());
		if (AsteroidGenerator.isAsteroidShip(ship)) {
			final Player player = event.getEntity();
			if (level.isClientSide) {
				player.displayClientMessage(Component.translatable("vsch.message.asteroid.place").withStyle(ChatFormatting.RED), true);
			}
			event.setCanceled(true);
		}
	}
}
