package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements EntityAccessor {
	@Shadow
	protected abstract Vec3 collide(Vec3 movement);

	@Override
	public Vec3 vsch$collide(final Vec3 movement) {
		return this.collide(movement);
	}

	@Inject(method = "collide", at = @At("RETURN"), cancellable = true)
	public void collide(final Vec3 originMovement, final CallbackInfoReturnable<Vec3> cir) {
		if (!(((Object)(this)) instanceof Player player) || !(player instanceof FreeRotatePlayerAccessor frp)) {
			return;
		}
		if (!frp.vsch$shouldFreeRotate()) {
			return;
		}
		Vec3 movement = cir.getReturnValue();
		for (PartEntity<?> part : player.getParts()) {
			movement = ((EntityAccessor)(part)).vsch$collide(movement);
		}
		cir.setReturnValue(movement);
	}
}
