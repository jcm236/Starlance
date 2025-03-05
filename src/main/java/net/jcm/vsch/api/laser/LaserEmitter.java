package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public abstract class LaserEmitter {
	private final Level level;
	private final Vec3 location;
	private final Vec3 direction;

	protected LaserEmitter(Level level, Vec3 location, Vec3 direction) {
		this.level = level;
		this.location = location;
		this.direction = direction;
	}

	public Level getLevel() {
		return this.level;
	}

	public Vec3 getLocation() {
		return this.location;
	}

	public Vec3 getDirection() {
		return this.direction;
	}

	public abstract LaserEmitterType getType();

	public abstract Object getSource();

	public static LaserEmitter fromBlock(Level level, Vec3 pos, Vec3 direction, BlockPos blockPos, BlockEntity be) {
		return new BlockLaserEmitter(level, pos, direction, blockPos, be);
	}

	public static LaserEmitter fromEntity(Level level, Vec3 pos, Vec3 direction, Entity entity) {
		return new EntityLaserEmitter(level, pos, direction, entity);
	}

	public static class BlockLaserEmitter extends LaserEmitter {
		private final BlockPos blockPos;
		private final BlockEntity be;

		BlockLaserEmitter(Level level, Vec3 pos, Vec3 direction, BlockPos blockPos, BlockEntity be) {
			super(level, pos, direction);
			this.blockPos = blockPos;
			this.be = be;
		}

		@Override
		public LaserEmitterType getType() {
			return LaserEmitterType.BLOCK;
		}

		@Override
		public BlockEntity getSource() {
			return this.be;
		}

		public BlockPos getSourceBlock() {
			return this.blockPos;
		}
	}

	public static class EntityLaserEmitter extends LaserEmitter {
		private final Entity entity;

		EntityLaserEmitter(Level level, Vec3 pos, Vec3 direction, Entity entity) {
			super(level, pos, direction);
			this.entity = entity;
		}

		@Override
		public LaserEmitterType getType() {
			return LaserEmitterType.ENTITY;
		}

		@Override
		public Entity getSource() {
			return this.entity;
		}
	}
}
