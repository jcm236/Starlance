package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	@Inject(method = "setOldPosAndRot", at = @At("RETURN"))
	private void setOldPosAndRot(final CallbackInfo ci) {
		if (((Object)(this)) instanceof final FreeRotatePlayerAccessor frp) {
			frp.vsch$setOldPosAndRot();
		}
	}
}
