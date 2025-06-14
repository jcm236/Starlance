package net.jcm.vsch.blocks.custom;


import net.jcm.vsch.blocks.custom.template.BlockWithEntity;
import net.jcm.vsch.blocks.entity.MagnetBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;


public class MagnetBlock extends BlockWithEntity<MagnetBlockEntity> {

	public MagnetBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(FACING);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);

		if (!(level instanceof ServerLevel)) {
			return;
		}

		// ----- Remove this block from the force appliers for the current level ----- //
		// I guess VS does this automatically when switching a shipyards dimension?
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships != null) {
			//ships.removeDragger(pos);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction dir = ctx.getNearestLookingDirection();
		if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
			dir = dir.getOpposite();
		}
		return defaultBlockState()
				.setValue(BlockStateProperties.FACING, dir);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);

		if (!(level instanceof ServerLevel)) return;

		int signal = level.getBestNeighborSignal(pos);
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);

		if (ships != null) {
			/*DraggerData data = ships.getDraggerAtPos(pos);

			if (data != null) {
				data.on = (signal > 0);
			}*/
		}
	}

	public MagnetBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MagnetBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTick) : ParticleBlockEntity::serverTick;
	}
}
