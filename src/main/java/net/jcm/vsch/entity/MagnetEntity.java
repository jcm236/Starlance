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
package net.jcm.vsch.entity;

import net.jcm.vsch.blocks.VSCHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class MagnetEntity extends Entity {
	private BlockPos pos;

	public MagnetEntity(EntityType<? extends MagnetEntity> entityType, Level level, BlockPos pos) {
		super(entityType, level);
		this.noPhysics = true; // Prevents collision with blocks
		this.setInvisible(true);
		this.pos = pos;
	}

	public MagnetEntity(EntityType<MagnetEntity> magnetEntityEntityType, Level level) {
		super(magnetEntityEntityType, level);
		this.noPhysics = true;
		this.setInvisible(true);
	}

	public void setAttachedPos(BlockPos pos) {
		this.pos = pos;
	}

	@Override
	public boolean shouldRender(double pX, double pY, double pZ) {
		return false;
	}

	@Override
	public void tick() {
		Level lv = level();
		if (!(lv instanceof ServerLevel)) {
			return;
		}
		if (pos == null) {
			this.discard();
			return;
		}

		BlockState block = lv.getBlockState(pos);

		/*if (!block.is(VSCHBlocks.MAGNET_BLOCK.get())) {
			this.discard();
		}*/
	}

	@Override
	protected void defineSynchedData() {}

	@Override
	protected void readAdditionalSaveData(CompoundTag compoundTag) {
		int x = compoundTag.getInt("attachPosX");
		int y = compoundTag.getInt("attachPosY");
		int z = compoundTag.getInt("attachPosZ");
		this.pos = new BlockPos(x, y, z);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compoundTag) {
		compoundTag.putInt("attachPosX", pos.getX());
		compoundTag.putInt("attachPosY", pos.getY());
		compoundTag.putInt("attachPosZ", pos.getZ());
	}
}
