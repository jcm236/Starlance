package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
	@Inject(
		method = "handleAddPlayer",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/player/RemotePlayer;setOldPosAndRot()V"
		)
	)
	public void handleAddPlayer(final ClientboundAddPlayerPacket packet, final CallbackInfo ci, @Local final RemotePlayer player) {
		if (!(player instanceof final FreeRotatePlayerAccessor frp)) {
			return;
		}
		final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
		frp.vsch$setBodyRotation(packetAccessor.vsch$rotation());
		frp.vsch$setHeadPitch(packetAccessor.vsch$getHeadPitch());
		frp.vsch$setHeadYaw(packetAccessor.vsch$getHeadYaw());
	}

	@Inject(method = "handleTeleportEntity", at = @At("RETURN"))
	public void handleTeleportEntity(final ClientboundTeleportEntityPacket packet, final CallbackInfo ci, @Local final Entity entity) {
		if (!(entity instanceof final FreeRotatePlayerAccessor frp)) {
			return;
		}
		final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
		frp.vsch$setLerpBodyRotation(packetAccessor.vsch$rotation());
		frp.vsch$setLerpHeadPitch(packetAccessor.vsch$getHeadPitch());
		frp.vsch$setLerpHeadYaw(packetAccessor.vsch$getHeadYaw());
	}

	@Inject(method = "handleMoveEntity", at = @At("RETURN"))
	public void handleMoveEntity(final ClientboundMoveEntityPacket packet, final CallbackInfo ci, @Local final Entity entity) {
		if (entity == null || entity.isControlledByLocalInstance()) {
			return;
		}
		if (!(entity instanceof final FreeRotatePlayerAccessor frp)) {
			return;
		}
		if (packet.hasRotation()) {
			final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
			frp.vsch$setLerpBodyRotation(packetAccessor.vsch$rotation());
			frp.vsch$setLerpHeadPitch(packetAccessor.vsch$getHeadPitch());
			frp.vsch$setLerpHeadYaw(packetAccessor.vsch$getHeadYaw());
		}
	}

	@Inject(
		method = "handleMovePlayer",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundAcceptTeleportationPacket;")
	)
	public void handleMovePlayer$acceptTeleport$before(final ClientboundPlayerPositionPacket packet, final CallbackInfo ci, @Local final Player player) {
		if (!(player instanceof final FreeRotatePlayerAccessor frp)) {
			return;
		}
		final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
		frp.vsch$setBodyRotation(packetAccessor.vsch$rotation());
		frp.vsch$setHeadPitch(packetAccessor.vsch$getHeadPitch());
		frp.vsch$setHeadYaw(packetAccessor.vsch$getHeadYaw());
	}

	@ModifyExpressionValue(
		method = "handleMovePlayer",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$PosRot;")
	)
	public ServerboundMovePlayerPacket.PosRot handleMovePlayer$new$ServerboundMovePlayerPacket$PosRot(final ServerboundMovePlayerPacket.PosRot packet, @Local final Player player) {
		if (player instanceof final FreeRotatePlayerAccessor frp) {
			final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
			packetAccessor.vsch$rotation().set(frp.vsch$getBodyRotation());
			packetAccessor.vsch$setHeadPitch(frp.vsch$getHeadPitch());
			packetAccessor.vsch$setHeadYaw(frp.vsch$getHeadYaw());
		}
		return packet;
	}
}
