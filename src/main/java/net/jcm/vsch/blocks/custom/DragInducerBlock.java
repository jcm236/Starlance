package net.jcm.vsch.blocks.custom;

import net.jcm.vsch.blocks.entity.DragInducerBlockEntity;
import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.ship.DraggerData;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.jcm.vsch.util.rot.DirectionalShape;
import net.jcm.vsch.util.rot.RotShape;
import net.jcm.vsch.util.rot.RotShapes;
import net.lointain.cosmos.init.CosmosModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class DragInducerBlock extends Block implements EntityBlock {

	public DragInducerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!(level instanceof ServerLevel)) return;

		// ----- Remove this block from the force appliers for the current level ----- //
		// I guess VS does this automatically when switching a shipyards dimension?
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
		if (ships != null) {
			ships.removeDragger(pos);
		}

		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
		super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
		DragInducerBlockEntity be = (DragInducerBlockEntity) level.getBlockEntity(pos);
		be.neighborChanged(neighbor, neighborPos, moving);
	}

	@Override
	public DragInducerBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DragInducerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTick) : ParticleBlockEntity::serverTick;
	}
}
