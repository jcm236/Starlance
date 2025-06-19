package net.jcm.vsch.blocks.custom;

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.blocks.thruster.AbstractThrusterBlockEntity;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.jcm.vsch.util.rot.DirectionalShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaseThrusterBlock<T extends AbstractThrusterBlockEntity> extends DirectionalBlock implements EntityBlock {

	public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

	private final DirectionalShape shape;
	private final BlockEntityType.BlockEntitySupplier<? extends T> blockEntitySupplier;

	public BaseThrusterBlock(
		final Properties properties,
		final DirectionalShape shape,
		final BlockEntityType.BlockEntitySupplier<? extends T> blockEntitySupplier
	) {
		super(properties);
		this.shape = shape;
		this.blockEntitySupplier = blockEntitySupplier;
		registerDefaultState(defaultBlockState()
			.setValue(FACING, Direction.NORTH)
			.setValue(LIT, Boolean.valueOf(false))
		);
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		builder.add(LIT);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	// Should run whenever the block state is changed, but not when the block itself is replaced
	//TODO: See if we can move setting the force inducer to here instead of all over the place (would require some extra stuff for redstone change tho)
	// Disabling this didn't fix Jade :(
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape.get(state.getValue(BlockStateProperties.FACING));
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);

		if (!(level instanceof ServerLevel)) {
			return;
		}

		// ----- Remove the thruster from the force appliers for the current level ----- //
		// I guess VS does this automatically when switching a shipyards dimension?
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships != null) {
			ships.removeThruster(pos);
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

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
		super.neighborChanged(state, world, pos, neighbor, neighborPos, moving);
		AbstractThrusterBlockEntity be = (AbstractThrusterBlockEntity) world.getBlockEntity(pos);
		be.neighborChanged(neighbor, neighborPos, moving);
	}

	@Override
	public T newBlockEntity(BlockPos pos, BlockState state) {
		return blockEntitySupplier.create(pos, state);
	}

	@Override
	public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTicker) : ParticleBlockEntity::serverTicker;
	}
}
