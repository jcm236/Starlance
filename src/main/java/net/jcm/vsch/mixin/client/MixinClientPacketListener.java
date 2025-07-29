package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.ClientboundPlayerPositionPacketAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.accessor.ServerboundMovePlayerPacketAccessor;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
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
		method = "handleMovePlayer",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundAcceptTeleportationPacket;")
	)
	public void handleMovePlayer$acceptTeleport$before(final ClientboundPlayerPositionPacket packet, final CallbackInfo ci, @Local final Player player) {
		if (!(player instanceof FreeRotatePlayerAccessor frp)) {
			return;
		}
		frp.vsch$setRotation(((ClientboundPlayerPositionPacketAccessor)(packet)).vsch$getRotation());
	}

	@ModifyExpressionValue(
		method = "handleMovePlayer",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$PosRot;")
	)
	public ServerboundMovePlayerPacket.PosRot handleMovePlayer$new$ServerboundMovePlayerPacket$PosRot(final ServerboundMovePlayerPacket.PosRot packet, @Local final Player player) {
		if (player instanceof FreeRotatePlayerAccessor frp) {
			((ServerboundMovePlayerPacketAccessor)(packet)).vsch$getRotation().set(frp.vsch$getRotation());
		}
		return packet;
	}
}
