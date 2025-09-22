package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;
import org.joml.Quaternionf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
	protected MixinLivingEntity() {
		super(null, null);
	}

	@Shadow
	protected int lerpSteps;

	@Shadow
	protected abstract float getJumpPower();

	@WrapOperation(
		method = "travel",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;shouldDiscardFriction()Z")
		),
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 0)
	)
	public void travel(final LivingEntity self, double x, double y, double z, final Operation<Void> operation) {
		if (self instanceof FreeRotatePlayerAccessor frp && frp.vsch$hasSupportingBlock()) {
			x *= 0.91;
			y *= 0.91;
			z *= 0.91;
		}
		operation.call(self, x, y, z);
	}

	@Inject(
		method = "aiStep",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setXRot(F)V", ordinal = 0)
		),
		at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;lerpSteps:I", opcode = Opcodes.PUTFIELD)
	)
	public void aiStep$lerp(final CallbackInfo ci) {
		if (!(this instanceof final FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
			return;
		}
		frp.vsch$stepLerp(this.lerpSteps);
	}

	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	protected void jumpFromGround(final CallbackInfo ci) {
		if (!(this instanceof final FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
			return;
		}
		final Quaternionf rotation = frp.vsch$getBodyRotation();
		final Vec3 dm0 = this.getDeltaMovement();
		final Vector3d dm = rotation.transformInverse(new Vector3d(dm0.x, dm0.y, dm0.z));
		dm.y = this.getJumpPower();
		rotation.transform(dm);
		this.setDeltaMovement(new Vec3(dm.x, dm.y, dm.z));
		this.hasImpulse = true;
	}
}
