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
package net.jcm.vsch.blocks.entity.template;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface ParticleBlockEntity {

	default void clientTick(Level level, BlockPos pos, BlockState state, ParticleBlockEntity be) {
		tickParticles(level, pos, state);
	}

	default void serverTick(Level level, BlockPos pos, BlockState state, ParticleBlockEntity be) {
		if (level instanceof ServerLevel serverLevel) {
			tickForce(serverLevel, pos, state);
		}
	}

	void tickForce(ServerLevel level, BlockPos pos, BlockState state);

	void tickParticles(Level level, BlockPos pos, BlockState state);

	public static void clientTick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.getBlockEntity(pos) != blockEntity) {
			blockEntity.setRemoved();
		}
		if (blockEntity.isRemoved()) {
			return;
		}
		ParticleBlockEntity be = (ParticleBlockEntity) blockEntity;
		be.clientTick(level, pos, state, be);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.getBlockEntity(pos) != blockEntity) {
			blockEntity.setRemoved();
		}
		if (blockEntity.isRemoved()) {
			return;
		}
		ParticleBlockEntity be = (ParticleBlockEntity) blockEntity;
		be.serverTick(level, pos, state, be);
	}
}
