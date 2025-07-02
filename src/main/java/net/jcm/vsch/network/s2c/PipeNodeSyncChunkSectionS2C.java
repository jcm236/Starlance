package net.jcm.vsch.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import net.jcm.vsch.network.INetworkPacket;

public class PipeNodeSyncChunkSectionS2C implements INetworkPacket {
	public final byte[] data;

	public PipeNodeSyncChunkSectionS2C(final byte[] data) {
		this.data = data;
	}

	@Override
	public void encode(final FriendlyByteBuf buf) {
		buf.writeByteArray(this.data);
	}

	public static PipeNodeSyncChunkSectionS2C decode(final FriendlyByteBuf buf) {
		final byte[] data = buf.readByteArray();
		return new PipeNodeSyncChunkSectionS2C(data);
	}

	@Override
	public void handle(NetworkEvent.Context ctx) {
		ctx.setPacketHandled(true);
		ctx.enqueueWork(() -> {
			//
		});
	}
}
