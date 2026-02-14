package net.jcm.vsch.mixin.ad_astra;

import earth.terrarium.adastra.common.utils.ModUtils;
import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.util.TaskUtil;
import net.jcm.vsch.util.TeleportationHandler;
import net.jcm.vsch.util.VSCHUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.EntityDraggingInformation;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(ModUtils.class)
public abstract class MixinModUtils {
    @Shadow
    public abstract Entity teleportToDimension(Entity entity, ServerLevel level);

    @Inject(
            method = "land",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void injectLand(ServerPlayer player, ServerLevel targetLevel, Vec3 pos, CallbackInfo ci) {
        if (player instanceof IEntityDraggingInformationProvider dragged) {
            Long id = dragged.getDraggingInformation().getLastShipStoodOn();
            if (id == null) return;
            LoadedServerShip serverShip = VSCHUtils.getLoadedShipsInLevel(player.serverLevel()).stream().filter((s) -> s.getId() == id).findAny().orElse(null);
            if (serverShip == null) return;
            final ShipLandingAttachment landingAttachment = ShipLandingAttachment.get(serverShip);
            landingAttachment.landing = true;

            TeleportationHandler handler = new TeleportationHandler(player.serverLevel(), targetLevel, false);
            handler.addShipWithVelocity(serverShip, VectorConversionsMCKt.toJOML(pos), serverShip.getTransform().getRotation(), new Vector3d(0.0, -10.0, 0.0), serverShip.getAngularVelocity());
            handler.afterShipsAdded().thenAcceptAsync((void_) -> {
                handler.finalizeTeleport();
            }, targetLevel.getServer());
            ci.cancel();
        }
    }
}
