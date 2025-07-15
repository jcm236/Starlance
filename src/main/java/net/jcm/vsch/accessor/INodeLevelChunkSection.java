package net.jcm.vsch.accessor;

import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;

public interface INodeLevelChunkSection {
	PipeNode vsch$getNode(int x, int y, int z, int index);

	/**
	 * Do NOT modify the returned array.
	 */
	PipeNode[] vsch$getNodes(int x, int y, int z);

	PipeNode vsch$setNode(int x, int y, int z, int index, PipeNode node);

	boolean vsch$hasAnyNode();

	void vsch$writeNodes(FriendlyByteBuf buf);

	void vsch$readNodes(NodeLevel level, SectionPos sectionPos, FriendlyByteBuf buf);
}

