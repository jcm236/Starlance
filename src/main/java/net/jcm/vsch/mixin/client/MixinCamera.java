package net.jcm.vsch.mixin.client;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;

import org.joml.Quaternionf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
	@Shadow
	private Entity entity;

	@Shadow
	@Final
	private Quaternionf rotation;

	@Inject(
		method = "setup",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0)
	)
	public void setup(
		final BlockGetter level,
		final Entity entity,
		final boolean isThirdPerson,
		final boolean isMirrored,
		final float partialTick,
		final CallbackInfo ci
	) {
		if (!(entity instanceof final FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
			return;
		}
		frp.vsch$getHeadRotationO().slerp(frp.vsch$getHeadRotation(), partialTick, this.rotation);
		if (isThirdPerson && isMirrored) {
			this.rotation.rotateY((float)(Math.PI));
		}
	}

	@WrapOperation(
		method = "setRotation",
		at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", remap = false)
	)
	private Quaternionf setRotation$rotationYXZ(final Quaternionf rotation, final float y, final float x, final float z, final Operation<Quaternionf> operation) {
		if (this.entity instanceof final FreeRotatePlayerAccessor frp && frp.vsch$isFreeRotating()) {
			return rotation;
		}
		return operation.call(rotation, y, x, z);
	}
}
