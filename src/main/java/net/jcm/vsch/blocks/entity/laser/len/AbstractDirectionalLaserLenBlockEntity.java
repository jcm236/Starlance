package net.jcm.vsch.blocks.entity.laser.len;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public abstract class AbstractDirectionalLaserLenBlockEntity extends AbstractLaserLenBlockEntity {
	public AbstractDirectionalLaserLenBlockEntity(
		BlockEntityType<? extends AbstractLaserLenBlockEntity> type,
		BlockPos pos,
		BlockState state
	) {
		super(type, pos, state);
	}

	public Direction getFacing() {
		return this.getBlockState().getValue(DirectionalBlock.FACING);
	}

	public Vector3d getPanelNormal() {
		final Vec3i facing = this.getFacing().getNormal();
		final Vector3d direction = new Vector3d(facing.getX(), facing.getY(), facing.getZ());
		final Ship ship = VSGameUtilsKt.getShipManagingPos(this.getLevel(), this.getBlockPos());
		if (ship != null) {
			ship.getShipToWorld().transformDirection(direction);
		}
		return direction;
	}
}
