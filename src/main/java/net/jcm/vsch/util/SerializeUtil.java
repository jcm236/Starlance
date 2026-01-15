package net.jcm.vsch.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SerializeUtil {
	private SerializeUtil() {}

	/** Quaterniond **/

	public static ListTag quaterniondToList(final Quaterniondc v) {
		final ListTag tag = new ListTag();
		tag.add(DoubleTag.valueOf(v.x()));
		tag.add(DoubleTag.valueOf(v.y()));
		tag.add(DoubleTag.valueOf(v.z()));
		tag.add(DoubleTag.valueOf(v.w()));
		return tag;
	}

	public static Quaterniond listToQuaterniond(final ListTag tag) {
		return listToQuaterniond(tag, new Quaterniond());
	}

	public static Quaterniond listToQuaterniond(final ListTag tag, final Quaterniond dest) {
		dest.x = tag.getDouble(0);
		dest.y = tag.getDouble(1);
		dest.z = tag.getDouble(2);
		dest.w = tag.getDouble(3);
		return dest;
	}

	public static void writeQuaterniond(final FriendlyByteBuf buf, final Quaterniondc v) {
		buf.writeDouble(v.x());
		buf.writeDouble(v.y());
		buf.writeDouble(v.z());
		buf.writeDouble(v.w());
	}

	public static Quaterniond readQuaterniond(final FriendlyByteBuf buf) {
		return readQuaterniond(buf, new Quaterniond());
	}

	public static Quaterniond readQuaterniond(final FriendlyByteBuf buf, final Quaterniond dest) {
		dest.x = buf.readDouble();
		dest.y = buf.readDouble();
		dest.z = buf.readDouble();
		dest.w = buf.readDouble();
		return dest;
	}

	/** Quaternionf **/

	public static ListTag quaternionfToList(final Quaternionfc v) {
		final ListTag tag = new ListTag();
		tag.add(FloatTag.valueOf(v.x()));
		tag.add(FloatTag.valueOf(v.y()));
		tag.add(FloatTag.valueOf(v.z()));
		tag.add(FloatTag.valueOf(v.w()));
		return tag;
	}

	public static Quaternionf listToQuaternionf(final ListTag tag) {
		return listToQuaternionf(tag, new Quaternionf());
	}

	public static Quaternionf listToQuaternionf(final ListTag tag, final Quaternionf dest) {
		dest.x = tag.getFloat(0);
		dest.y = tag.getFloat(1);
		dest.z = tag.getFloat(2);
		dest.w = tag.getFloat(3);
		return dest;
	}

	public static void writeQuaternionf(final FriendlyByteBuf buf, final Quaternionfc v) {
		buf.writeFloat(v.x());
		buf.writeFloat(v.y());
		buf.writeFloat(v.z());
		buf.writeFloat(v.w());
	}

	public static Quaternionf readQuaternionf(final FriendlyByteBuf buf) {
		return readQuaternionf(buf, new Quaternionf());
	}

	public static Quaternionf readQuaternionf(final FriendlyByteBuf buf, final Quaternionf dest) {
		dest.x = buf.readFloat();
		dest.y = buf.readFloat();
		dest.z = buf.readFloat();
		dest.w = buf.readFloat();
		return dest;
	}

	/** Vector3d **/

	public static ListTag vector3dToList(final Vector3dc v) {
		final ListTag tag = new ListTag();
		tag.add(DoubleTag.valueOf(v.x()));
		tag.add(DoubleTag.valueOf(v.y()));
		tag.add(DoubleTag.valueOf(v.z()));
		return tag;
	}

	public static Vector3d listToVector3d(final ListTag tag) {
		return listToVector3d(tag, new Vector3d());
	}

	public static Vector3d listToVector3d(final ListTag tag, final Vector3d dest) {
		dest.x = tag.getDouble(0);
		dest.y = tag.getDouble(1);
		dest.z = tag.getDouble(2);
		return dest;
	}

	public static CompoundTag putVector3d(final CompoundTag tag, final Vector3dc v) {
		tag.putDouble("x", v.x());
		tag.putDouble("y", v.y());
		tag.putDouble("z", v.z());
		return tag;
	}

	public static Vector3d getVector3d(final CompoundTag tag) {
		return getVector3d(tag, new Vector3d());
	}

	public static Vector3d getVector3d(final CompoundTag tag, final Vector3d dest) {
		dest.x = tag.getDouble("x");
		dest.y = tag.getDouble("y");
		dest.z = tag.getDouble("z");
		return dest;
	}

	public static void writeVector3d(final FriendlyByteBuf buf, final Vector3dc v) {
		buf.writeDouble(v.x());
		buf.writeDouble(v.y());
		buf.writeDouble(v.z());
	}

	public static Vector3d readVector3d(final FriendlyByteBuf buf) {
		return readVector3d(buf, new Vector3d());
	}

	public static Vector3d readVector3d(final FriendlyByteBuf buf, final Vector3d dest) {
		dest.x = buf.readDouble();
		dest.y = buf.readDouble();
		dest.z = buf.readDouble();
		return dest;
	}
}
