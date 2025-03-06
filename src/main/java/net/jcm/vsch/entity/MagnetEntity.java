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
