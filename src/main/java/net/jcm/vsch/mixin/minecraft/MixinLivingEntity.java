/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
package net.jcm.vsch.mixin.minecraft;

// import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.accessor.LivingEntityAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Quaternionf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity implements LivingEntityAccessor {
	protected MixinLivingEntity() {
		super(null, null);
	}

	@Shadow
	protected int lerpSteps;
	@Unique
	private int tickSinceLastJump = Integer.MAX_VALUE;

	// @Shadow
	// protected abstract float getJumpPower();

	@Override
	public int vsch$getTickSinceLastJump() {
		return this.tickSinceLastJump;
	}

	// @WrapOperation(
	// 	method = "travel",
	// 	slice = @Slice(
	// 		from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;shouldDiscardFriction()Z")
	// 	),
	// 	at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 0)
	// )
	// public void travel(final LivingEntity self, double x, double y, double z, final Operation<Void> operation) {
	// 	if (self instanceof final FreeRotatePlayerAccessor frp && frp.vsch$isFreeRotating()) {
	// 		final Vec3 movement = self.getDeltaMovement();
	// 		x = movement.x;
	// 		y = movement.y;
	// 		z = movement.z;
	// 		final double inflate = 1.0 / 16;
	// 		final BlockPos pos = frp.vsch$findSupportingBlock((box) -> {
	// 			box.minX -= inflate;
	// 			box.minY -= inflate;
	// 			box.minZ -= inflate;
	// 			box.maxX += inflate;
	// 			box.maxY += inflate;
	// 			box.maxZ += inflate;
	// 		});
	// 		if (pos != null) {
	// 			// TODO: maybe check the block's friction
	// 			final double friction = 0.1;
	// 			final double inertia = 1 - friction;
	// 			x *= inertia;
	// 			if (Math.abs(x) < 1e-6) {
	// 				x = 0;
	// 			}
	// 			y *= inertia;
	// 			if (Math.abs(y) < 1e-6) {
	// 				y = 0;
	// 			}
	// 			z *= inertia;
	// 			if (Math.abs(z) < 1e-6) {
	// 				z = 0;
	// 			}
	// 		}
	// 	}
	// 	operation.call(self, x, y, z);
	// }

	@Inject(method = "aiStep", at = @At("HEAD"))
	public void aiStep(final CallbackInfo ci) {
		if (this.tickSinceLastJump < 100) {
			this.tickSinceLastJump++;
		} else {
			this.tickSinceLastJump = Integer.MAX_VALUE;
		}
	}

	// @Inject(
	// 	method = "aiStep",
	// 	slice = @Slice(
	// 		from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setXRot(F)V", ordinal = 0)
	// 	),
	// 	at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;lerpSteps:I", opcode = Opcodes.PUTFIELD)
	// )
	// public void aiStep$lerp(final CallbackInfo ci) {
	// 	if (!(this instanceof final FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
	// 		return;
	// 	}
	// 	frp.vsch$stepLerp(this.lerpSteps);
	// }

	// @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	// protected void jumpFromGround(final CallbackInfo ci) {
	// 	if (!(this instanceof final FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
	// 		return;
	// 	}
	// 	final Quaternionf rotation = frp.vsch$getBodyRotation();
	// 	final Vec3 dm0 = this.getDeltaMovement();
	// 	final Vector3d dm = rotation.transformInverse(new Vector3d(dm0.x, dm0.y, dm0.z));
	// 	dm.y = this.getJumpPower();
	// 	rotation.transform(dm);
	// 	this.setDeltaMovement(new Vec3(dm.x, dm.y, dm.z));
	// 	this.hasImpulse = true;
	// 	this.tickSinceLastJump = 0;
	// 	ci.cancel();
	// }

	@Inject(method = "jumpFromGround", at = @At("TAIL"))
	protected void jumpFromGround$tail(final CallbackInfo ci) {
		this.tickSinceLastJump = 0;
	}
}
