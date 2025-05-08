package net.jcm.vsch.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SerializeUtil {
	private SerializeUtil() {}

	public static long[] vec3ToLongArray(Vec3 pos) {
		return new long[]{
			Double.doubleToRawLongBits(pos.x),
			Double.doubleToRawLongBits(pos.y),
			Double.doubleToRawLongBits(pos.z)
		};
	}

	public static Vec3 vec3FromLongArray(long[] arr) {
		return new Vec3(
			Double.longBitsToDouble(arr[0]),
			Double.longBitsToDouble(arr[1]),
			Double.longBitsToDouble(arr[2])
		);
	}

	public static CompoundTag hitResultToNBT(HitResult hit) {
		CompoundTag tag = new CompoundTag();
		tag.putByte("Type", (byte) (hit.getType().ordinal()));
		tag.putLongArray("Loc", vec3ToLongArray(hit.getLocation()));
		if (hit instanceof BlockHitResult blockHit) {
			tag.putLong("Pos", blockHit.getBlockPos().asLong());
			tag.putByte("Dir", (byte) (blockHit.getDirection().get3DDataValue()));
			if (hit.getType() == HitResult.Type.BLOCK) {
				tag.putBoolean("Inside", blockHit.isInside());
			}
		} else if (hit instanceof EntityHitResult entityHit) {
			tag.putInt("Entity", entityHit.getEntity().getId());
		} else {
			throw new IllegalArgumentException("Unsupported HitResult " + hit.getClass().toString());
		}
		return tag;
	}

	public static HitResult hitResultFromNBT(Level level, CompoundTag tag) {
		HitResult.Type type = HitResult.Type.values()[tag.getByte("Type")];
		return switch (type) {
			case BLOCK -> new BlockHitResult(
				vec3FromLongArray(tag.getLongArray("Loc")),
				Direction.from3DDataValue(tag.getByte("Dir")),
				BlockPos.of(tag.getLong("Pos")),
				tag.getBoolean("Inside")
			);
			case MISS -> BlockHitResult.miss(
				vec3FromLongArray(tag.getLongArray("Loc")),
				Direction.from3DDataValue(tag.getByte("Dir")),
				BlockPos.of(tag.getLong("Pos"))
			);
			case ENTITY -> new EntityHitResult(
				level.getEntity(tag.getInt("Entity")),
				vec3FromLongArray(tag.getLongArray("Loc"))
			);
		};
	}
}
