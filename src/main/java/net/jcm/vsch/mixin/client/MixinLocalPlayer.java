package net.jcm.vsch.mixin.client;

import net.jcm.vsch.mixin.minecraft.MixinPlayer;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;

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

	@WrapOperation(
		method = "aiStep",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isShiftKeyDown()Z", ordinal = 0)
	)
	protected boolean aiStep$setCrouching$isShiftKeyDown(final LocalPlayer self, final Operation operation) {
		if (((Boolean)(operation.call(self))) == Boolean.FALSE) {
			return false;
		}
		if (this.getAbilities().flying || !this.vsch$shouldFreeRotate()) {
			return true;
		}
		return this.onGround();
	}

	@Inject(
		method = "aiStep",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;jumpableVehicle()Lnet/minecraft/world/entity/PlayerRideableJumping;",
			ordinal = 0
		)
	)
	public void aiStep$afterFlying(final CallbackInfo ci) {
		if (!this.vsch$shouldFreeRotate() || this.getAbilities().flying || !this.isControlledCamera()) {
			return;
		}
		int direction = 0;
		if (this.input.shiftKeyDown) {
			direction--;
		}
		if (this.input.jumping) {
			direction++;
		}
		if (direction != 0) {
			this.setDeltaMovement(this.getDeltaMovement().add(0, direction * this.getFlyingSpeed(), 0));
		}
	}
}
