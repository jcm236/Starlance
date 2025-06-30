package net.jcm.vsch.accessor;

import net.jcm.vsch.pipe.PipeNode;

import net.minecraft.network.FriendlyByteBuf;

public interface INodeLevelChunkSection {
	PipeNode vsch$getNode(int x, int y, int z, int index);

	PipeNode[] vsch$getNodes(int x, int y, int z);

	void vsch$setNode(int x, int y, int z, int index, PipeNode node);

	boolean vsch$hasAnyNode();

	void vsch$writeNodes(FriendlyByteBuf buf);

	void vsch$readNodes(FriendlyByteBuf buf);
}

