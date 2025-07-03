package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.INodeLevelChunkSection;
import net.jcm.vsch.api.pipe.NodePos;
import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.util.EncodeHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.LevelChunkSection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunkSection.class)
public class MixinLevelChunkSection implements INodeLevelChunkSection {
	@Unique
	private PipeNode[][] nodes = null;

	@Unique
	private int nodeCount = 0;

	@Override
	public PipeNode vsch$getNode(final int x, final int y, final int z, final int index) {
		if (this.nodes == null) {
			return null;
		}
		final PipeNode[] nodes = this.nodes[x << 8 | z << 4 | y];
		return nodes == null ? null : nodes[index];
	}

	@Override
	public PipeNode[] vsch$getNodes(final int x, final int y, final int z) {
		if (this.nodes == null) {
			return null;
		}
		final PipeNode[] nodes = this.nodes[x << 8 | z << 4 | y];
		return nodes == null ? null : nodes;
	}

	@Override
	public void vsch$setNode(final int x, final int y, final int z, final int index, final PipeNode node) {
		if (this.nodes == null) {
			this.nodes = new PipeNode[LevelChunkSection.SECTION_SIZE][];
		}
		final int blockIndex = x << 8 | z << 4 | y;
		PipeNode[] nodes = this.nodes[blockIndex];
		if (nodes == null) {
			nodes = new PipeNode[NodePos.UNIQUE_INDEX_BOUND];
			this.nodes[blockIndex] = nodes;
		}
		final PipeNode oldNode = nodes[index];
		nodes[index] = node;
		final boolean hasOld = oldNode != null;
		final boolean hasNew = node != null;
		if (hasOld) {
			if (!hasNew) {
				this.nodeCount--;
			}
		} else if (hasNew) {
			this.nodeCount++;
		}
	}

	@Override
	public boolean vsch$hasAnyNode() {
		return this.nodeCount > 0;
	}

	@Override
	public void vsch$writeNodes(final FriendlyByteBuf buf) {
		final int maxWritten = this.nodeCount;
		buf.writeVarInt(maxWritten);
		if (maxWritten == 0) {
			return;
		}
		int written = 0;
		for (int blockIndex = 0; blockIndex < LevelChunkSection.SECTION_SIZE && written < maxWritten; blockIndex++) {
			final PipeNode[] nodes = this.nodes[blockIndex];
			if (nodes == null) {
				EncodeHelper.writeVarInt22(buf, 0);
				continue;
			}
			int bitset = 0;
			for (int i = 0; i < NodePos.UNIQUE_INDEX_BOUND; i++) {
				if (nodes[i] != null) {
					bitset |= 1 << i;
				}
			}
			EncodeHelper.writeVarInt22(buf, bitset);
			if (bitset == 0) {
				continue;
			}
			for (int i = 0; i < NodePos.UNIQUE_INDEX_BOUND; i++) {
				final PipeNode node = nodes[i];
				if (node != null) {
					written++;
					node.writeTo(buf);
				}
			}
		}
		if (written != maxWritten) {
			throw new RuntimeException("Incorrect node count, expect " + maxWritten + ", got" + written);
		}
	}

	@Override
	public void vsch$readNodes(final FriendlyByteBuf buf) {
		final int maxRead = buf.readVarInt();
		this.nodeCount = maxRead;
		if (maxRead == 0) {
			this.nodes = null;
			return;
		}
		this.nodes = new PipeNode[LevelChunkSection.SECTION_SIZE][];
		int read = 0;
		for (int blockIndex = 0; blockIndex < LevelChunkSection.SECTION_SIZE && read < maxRead; blockIndex++) {
			int bitset = EncodeHelper.readVarInt22(buf);
			if (bitset == 0) {
				continue;
			}
			final PipeNode[] nodes = new PipeNode[NodePos.UNIQUE_INDEX_BOUND];
			this.nodes[blockIndex] = nodes;
			for (int i = 0; bitset != 0; i++) {
				if ((bitset & 1) != 0) {
					read++;
					final PipeNode node = PipeNode.readFrom(buf);
					if (node == null) {
						this.nodeCount--;
					} else {
						nodes[i] = node;
					}
				}
				bitset >>>= 1;
			}
		}
		if (read != maxRead) {
			throw new RuntimeException("Incorrect node count, expect " + maxRead + ", got" + read);
		}
	}
}
