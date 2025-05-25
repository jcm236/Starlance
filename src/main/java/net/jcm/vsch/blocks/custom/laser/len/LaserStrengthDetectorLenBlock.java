package net.jcm.vsch.blocks.custom.laser.len;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import net.jcm.vsch.blocks.entity.laser.len.LaserStrengthDetectorLenBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.util.rot.DirectionalShape;
import net.jcm.vsch.util.rot.RotShapes;

public class LaserStrengthDetectorLenBlock extends DirectionalBlock implements EntityBlock {
	private static final DirectionalShape SHAPE = DirectionalShape.down(RotShapes.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0));

	public LaserStrengthDetectorLenBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(FACING, Direction.DOWN)
		);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LaserStrengthDetectorLenBlockEntity be ? be.getAnalogOutput() : 0;
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
		Direction dir = ctx.getClickedFace().getOpposite();
		return this.defaultBlockState()
			.setValue(BlockStateProperties.FACING, dir);
	}

	@Override
	public LaserStrengthDetectorLenBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LaserStrengthDetectorLenBlockEntity(pos, state);
	}

	@Override
	public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTicker) : ParticleBlockEntity::serverTicker;
	}
}
