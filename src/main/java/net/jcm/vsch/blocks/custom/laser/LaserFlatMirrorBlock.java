package net.jcm.vsch.blocks.custom.laser;

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

import net.jcm.vsch.blocks.custom.template.AbstractCannonBlock;
import net.jcm.vsch.blocks.entity.laser.AbstractLaserLenBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.util.rot.DirectionalShape;
import net.jcm.vsch.util.rot.RotShape;
import net.jcm.vsch.util.rot.RotShapes;

import java.util.function.BiFunction;

public class LaserFlatMirrorBlock<T extends AbstractLaserLenBlockEntity> extends DirectionalBlock implements EntityBlock {
	private static final DirectionalShape SHAPE = DirectionalShape.down(RotShapes.box(1.0, 7.0, 1.0, 15.0, 9.0, 15.0));
	private final BiFunction<BlockPos, BlockState, T> blockEntityFactory;

	public LaserFlatMirrorBlock(BlockBehaviour.Properties properties, BiFunction<BlockPos, BlockState, T> blockEntityFactory) {
		super(properties);
		this.blockEntityFactory = blockEntityFactory;
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
		return SHAPE.get(state.getValue(BlockStateProperties.FACING));
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
	public T newBlockEntity(BlockPos pos, BlockState state) {
		return this.blockEntityFactory.apply(pos, state);
	}

	@Override
	public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTicker) : ParticleBlockEntity::serverTicker;
	}
}
