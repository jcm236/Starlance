package net.jcm.vsch.mixin.ad_astra;

import net.jcm.vsch.ship.ShipLandingAttachment;
import net.jcm.vsch.util.TaskUtil;
import net.jcm.vsch.util.TeleportationHandler;
import net.jcm.vsch.util.VSCHUtils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.EntityDraggingInformation;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;

import earth.terrarium.adastra.common.utils.ModUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
		// TODO: use proper VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips() instead
		final LoadedServerShip serverShip = VSCHUtils.getLoadedShipsInLevel(oldLevel).stream().filter((s) -> s.getId() == id).findAny().orElse(null);
		if (serverShip == null) {
			return;
		}
		final Vector3dc newVelocity = new Vector3d(0.0, -10.0, 0.0);

		final ShipLandingAttachment landingAttachment = ShipLandingAttachment.get(serverShip);
		// Since the ship is freezed, velocity in the attachment will be used regladless of addShipWithVelocity's param
		// TODO: should this be the intended behaviour?
		landingAttachment.velocity = newVelocity;

		final TeleportationHandler handler = new TeleportationHandler(oldLevel, targetLevel, false);
		handler.addShipWithVelocity(serverShip, new Vector3d(pos.x, pos.y, pos.z), serverShip.getTransform().getRotation(), newVelocity, serverShip.getAngularVelocity());
		handler.afterShipsAdded().thenAcceptAsync((void_) -> {
			for (final LoadedServerShip ship : handler.getPendingShips()) {
				final ShipLandingAttachment attachment = ShipLandingAttachment.get(ship);
				attachment.frozen = false;
				attachment.setLanding();
			}
			handler.finalizeTeleport();
		}, targetLevel.getServer());
		ci.cancel();
	}
}
