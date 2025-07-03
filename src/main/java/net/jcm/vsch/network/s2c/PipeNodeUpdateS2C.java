package net.jcm.vsch.network.s2c;

import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.network.INetworkPacket;
import net.jcm.vsch.pipe.level.NodeLevel;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.network.NetworkEvent;

public class PipeNodeUpdateS2C implements INetworkPacket {
	private static final byte[] EMPTY_BYTES = new byte[0];

	private final NodePos pos;
	private final byte[] data;

	public PipeNodeUpdateS2C(final NodePos pos, final byte[] data) {
		this.pos = pos;
		this.data = data;
	}

	@Override
	public void encode(final FriendlyByteBuf buf) {
		this.pos.writeTo(buf);
		buf.writeByteArray(this.data);
	}

	public static PipeNodeUpdateS2C fromNode(final NodePos pos, final PipeNode node) {
		if (node == null) {
			return new PipeNodeUpdateS2C(pos, EMPTY_BYTES);
		}
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(4));
		node.writeTo(buf);
		final byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new PipeNodeUpdateS2C(pos, data);
	}

	public static PipeNodeUpdateS2C decode(final FriendlyByteBuf buf) {
		final NodePos pos = NodePos.readFrom(buf);
		final byte[] data = buf.readByteArray();
		return new PipeNodeUpdateS2C(pos, data);
	}

	@Override
	public void handle(final NetworkEvent.Context ctx) {
		ctx.setPacketHandled(true);
		final NodePos pos = this.pos;
		final PipeNode node = this.data.length > 0 ? PipeNode.readFrom(new FriendlyByteBuf(Unpooled.wrappedBuffer(this.data))) : null;
		ctx.enqueueWork(() -> {
			final ClientLevel level = Minecraft.getInstance().level;
			final NodeLevel nodeLevel = NodeLevel.get(level);
			nodeLevel.setNode(pos, node);
		});
	}
}
