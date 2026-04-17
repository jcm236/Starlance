package net.jcm.vsch.mixin.ad_astra;

import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.util.TeleportationHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;

import earth.terrarium.adastra.common.utils.ModUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModUtils.class)
public abstract class MixinModUtils {
	@Inject(method = "land", at = @At("HEAD"), cancellable = true, remap = false)
	private static void injectLand(final ServerPlayer player, final ServerLevel targetLevel, final Vec3 pos, final CallbackInfo ci) {
		if (!(player instanceof final IEntityDraggingInformationProvider dragged)) {
			return;
		}
		final ServerLevel oldLevel = player.serverLevel();
		final Long id = dragged.getDraggingInformation().getLastShipStoodOn();
		if (id == null) {
			return;
		}
		final LoadedServerShip serverShip = VSGameUtilsKt.getShipObjectWorld(oldLevel).getLoadedShips().getById(id);
		if (serverShip == null || !serverShip.getChunkClaimDimension().equals(VSGameUtilsKt.getDimensionId(oldLevel))) {
			return;
		}
		final Vector3dc newVelocity = new Vector3d(0.0, -10.0, 0.0);

		final TeleportationHandler handler = new TeleportationHandler(oldLevel, targetLevel, false);
		handler.addShipWithVelocity(serverShip, new Vector3d(pos.x, pos.y, pos.z), serverShip.getTransform().getRotation(), newVelocity, null);
		for (final LoadedServerShip ship : handler.getPendingShips()) {
			final ShipLandingAttachment attachment = ShipLandingAttachment.get(ship);
			attachment.setLanding();
		}
		handler.finalizeTeleport();
		ci.cancel();
	}
}
