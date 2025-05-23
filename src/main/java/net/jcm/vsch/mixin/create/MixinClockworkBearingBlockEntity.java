package net.jcm.vsch.mixin.create;

import net.jcm.vsch.util.assemble.IMoveable;
import net.jcm.vsch.util.assemble.MoveableIControlContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Pseudo
@Mixin(ClockworkBearingBlockEntity.class)
public abstract class MixinClockworkBearingBlockEntity extends MixinKineticBlockEntity implements IMoveable {
	@Shadow(remap = false)
	protected ControlledContraptionEntity hourHand;

	@Shadow(remap = false)
	protected ControlledContraptionEntity minuteHand;

	@Override
	public Object beforeMove(ServerLevel level, BlockPos origin, BlockPos target) {
		super.beforeMove(level, origin, target);
		final List<ControlledContraptionEntity> res =
			this.hourHand == null
				? this.minuteHand == null ? List.of() : List.of(this.minuteHand)
				: this.minuteHand == null ? List.of(this.hourHand) : List.of(this.hourHand, this.minuteHand);
		this.hourHand = null;
		this.minuteHand = null;
		return res;
	}

	@Override
	public void afterMove(ServerLevel level, BlockPos origin, BlockPos target, Object data) {
		super.afterMove(level, origin, target, null);
		final BlockPos offset = target.subtract(origin);
		for (final ControlledContraptionEntity entity : (List<ControlledContraptionEntity>) (data)) {
			MoveableIControlContraption.moveContraptionEntity(level, entity, offset);
		}
	}
}
