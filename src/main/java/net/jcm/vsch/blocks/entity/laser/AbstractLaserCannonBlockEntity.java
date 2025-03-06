package net.jcm.vsch.blocks.entity.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.jcm.vsch.blocks.entity.template.AbstractCannonBlockEntity;

public abstract class AbstractLaserCannonBlockEntity extends AbstractCannonBlockEntity {
	protected AbstractLaserCannonBlockEntity(String peripheralType, BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(peripheralType, type, pos, state);
	}

	@Override
	public boolean isValidPart(Direction dir, AbstractCannonBlockEntity be) {
		return dir == this.facing || dir.getOpposite() == this.facing;
	}

	@Override
	public void partChanged(Direction dir, AbstractCannonBlockEntity be) {
		//
	}
}
