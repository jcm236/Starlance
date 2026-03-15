package net.jcm.vsch.mixin.ad_astra;

import net.jcm.vsch.ship.ShipTierAttachment;
import net.jcm.vsch.spacemods.ad_astra.ClientValues;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;

import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.menus.PlanetsMenu;
import earth.terrarium.adastra.common.registry.ModEntityTypes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlanetsMenu.class)
public class MixinPlanetsMenu {
	@WrapOperation(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Ljava/util/Set;Ljava/util/Map;Lit/unimi/dsi/fastutil/objects/Object2BooleanMap;Ljava/util/Set;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getVehicle()Lnet/minecraft/world/entity/Entity;"
		)
	)
	private Entity player$getVehicle(
		final Player player,
		final Operation<Entity> operation,
		final @Share("shipTier") LocalIntRef shipTier
	) {
		final Level level = player.level();
		if (level.isClientSide) {
			if (ClientValues.storedTier == null) {
				return operation.call(player);
			}
			shipTier.set(ClientValues.storedTier);
		} else {
			if (!(player instanceof final IEntityDraggingInformationProvider dragged)) {
				return operation.call(player);
			}

			final Long id = dragged.getDraggingInformation().getLastShipStoodOn();
			if (id == null) {
				return operation.call(player);
			}

			final ServerLevel serverLevel = (ServerLevel) level;
			final LoadedServerShip serverShip = VSGameUtilsKt.getShipObjectWorld(serverLevel).getLoadedShips().getById(id);
			if (serverShip == null || !serverShip.getChunkClaimDimension().equals(VSGameUtilsKt.getDimensionId(serverLevel))) {
				return operation.call(player);
			}

			final ShipTierAttachment tierAttachment = ShipTierAttachment.get(serverShip);
			shipTier.set(tierAttachment.getHighestTier());
		}

		// Have to be an instance of Rocket to bypass a check
		return ModEntityTypes.TIER_1_ROCKET.get().create(player.level());
	}

	@WrapOperation(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Ljava/util/Set;Ljava/util/Map;Lit/unimi/dsi/fastutil/objects/Object2BooleanMap;Ljava/util/Set;)V",
		at = @At(
			value = "INVOKE",
			target = "Learth/terrarium/adastra/common/entities/vehicles/Rocket;tier()I",
			remap = false
		),
		remap = false
	)
	private int rocket$getTier(
		final Rocket instance,
		final Operation<Integer> original,
		final @Share("shipTier") LocalIntRef shipTierRef
	) {
		final int shipTier = shipTierRef.get();
		return shipTier != 0 ? shipTier : original.call(instance);
	}
}
