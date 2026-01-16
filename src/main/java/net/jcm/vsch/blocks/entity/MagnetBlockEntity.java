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
package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.custom.template.BlockEntityWithEntity;
import net.jcm.vsch.entity.MagnetEntity;
import net.jcm.vsch.entity.VSCHEntities;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MagnetBlockEntity extends BlockEntityWithEntity<MagnetEntity> {

	public MagnetBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.DRAG_INDUCER_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		this.spawnLinkedEntityIfNeeded();
		super.tickForce(level, pos, state);

		// ----- Add this block to the force appliers for the current level ----- //

		int signal = level.getBestNeighborSignal(pos);
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);

		if (ships != null) {
			/*if (ships.getDraggerAtPos(pos) == null) {
				ships.addDragger(pos, new DraggerData(signal > 0));
			}*/
		}
	}

	@Override
	public MagnetEntity createLinkedEntity(ServerLevel level, BlockPos pos) {
		return new MagnetEntity(VSCHEntities.MAGNET_ENTITY.get(), level, pos);
	}
}
