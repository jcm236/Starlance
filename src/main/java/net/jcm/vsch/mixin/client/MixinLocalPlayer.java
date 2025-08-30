package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.EntityRotationPacketAccessor;
import net.jcm.vsch.mixin.minecraft.MixinPlayer;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends MixinPlayer {
	@Shadow
	public Input input;

	@Shadow
	protected abstract boolean isControlledCamera();

	@ModifyExpressionValue(
		method = {"tick", "sendPosition"},
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$Rot;")
	)
	public ServerboundMovePlayerPacket.Rot new$ServerboundMovePlayerPacket$Rot(final ServerboundMovePlayerPacket.Rot packet) {
		((EntityRotationPacketAccessor)(packet)).vsch$rotation().set(this.vsch$getRotation());
		return packet;
	}

	@ModifyExpressionValue(
		method = "sendPosition",
		at = @At(value = "NEW", target = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$PosRot;")
	)
	public ServerboundMovePlayerPacket.PosRot sendPosition$new$ServerboundMovePlayerPacket$PosRot(final ServerboundMovePlayerPacket.PosRot packet) {
		((EntityRotationPacketAccessor)(packet)).vsch$rotation().set(this.vsch$getRotation());
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
		this.yya = (this.input.jumping ? 1 : 0) + (this.input.shiftKeyDown ? -1 : 0);
	}
}
