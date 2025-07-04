package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.VSCHEvents;
import net.jcm.vsch.accessor.ILevelAccessor;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel implements ILevelAccessor {
	@Unique
	private NodeLevel nodeLevel;

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void init(final CallbackInfo ci) {
		this.nodeLevel = new NodeLevel((Level)((Object)(this)));
	}

	@Override
	public NodeLevel vsch$getNodeLevel() {
		return this.nodeLevel;
	}

	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
	public void setBlock(final BlockPos pos, final BlockState newState, final int flags, final int maxUpdates, final CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			VSCHEvents.onBlockUpdate((Level)((Object)(this)), pos);
		}
	}
}
