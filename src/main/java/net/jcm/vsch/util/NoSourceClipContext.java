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
package net.jcm.vsch.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NoSourceClipContext extends ClipContext {
	private final BlockPos source;

	public NoSourceClipContext(Vec3 from, Vec3 to, BlockPos source) {
		super(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null);
		this.source = source;
	}

	@Override
	public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
		if (this.source != null && this.source.equals(pos)) {
			return Shapes.empty();
		}
		return super.getBlockShape(state, level, pos);
	}

	@Override
	public VoxelShape getFluidShape(FluidState state, BlockGetter level, BlockPos pos) {
		return super.getFluidShape(state, level, pos);
	}
}
