/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class MagnetBlock extends BlockWithEntity<MagnetBlockEntity> {
	public static final BooleanProperty GENERATOR = BooleanProperty.create("generator");

	public MagnetBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
			.setValue(FACING, Direction.NORTH)
			.setValue(GENERATOR, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder
			.add(FACING)
			.add(GENERATOR);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);

		if (!(level instanceof final ServerLevel serverLevel)) {
			return;
		}

<<<<<<< HEAD
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);
=======
		// ----- Remove this block from the force appliers for the current level ----- //
		// I guess VS does this automatically when switching a shipyards dimension?
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(serverLevel, pos);
>>>>>>> main
		if (ships != null) {
			ships.removeMagnet(pos);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction dir = ctx.getNearestLookingDirection();
		if (ctx.isSecondaryUseActive()) {
			dir = dir.getOpposite();
		}
		return defaultBlockState()
			.setValue(FACING, dir);
	}

	@Override
<<<<<<< HEAD
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
		super.neighborChanged(state, world, pos, neighbor, neighborPos, moving);
		MagnetBlockEntity be = (MagnetBlockEntity) world.getBlockEntity(pos);
		be.neighborChanged(neighbor, neighborPos, moving);
=======
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);

		if (!(level instanceof final ServerLevel serverLevel)) {
			return;
		}

		int signal = level.getBestNeighborSignal(pos);
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(serverLevel, pos);

		if (ships != null) {
			/*DraggerData data = ships.getDraggerAtPos(pos);

			if (data != null) {
				data.on = (signal > 0);
			}*/
		}
>>>>>>> main
	}

	public MagnetBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MagnetBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? (ParticleBlockEntity::clientTick) : ParticleBlockEntity::serverTick;
	}
}
