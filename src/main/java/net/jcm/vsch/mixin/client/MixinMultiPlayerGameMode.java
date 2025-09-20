package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
	@ModifyExpressionValue(
		method = "useItem",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$PosRot;")
	)
	public ServerboundMovePlayerPacket.PosRot useItem$new$ServerboundMovePlayerPacket$PosRot(final ServerboundMovePlayerPacket.PosRot packet, @Local final Player player) {
		if (player instanceof final FreeRotatePlayerAccessor frp) {
			final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
			packetAccessor.vsch$rotation().set(frp.vsch$getBodyRotation());
			packetAccessor.vsch$setHeadPitch(frp.vsch$getHeadPitch());
			packetAccessor.vsch$setHeadYaw(frp.vsch$getHeadYaw());
		}
		return packet;
	}
}
