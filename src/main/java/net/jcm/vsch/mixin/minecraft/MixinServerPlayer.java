package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;

import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.server.level.ServerPlayer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer extends MixinPlayer {
	@ModifyArg(
		method = "dismountTo",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setPos(DDD)V"),
		index = 1
	)
	public double dismountTo$setPos$y(double y) {
		if (this.vsch$isFreeRotating()) {
			final float oldHeight = this.vsch$getVanillaDimensions(this.getPose()).height;
			final float newHeight = 0.6f;
			y += oldHeight - newHeight;
		}
		return y;
	}

	@ModifyExpressionValue(
		method = "getAddEntityPacket",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ClientboundAddPlayerPacket;")
	)
	public ClientboundAddPlayerPacket new$ClientboundAddPlayerPacket(final ClientboundAddPlayerPacket packet) {
		final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
		packetAccessor.vsch$rotation().set(this.vsch$getBodyRotation());
		packetAccessor.vsch$setHeadPitch(this.vsch$getHeadPitch());
		packetAccessor.vsch$setHeadYaw(this.vsch$getHeadYaw());
		return packet;
	}

}
