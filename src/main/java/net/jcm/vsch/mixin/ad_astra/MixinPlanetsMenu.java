package net.jcm.vsch.mixin.ad_astra;

import net.jcm.vsch.ship.ShipTierAttachment;
import net.jcm.vsch.spacemods.ad_astra.ClientValues;
import net.jcm.vsch.util.VSCHUtils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;

import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.menus.PlanetsMenu;
import earth.terrarium.adastra.common.registry.ModEntityTypes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlanetsMenu.class)
public class MixinPlanetsMenu {
	// TODO: replace the fields with @Shared LocalRef
	@Unique
	private int shipTier = 0;
	@Unique
	private boolean fakeRocket = false;

	@WrapOperation(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Ljava/util/Set;Ljava/util/Map;Lit/unimi/dsi/fastutil/objects/Object2BooleanMap;Ljava/util/Set;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getVehicle()Lnet/minecraft/world/entity/Entity;"
		)
	)
	private Entity injectVehicle(final Player player, final Operation<Entity> operation) {
		if (player.level().isClientSide) {
			if (ClientValues.storedTier == null) {
				return operation.call(player);
			}
			this.shipTier = ClientValues.storedTier;
		} else {
			if (!(player instanceof final IEntityDraggingInformationProvider dragged)) {
				return operation.call(player);
			}

			final Long id = dragged.getDraggingInformation().getLastShipStoodOn();
			if (id == null) {
				return operation.call(player);
			}

			// TODO: use proper ship Query API
			final LoadedServerShip serverShip = VSCHUtils.getLoadedShipsInLevel((ServerLevel) player.level()).stream().filter((s) -> s.getId() == id).findAny().orElse(null);
			if (serverShip == null) {
				return operation.call(player);
			}

			final ShipTierAttachment tierAttachment = ShipTierAttachment.get(serverShip);
			this.shipTier = tierAttachment.getHighestTier();
		}

		this.fakeRocket = true;

		// Have to be an instance of Rocket to bypass a check
		return ModEntityTypes.TIER_1_ROCKET.get().create(player.level());
	}

	@WrapOperation(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Ljava/util/Set;Ljava/util/Map;Lit/unimi/dsi/fastutil/objects/Object2BooleanMap;Ljava/util/Set;)V",
		at = @At(value = "INVOKE", target = "Learth/terrarium/adastra/common/entities/vehicles/Rocket;tier()I", remap = false),
		remap = false
	)
	private int injectTier(Rocket instance, Operation<Integer> original) {
		if (this.fakeRocket) {
			return this.shipTier;
		}
		return original.call(instance);
	}
}
