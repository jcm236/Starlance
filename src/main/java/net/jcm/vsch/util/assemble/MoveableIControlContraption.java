package net.jcm.vsch.util.assemble;

import net.jcm.vsch.accessor.ContraptionHolder;
import net.jcm.vsch.accessor.ControlledContraptionEntityAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import java.util.List;
import java.util.stream.Collectors;

public class MoveableIControlContraption implements IMoveable<List<AbstractContraptionEntity>> {
	public static final MoveableIControlContraption INSTANCE = new MoveableIControlContraption();

	private MoveableIControlContraption() {}

	@Override
	public List<AbstractContraptionEntity> beforeMove(ServerLevel level, BlockPos origin, BlockPos target) {
		final BlockEntity be = level.getBlockEntity(origin);
		if (be instanceof ContraptionHolder holder) {
			return holder.starlance$clearContraptions();
		}
		return null;
	}

	@Override
	public void afterMove(ServerLevel level, BlockPos origin, BlockPos target, List<AbstractContraptionEntity> data) {
		final BlockEntity be = level.getBlockEntity(target);
		final Vec3i offset = target.subtract(origin);
		data = data.stream().map(entity -> moveContraptionEntity(level, entity, offset)).collect(Collectors.toList());
		if (be instanceof ContraptionHolder holder) {
			holder.starlance$restoreContraptions(data);
		}
	}

	public static AbstractContraptionEntity moveContraptionEntity(final ServerLevel level, final AbstractContraptionEntity entity, final Vec3i offset) {
		final Contraption contraption = entity.getContraption();
		contraption.anchor = contraption.anchor.offset(offset);
		entity.setPos(entity.position().add(offset.getX(), offset.getY(), offset.getZ()));
		if (entity instanceof ControlledContraptionEntityAccessor ccea) {
			ccea.starlance$setControllerPos(ccea.starlance$getControllerPos().offset(offset));
		}
		final AbstractContraptionEntity newEntity = (AbstractContraptionEntity) (entity.getType().create(level));
		newEntity.restoreFrom(entity);
		entity.getPassengers().forEach((passenger) -> {
			final Integer seat = contraption.getSeatMapping().get(passenger.getUUID());
			if (seat != null) {
				newEntity.addSittingPassenger(passenger, seat);
			}
		});
		entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		level.addFreshEntity(newEntity);
		return newEntity;
	}
}
