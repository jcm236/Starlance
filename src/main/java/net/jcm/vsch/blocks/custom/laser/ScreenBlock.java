package net.jcm.vsch.blocks.custom.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import net.jcm.vsch.blocks.entity.laser.ScreenBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;

public class ScreenBlock extends Block implements EntityBlock {
	public static final IntegerProperty LIGHT_LEVEL = BlockStateProperties.LEVEL;

	public ScreenBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(LIGHT_LEVEL, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIGHT_LEVEL);
	}

	@Override
	public ScreenBlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new ScreenBlockEntity(blockPos, blockState);
	}

	@Override
	public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTicker) : ParticleBlockEntity::serverTicker;
	}

	public static int getLightLevel(BlockState state) {
		return state.getValue(LIGHT_LEVEL);
	}
}
