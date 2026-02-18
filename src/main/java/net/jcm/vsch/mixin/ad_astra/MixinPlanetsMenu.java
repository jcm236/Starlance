package net.jcm.vsch.mixin.ad_astra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.menus.PlanetsMenu;
import earth.terrarium.adastra.common.registry.ModEntityTypes;
import net.jcm.vsch.ship.ShipTierAttachment;
import net.jcm.vsch.spacemods.ad_astra.ClientValues;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;

@Mixin(PlanetsMenu.class)
public class MixinPlanetsMenu {

    @Unique
    private int shipTier = 0;

    @Unique
    private boolean fakeRocket = false;

    @WrapOperation(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Ljava/util/Set;Ljava/util/Map;Lit/unimi/dsi/fastutil/objects/Object2BooleanMap;Ljava/util/Set;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getVehicle()Lnet/minecraft/world/entity/Entity;")
    )
    private Entity injectVehicle(Player instance, Operation<Entity> original) {

        if (instance.level().isClientSide) {
            if (ClientValues.storedTier == null) return original.call(instance);
            shipTier = ClientValues.storedTier;
        } else {
            if (!(instance instanceof IEntityDraggingInformationProvider dragged)) return original.call(instance);

            Long id = dragged.getDraggingInformation().getLastShipStoodOn();
            if (id == null) return original.call(instance);

            LoadedServerShip serverShip = VSCHUtils.getLoadedShipsInLevel((ServerLevel) instance.level()).stream().filter((s) -> s.getId() == id).findAny().orElse(null);
            if (serverShip == null) return original.call(instance);

            final ShipTierAttachment tierAttachment = ShipTierAttachment.get(serverShip);
            shipTier = tierAttachment.getHighestTier();
        }

        fakeRocket = true;

        // Just needs to be an instance of Rocket to bypass a check
        return ModEntityTypes.TIER_1_ROCKET.get().create(instance.level());
    }

    @WrapOperation(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Ljava/util/Set;Ljava/util/Map;Lit/unimi/dsi/fastutil/objects/Object2BooleanMap;Ljava/util/Set;)V",
            at = @At(value = "INVOKE", target = "Learth/terrarium/adastra/common/entities/vehicles/Rocket;tier()I", remap = false),
            remap = false
    )
    private int injectTier(Rocket instance, Operation<Integer> original) {
        if (!fakeRocket) return original.call(instance);

        return shipTier;
    }

}
