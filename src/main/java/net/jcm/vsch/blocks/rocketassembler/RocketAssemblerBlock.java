package net.jcm.vsch.blocks.rocketassembler;

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.ship.VSCHForceInducedShips;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class RocketAssemblerBlock extends DirectionalBlock implements EntityBlock {
	public static final EnumProperty<AssembleLED> LED = EnumProperty.create("led", AssembleLED.class);

	public RocketAssemblerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder
			.add(DirectionalBlock.FACING)
			.add(LED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction dir = ctx.getNearestLookingDirection().getOpposite();
		if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
			dir = dir.getOpposite();
		}
		return defaultBlockState()
			.setValue(DirectionalBlock.FACING, dir)
			.setValue(LED, AssembleLED.GREEN);
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
		super.neighborChanged(state, world, pos, neighbor, neighborPos, moving);
		RocketAssemblerBlockEntity be = (RocketAssemblerBlockEntity) world.getBlockEntity(pos);
		be.neighborChanged(neighbor, neighborPos, moving);
	}

	@Override
	public RocketAssemblerBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RocketAssemblerBlockEntity(pos, state);
	}

	@Override
	public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTick) : ParticleBlockEntity::serverTick;
	}
}