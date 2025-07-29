package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements EntityAccessor {
	@Shadow
	public abstract Level level();

	@Shadow
	protected abstract Vec3 collide(Vec3 movement);

	@Shadow
	protected abstract void checkInsideBlocks();

	@Shadow
	protected abstract void onInsideBlock(BlockState block);

	@Override
	public Vec3 vsch$collide(final Vec3 movement) {
		return this.collide(movement);
	}

	@Override
	public void vsch$checkInsideBlocks() {
		this.checkInsideBlocks();
	}

	@Override
	public void vsch$onInsideBlock(final BlockState block) {
		this.onInsideBlock(block);
	}

	@Inject(method = "collide", at = @At("RETURN"), cancellable = true)
	private void collide(final Vec3 originMovement, final CallbackInfoReturnable<Vec3> cir) {
		if (!(((Object)(this)) instanceof Player player) || !(player instanceof FreeRotatePlayerAccessor frp)) {
			return;
		}
		if (!frp.vsch$isFreeRotating()) {
			return;
		}
		Vec3 movement = cir.getReturnValue();
		for (PartEntity<?> part : player.getParts()) {
			movement = EntityShipCollisionUtils.INSTANCE.adjustEntityMovementForShipCollisions(part, movement, part.getBoundingBox(), this.level());
			movement = ((EntityAccessor)(part)).vsch$collide(movement);
		}
		cir.setReturnValue(movement);
	}

	@Inject(method = "setOldPosAndRot", at = @At("RETURN"))
	private void setOldPosAndRot(final CallbackInfo ci) {
		if (((Object)(this)) instanceof FreeRotatePlayerAccessor frp) {
			frp.vsch$setRotationO(frp.vsch$getRotationO().set(frp.vsch$getRotation()));
		}
	}
}
