package net.jcm.vsch.mixin.minecraft;

import net.minecraft.server.level.ServerPlayer;

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
}
