package net.jcm.vsch.blocks.custom.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.jcm.vsch.blocks.entity.laser.LaserEmitterBlockEntity;
import net.jcm.vsch.blocks.entity.template.IColoredBlockEntity;

public class LaserEmitterBlock extends LaserCannonBlock<LaserEmitterBlockEntity> {
	public LaserEmitterBlock(BlockBehaviour.Properties properties) {
		super(properties, LaserEmitterBlockEntity::new);
	}

	@Override
	public InteractionResult use(
			BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand,
			BlockHitResult hit
	) {
		return IColoredBlockEntity.onUse(state, level, pos, player, hand, hit, true);
	}
}
