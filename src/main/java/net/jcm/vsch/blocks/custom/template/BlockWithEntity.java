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
package net.jcm.vsch.blocks.custom.template;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * For making a block that has an entity always attached (an actual entity)
 * @param <T> The block entity class that is responsible for spawning the entity, removing it, etc
 * @see BlockEntityWithEntity
 */
public abstract class BlockWithEntity<T extends BlockEntityWithEntity<?>> extends DirectionalBlock implements EntityBlock {
	public BlockWithEntity(Properties properties) {
		super(properties);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		super.onRemove(state, level, pos, newState, isMoving);
		if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof BlockEntityWithEntity<?> blockEntityWithEntity) {
				blockEntityWithEntity.removeLinkedEntity();
			}
		}
	}

	@Override
	public abstract T newBlockEntity(BlockPos pos, BlockState state);
}
