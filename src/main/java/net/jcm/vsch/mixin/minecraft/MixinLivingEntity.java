package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
	protected MixinLivingEntity() {
		super(null, null);
	}

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
}
