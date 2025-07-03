package net.jcm.vsch.network.s2c;

import net.jcm.vsch.network.INetworkPacket;
import net.jcm.vsch.pipe.level.NodeGetter;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.network.NetworkEvent;

public class PipeNodeSyncChunkS2C implements INetworkPacket {
	private final ChunkPos chunkPos;
	private final byte[] data;

	public PipeNodeSyncChunkS2C(final ChunkPos chunkPos, final byte[] data) {
		this.chunkPos = chunkPos;
		this.data = data;
	}

	@Override
	public void encode(final FriendlyByteBuf buf) {
		buf.writeChunkPos(this.chunkPos);
		buf.writeByteArray(this.data);
	}

	public static PipeNodeSyncChunkS2C fromChunk(final ChunkAccess chunk) {
		if (!(chunk instanceof NodeGetter nodeGetter)) {
			return null;
		}
		// TODO: investigate if Pooled buffer can give more performance
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(128));
		nodeGetter.writeNodes(buf);
		final byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new PipeNodeSyncChunkS2C(chunk.getPos(), data);
	}

	public static PipeNodeSyncChunkS2C decode(final FriendlyByteBuf buf) {
		final ChunkPos chunkPos = buf.readChunkPos();
		final byte[] data = buf.readByteArray();
		return new PipeNodeSyncChunkS2C(chunkPos, data);
	}

	@Override
	public void handle(final NetworkEvent.Context ctx) {
		ctx.setPacketHandled(true);
		final ChunkPos chunkPos = this.chunkPos;
		final byte[] data = this.data;
		ctx.enqueueWork(() -> {
			final ClientLevel level = Minecraft.getInstance().level;
			final ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
			if (!(chunk instanceof NodeGetter nodeGetter)) {
				return;
			}
			nodeGetter.readNodes(new FriendlyByteBuf(Unpooled.wrappedBuffer(data)));
		});
	}
}
