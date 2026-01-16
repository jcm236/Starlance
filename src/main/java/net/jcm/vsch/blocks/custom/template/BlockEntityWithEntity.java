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

import net.jcm.vsch.blocks.entity.template.ParticleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A block entity for spawning and removing an entity with the block.
 * Make sure to overwrite createLinkedEntity.
 * @see BlockWithEntity
 * @param <E> The entity class to be using
 */
public abstract class BlockEntityWithEntity<E extends Entity> extends BlockEntity implements ParticleBlockEntity {
	private UUID entityUUID;

	public BlockEntityWithEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void spawnLinkedEntity() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (this.entityUUID != null) {
			removeLinkedEntity();
		}
		spawnLinkedEntityImpl(serverLevel, getBlockPos());
	}

	public void spawnLinkedEntityIfNeeded() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (this.entityUUID != null) {
			return;
		}
		spawnLinkedEntityImpl(serverLevel, getBlockPos());
	}

	private E spawnLinkedEntityImpl(ServerLevel level, BlockPos blockPos) {
		E entity = this.createLinkedEntity(level, blockPos);
		entity.setPos(Vec3.atCenterOf(blockPos));
		level.addFreshEntity(entity);
		this.entityUUID = entity.getUUID();
		return entity;
	}

	public void removeLinkedEntity() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (this.entityUUID == null) {
			return;
		}
		Entity entity = serverLevel.getEntity(this.entityUUID);
		this.entityUUID = null;
		// Don't remove the entity if it doesn't exist, duh
		if (entity != null) {
			entity.discard();
		}
	}

	/**
	 * The function used by this BE to create the entity object to spawn.
	 * Overwrite with creating a new object of your entity class.
	 * @param level Level, for help creating the entity object
	 * @return
	 */
	public abstract E createLinkedEntity(ServerLevel level, BlockPos pos);

	// Saving and loading the attached entity UUID on world reload:
	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (this.entityUUID != null) {
			tag.putUUID("EntityUUID", this.entityUUID);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		if (tag.hasUUID("EntityUUID")) {
			this.entityUUID = tag.getUUID("EntityUUID");
		}
		super.load(tag);
	}

	@Override
	public void tickParticles(Level level, BlockPos pos, BlockState state) {}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		if (this.entityUUID == null) {
			return;
		}
		Entity entity = level.getEntity(this.entityUUID);
		if (entity == null) {
			return;
		}
		entity.setPos(Vec3.atCenterOf(getBlockPos()));
	}
}
