package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.joml.Quaternionf;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {
	@Shadow
	public ServerPlayer player;

	@WrapOperation(
		method = "handleMovePlayer",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;containsInvalidValues(DDDFF)Z"
		)
	)
	public boolean handleMovePlayer$containsInvalidValues(
		final double x,
		final double y,
		final double z,
		final float yRot,
		final float xRot,
		final Operation<Boolean> operation,
		final ServerboundMovePlayerPacket packet
	) {
		if (!operation.call(x, y, z, yRot, xRot)) {
			return false;
		}
		if (!packet.hasRotation()) {
			return false;
		}
		final Quaternionf rotation = ((EntityRotationPacketAccessor)(packet)).vsch$getRotation();
		return !rotation.isFinite();
	}

	@Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;wrapDegrees(F)F"))
	public void handleMovePlayer$wrapDegrees(final ServerboundMovePlayerPacket packet, final CallbackInfo ci) {
		if (!packet.hasRotation() || !(this.player instanceof FreeRotatePlayerAccessor frp)) {
			return;
		}
		frp.vsch$setRotation(((EntityRotationPacketAccessor)(packet)).vsch$getRotation());
	}

	@ModifyExpressionValue(
		method = "teleport(DDDFFLjava/util/Set;)V",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;")
	)
	public ClientboundPlayerPositionPacket teleport$new$ClientboundPlayerPositionPacket(final ClientboundPlayerPositionPacket packet) {
		if (this.player instanceof FreeRotatePlayerAccessor frp) {
			((EntityRotationPacketAccessor)(packet)).vsch$getRotation().set(frp.vsch$getRotation());
		}
		return packet;
	}
}
