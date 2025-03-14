package net.jcm.vsch.blocks.custom.template;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.jcm.vsch.blocks.entity.template.AbstractCannonBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.util.rot.DirectionalShape;

public abstract class AbstractCannonBlock<T extends AbstractCannonBlockEntity> extends DirectionalBlock implements EntityBlock {
	private final DirectionalShape shape;

	protected AbstractCannonBlock(BlockBehaviour.Properties properties, DirectionalShape shape) {
		super(properties);
		this.shape = shape;
		this.registerDefaultState(this.defaultBlockState()
			.setValue(FACING, Direction.NORTH)
		);
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.shape.get(state.getValue(BlockStateProperties.FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction dir = ctx.getNearestLookingDirection().getOpposite();
		Player player = ctx.getPlayer();
		if (player != null && player.isShiftKeyDown()) {
			dir = dir.getOpposite();
		}
		return this.defaultBlockState()
			.setValue(BlockStateProperties.FACING, dir);
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
		super.neighborChanged(state, world, pos, neighbor, neighborPos, moving);
		if (world.getBlockEntity(pos) instanceof AbstractCannonBlockEntity be) {
			be.neighborChanged(neighbor, neighborPos, moving);
		}
	}

	@Override
	public abstract T newBlockEntity(BlockPos pos, BlockState state);

	@Override
	public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTicker) : ParticleBlockEntity::serverTicker;
	}
}
