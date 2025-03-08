package net.jcm.vsch.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import net.jcm.vsch.api.laser.LaserContext;

public class LaserEntity extends Entity {
	private static final EntityDataAccessor<CompoundTag> DATA_LASER = SynchedEntityData.defineId(LaserEntity.class, EntityDataSerializers.COMPOUND_TAG);
	private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(LaserEntity.class, EntityDataSerializers.INT);

	private LaserContext laser;
	private int life;

	public LaserEntity(
		final EntityType<? extends LaserEntity> entityType, final Level level,
		final LaserContext laser, final int life
	) {
		super(entityType, level);
		this.noPhysics = true;
		this.noCulling = true;
		this.laser = laser;
		this.life = life;
		if (laser != null) {
			this.entityData.set(DATA_LASER, laser.writeToNBT(new CompoundTag()));
			this.entityData.set(DATA_LIFE, life);
			final Vec3 pos = laser.getEmitPosition();
			this.absMoveTo(pos.x, pos.y, pos.z);
		}
	}

	public LaserEntity(final EntityType<LaserEntity> entityType, final Level level) {
		this(entityType, level, null, -1);
	}

	public static LaserEntity createAndAdd(final LaserContext laser, final int life) {
		// TODO: reuse entity to prevent network lag cause flash.
		final Level level = laser.getLevel();
		final LaserEntity entity = new LaserEntity(VSCHEntities.LASER_ENTITY.get(), level, laser, life);
		level.addFreshEntity(entity);
		return entity;
	}

	public LaserContext getLaser() {
		if (this.laser == null) {
			LaserContext laser = new LaserContext();
			laser.readFromNBT(this.level(), this.entityData.get(DATA_LASER));
			this.laser = laser;
		}
		return this.laser;
	}

	@Override
	public boolean shouldRender(double pX, double pY, double pZ) {
		return true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double dist) {
		return true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(DATA_LASER, new CompoundTag());
		this.entityData.define(DATA_LIFE, 0);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag data) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag data) {}

	@Override
	public void tick() {
		if (this.life == -1) {
			this.life = this.entityData.get(DATA_LIFE);
		}
		this.life--;
		if (this.life < 0) {
			this.discard();
			return;
		}
	}
}
