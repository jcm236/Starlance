package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RemotePlayer.class)
public abstract class MixinRemotePlayer extends AbstractClientPlayer {
	protected MixinRemotePlayer() {
		super(null, null);
	}

	@Inject(
		method = "aiStep",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/RemotePlayer;setXRot(F)V", ordinal = 0)
		),
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/RemotePlayer;lerpSteps:I", opcode = Opcodes.PUTFIELD)
	)
	public void aiStep$lerp(final CallbackInfo ci) {
		if (!(this instanceof FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
			return;
		}
		frp.vsch$setRotation(frp.vsch$getRotation().nlerp(frp.vsch$getLerpRotation(), 1.0f / this.lerpSteps).normalize());
	}
}
