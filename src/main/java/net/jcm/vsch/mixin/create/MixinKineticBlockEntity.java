package net.jcm.vsch.mixin.create;

import net.jcm.vsch.util.assemble.IMoveable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(KineticBlockEntity.class)
public abstract class MixinKineticBlockEntity implements IMoveable {
	@Shadow(remap = false)
	public BlockPos source;

	@Override
	public Object beforeMove(ServerLevel level, BlockPos origin, BlockPos target) {
		return null;
	}

	@Override
	public void afterMove(ServerLevel level, BlockPos origin, BlockPos target, Object data) {
		if (this.source != null) {
			this.source = this.source.subtract(origin).offset(target);
		}
	}
}
