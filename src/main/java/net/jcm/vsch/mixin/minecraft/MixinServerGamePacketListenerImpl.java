package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.joml.Vector3d;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
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
		final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
		return !packetAccessor.vsch$rotation().isFinite() ||
			!Double.isFinite(packetAccessor.vsch$getHeadPitch()) ||
			!Double.isFinite(packetAccessor.vsch$getHeadYaw());
	}

	@Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;wrapDegrees(F)F", ordinal = 0))
	public void handleMovePlayer$wrapDegrees(final ServerboundMovePlayerPacket packet, final CallbackInfo ci) {
		if (!packet.hasRotation() || !(this.player instanceof FreeRotatePlayerAccessor frp)) {
			return;
		}
		final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
		frp.vsch$setBodyRotation(packetAccessor.vsch$rotation());
		frp.vsch$setHeadPitch(packetAccessor.vsch$getHeadPitch());
		frp.vsch$setHeadYaw(packetAccessor.vsch$getHeadYaw());
	}

	@WrapOperation(
		method = "handleMovePlayer",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getBoundingBox()Lnet/minecraft/world/phys/AABB;"),
			to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;jumpFromGround()V")
		),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;onGround()Z"
		)
	)
	public boolean handleMovePlayer$jumpFromGround(
		final ServerPlayer player,
		final Operation<Boolean> operation,
		@Local(argsOnly = true) final ServerboundMovePlayerPacket packet,
		@Local final LocalBooleanRef jumped
	) {
		if (!operation.call(player)) {
			return false;
		}
		if (!(player instanceof final FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
			return true;
		}
		final Vector3d movement = new Vector3d(packet.getX(player.getX()), packet.getY(player.getY()), packet.getZ(player.getZ()));
		frp.vsch$getBodyRotation().transformInverse(movement);
		final boolean movedUp = movement.y > 0;
		jumped.set(movedUp);
		if (!movedUp) {
			return false;
		}
		if (packet.isOnGround()) {
			return false;
		}
		player.jumpFromGround();
		return false;
	}

	@ModifyExpressionValue(
		method = "teleport(DDDFFLjava/util/Set;)V",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;")
	)
	public ClientboundPlayerPositionPacket teleport$new$ClientboundPlayerPositionPacket(final ClientboundPlayerPositionPacket packet) {
		if (this.player instanceof FreeRotatePlayerAccessor frp) {
			final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
			packetAccessor.vsch$rotation().set(frp.vsch$getBodyRotation());
			packetAccessor.vsch$setHeadPitch(frp.vsch$getHeadPitch());
			packetAccessor.vsch$setHeadYaw(frp.vsch$getHeadYaw());
		}
		return packet;
	}
}
