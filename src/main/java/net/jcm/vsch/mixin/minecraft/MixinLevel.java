package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.ILevelAccessor;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
