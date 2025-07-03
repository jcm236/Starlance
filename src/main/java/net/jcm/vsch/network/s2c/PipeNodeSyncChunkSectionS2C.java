package net.jcm.vsch.network.s2c;

import net.jcm.vsch.VSCHMod;
import net.jcm.vsch.accessor.INodeLevelChunkSection;
import net.jcm.vsch.network.INetworkPacket;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.network.NetworkEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PipeNodeSyncChunkSectionS2C implements INetworkPacket {
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	private final SectionPos sectionPos;
	private final byte[] data;

	public PipeNodeSyncChunkSectionS2C(final SectionPos sectionPos, final byte[] data) {
		this.sectionPos = sectionPos;
		this.data = data;
	}

	@Override
	public void encode(final FriendlyByteBuf buf) {
		buf.writeSectionPos(this.sectionPos);
		buf.writeByteArray(this.data);
	}

	public static PipeNodeSyncChunkSectionS2C fromChunkSection(final SectionPos pos, final LevelChunkSection section) {
		if (!(section instanceof INodeLevelChunkSection nodeSection)) {
			return null;
		}
		// TODO: investigate if Pooled buffer can give more performance
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(128));
		nodeSection.vsch$writeNodes(buf);
		final byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return new PipeNodeSyncChunkSectionS2C(pos, data);
	}

	public static PipeNodeSyncChunkSectionS2C decode(final FriendlyByteBuf buf) {
		final SectionPos sectionPos = buf.readSectionPos();
		final byte[] data = buf.readByteArray();
		return new PipeNodeSyncChunkSectionS2C(sectionPos, data);
	}

	@Override
	public void handle(final NetworkEvent.Context ctx) {
		ctx.setPacketHandled(true);
		final SectionPos sectionPos = this.sectionPos;
		final byte[] data = this.data;
		ctx.enqueueWork(() -> {
			final ClientLevel level = Minecraft.getInstance().level;
			final ChunkAccess chunk = level.getChunkSource().getChunkNow(sectionPos.getX(), sectionPos.getZ());
			if (chunk == null) {
				return;
			}
			final LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionPos.getY()));
			if (!(section instanceof INodeLevelChunkSection nodeSection)) {
				LOGGER.error("[starlance]: Chunk section at {} is not a INodeLevelChunkSection, got {}", sectionPos, section != null ? section.getClass().getName() : "null");
				return;
			}
			nodeSection.vsch$readNodes(new FriendlyByteBuf(Unpooled.wrappedBuffer(data)));
		});
	}
}
