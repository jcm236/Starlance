package net.jcm.vsch.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import net.jcm.vsch.api.laser.LaserContext;
import net.jcm.vsch.network.INetworkPacket;

public class LaserContextPacketS2C implements INetworkPacket {
	private static final LaserContextPacketS2C NULL_INSTANCE = new LaserContextPacketS2C(null);

	public final LaserContext laser;

	public LaserContextPacketS2C(final LaserContext laser) {
		this.laser = laser;
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeNbt(laser.writeToNBT(new CompoundTag()));
	}

	public static LaserContextPacketS2C decode(FriendlyByteBuf buf) {
		final LaserContext laser = new LaserContext();
		Level level = Minecraft.getInstance().level;
		if (level == null) {
			return NULL_INSTANCE;
		}
		laser.readFromNBT(level, buf.readNbt());
		return new LaserContextPacketS2C(laser);
	}

	@Override
	public void handle(NetworkEvent.Context ctx) {
		ctx.setPacketHandled(true);
		if (this.laser == null) {
			return;
		}
		ctx.enqueueWork(() -> {
			//
		});
	}
}
