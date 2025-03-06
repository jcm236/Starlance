package net.jcm.vsch.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.jcm.vsch.util.SerializeUtil;

public abstract class LaserEmitter {
	private final Level level;
	private Vec3 location;
	private Vec3 direction;

	public LaserEmitter(Level level) {
		this(level, null, null);
	}

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

	public CompoundTag writeToNBT(CompoundTag data) {
		data.putLongArray("Loc", SerializeUtil.vec3ToLongArray(this.location));
		data.putLongArray("Dir", SerializeUtil.vec3ToLongArray(this.direction));
		return data;
	}

	public void readFromNBT(CompoundTag data) {
		this.location = SerializeUtil.vec3FromLongArray(data.getLongArray("Loc"));
		this.direction = SerializeUtil.vec3FromLongArray(data.getLongArray("Dir"));
	}

	public static LaserEmitter fromBlock(Level level, Vec3 pos, Vec3 direction, BlockPos blockPos, BlockEntity be) {
		return new BlockLaserEmitter(level, pos, direction, blockPos, be);
	}

	public static LaserEmitter fromBlockEntity(final BlockEntity be, final Direction facing) {
		final Level level = be.getLevel();
		final BlockPos blockPos = be.getBlockPos();
		Vec3 worldPos = Vec3.atCenterOf(blockPos);
		Vec3 direction = Vec3.atLowerCornerOf(facing.getNormal());
		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPos);
		if (ship != null) {
			Matrix4dc transform = ship.getShipToWorld();
			Vector3d dest = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
			transform.transformPosition(dest);
			worldPos = new Vec3(dest.x, dest.y, dest.z);
			transform.transformDirection(dest.set(direction.x, direction.y, direction.z));
			direction = new Vec3(dest.x, dest.y, dest.z);
		}
		return fromBlock(level, worldPos, direction, blockPos, be);
	}

	public static LaserEmitter fromEntity(Level level, Vec3 pos, Vec3 direction, Entity entity) {
		return new EntityLaserEmitter(level, pos, direction, entity);
	}

	public static LaserEmitter parseFromNBT(Level level, CompoundTag data) {
		final String type = data.getString("Type");
		final LaserEmitter emitter = switch (type) {
			case "block" -> new BlockLaserEmitter(level);
			case "entity" -> new EntityLaserEmitter(level);
			default -> throw new IllegalArgumentException("Unknown LaserEmitter type: " + type);
		};
		emitter.readFromNBT(data);
		return emitter;
	}

	public static class BlockLaserEmitter extends LaserEmitter {
		private BlockPos blockPos;
		private BlockEntity be;

		public BlockLaserEmitter(Level level) {
			this(level, null, null, null, null);
		}

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

		@Override
		public CompoundTag writeToNBT(CompoundTag data) {
			super.writeToNBT(data);
			data.putString("Type", "block");
			data.putLong("Block", this.blockPos.asLong());
			return data;
		}

		@Override
		public void readFromNBT(CompoundTag data) {
			this.blockPos = BlockPos.of(data.getLong("Block"));
			this.be = this.getLevel().getBlockEntity(this.blockPos);
			super.readFromNBT(data);
		}
	}

	public static class EntityLaserEmitter extends LaserEmitter {
		private Entity entity;

		public EntityLaserEmitter(Level level) {
			this(level, null, null, null);
		}

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

		@Override
		public CompoundTag writeToNBT(CompoundTag data) {
			super.writeToNBT(data);
			data.putString("Type", "entity");
			data.putInt("Entity", this.entity.getId());
			return data;
		}

		@Override
		public void readFromNBT(CompoundTag data) {
			this.entity = this.getLevel().getEntity(data.getInt("Entity"));
			super.readFromNBT(data);
		}
	}
}
