package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;
import net.jcm.vsch.mixin.minecraft.MixinPlayer;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends MixinPlayer {
	@Shadow
	public Input input;
	@Shadow
	public float yBob;
	@Shadow
	public float xBob;
	@Shadow
	public float yBobO;
	@Shadow
	public float xBobO;

	@Shadow
	protected abstract boolean isControlledCamera();

	@ModifyExpressionValue(
		method = {"tick", "sendPosition"},
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$Rot;")
	)
	public ServerboundMovePlayerPacket.Rot new$ServerboundMovePlayerPacket$Rot(final ServerboundMovePlayerPacket.Rot packet) {
		final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
		packetAccessor.vsch$rotation().set(this.vsch$getBodyRotation());
		packetAccessor.vsch$setHeadPitch(this.vsch$getHeadPitch());
		packetAccessor.vsch$setHeadYaw(this.vsch$getHeadYaw());
		return packet;
	}

	@ModifyExpressionValue(
		method = "sendPosition",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$PosRot;")
	)
	public ServerboundMovePlayerPacket.PosRot sendPosition$new$ServerboundMovePlayerPacket$PosRot(final ServerboundMovePlayerPacket.PosRot packet) {
		final EntityRotationPacketAccessor packetAccessor = ((EntityRotationPacketAccessor)(packet));
		packetAccessor.vsch$rotation().set(this.vsch$getBodyRotation());
		packetAccessor.vsch$setHeadPitch(this.vsch$getHeadPitch());
		packetAccessor.vsch$setHeadYaw(this.vsch$getHeadYaw());
		return packet;
	}

	@WrapOperation(
		method = "aiStep",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isShiftKeyDown()Z", ordinal = 0)
	)
	protected boolean aiStep$setCrouching$isShiftKeyDown(final LocalPlayer self, final Operation<Boolean> operation) {
		if (!operation.call(self)) {
			return false;
		}
		if (this.getAbilities().flying || !this.vsch$isFreeRotating()) {
			return true;
		}
		return this.onGround();
	}

	@Inject(method = "serverAiStep", at = @At("RETURN"))
	public void serverAiStep(final CallbackInfo ci) {
		if (!this.vsch$isFreeRotating() || this.getAbilities().flying || !this.isControlledCamera()) {
			return;
		}
		this.yya = this.isBodyRotationLocked() ? 0 : ((this.input.jumping ? 1 : 0) + (this.input.shiftKeyDown ? -1 : 0));
		// TODO: fix arm animation
		this.xBob = this.xBobO + Mth.wrapDegrees(this.getViewXRot(1) - this.xBobO) * 0.5f;
		this.yBob = this.yBobO + Mth.wrapDegrees(this.getViewYRot(1) - this.yBobO) * 0.5f;
		while (this.yBob > 180) {
			this.yBob -= 360;
			this.yBobO -= 360;
		}
		while (this.yBob < -180) {
			this.yBob += 360;
			this.yBobO += 360;
		}
	}

	@Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
	private void moveTowardsClosestSpace(final double x, final double z, final CallbackInfo ci) {
		if (this.vsch$isFreeRotating()) {
			ci.cancel();
			return;
		}
	}

	@Inject(method = "canStartSprinting", at = @At("HEAD"), cancellable = true)
	private void canStartSprinting(final CallbackInfoReturnable<Boolean> cir) {
		if (!this.vsch$isFreeRotating()) {
			return;
		}
		cir.setReturnValue(false);
	}
}
