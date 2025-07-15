package net.jcm.vsch.util;

import net.minecraft.network.FriendlyByteBuf;

public final class EncodeHelper {
	private EncodeHelper() {}

	public static void writeVarInt22(final FriendlyByteBuf buf, int num) {
		for (int i = 0; i < 2 && num > 0xff; i++) {
			buf.writeByte((num & 0x7f) | 0x80);
			num >>>= 7;
		}
		buf.writeByte(num);
	}

	public static int readVarInt22(final FriendlyByteBuf buf) {
		int num = 0;
		for (int i = 0; true; i++) {
			final byte b = buf.readByte();
			num |= (b & 0x7f) << (7 * i);
			if ((b & 0x80) == 0) {
				break;
			}
			if (i == 2) {
				num |= 0x80 << (7 * 2);
				break;
			}
		}
		return num;
	}
}
