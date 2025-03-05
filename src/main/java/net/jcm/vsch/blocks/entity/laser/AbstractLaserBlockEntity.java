package net.jcm.vsch.blocks.entity.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.jcm.vsch.blocks.entity.template.AbstractCannonBlockEntity;

public abstract class AbstractLaserBlockEntity extends AbstractCannonBlockEntity {
	protected AbstractLaserBlockEntity(String peripheralType, BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(peripheralType, type, pos, state);
	}
}
