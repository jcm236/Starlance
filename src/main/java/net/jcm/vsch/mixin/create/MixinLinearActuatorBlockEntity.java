package net.jcm.vsch.mixin.create;

import net.jcm.vsch.util.assemble.IMoveable;
import net.jcm.vsch.util.assemble.MoveableIControlContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(LinearActuatorBlockEntity.class)
public abstract class MixinLinearActuatorBlockEntity extends MixinKineticBlockEntity implements IMoveable {
	@Shadow(remap = false)
	public AbstractContraptionEntity movedContraption;

	@Override
	public Object beforeMove(ServerLevel level, BlockPos origin, BlockPos target) {
		super.beforeMove(level, origin, target);
		final AbstractContraptionEntity res = this.movedContraption;
		this.movedContraption = null;
		return res;
	}

	@Override
	public void afterMove(ServerLevel level, BlockPos origin, BlockPos target, Object data) {
		super.afterMove(level, origin, target, null);
		final BlockPos offset = target.subtract(origin);
		MoveableIControlContraption.moveContraptionEntity(level, (AbstractContraptionEntity) (data), offset);
	}
}
