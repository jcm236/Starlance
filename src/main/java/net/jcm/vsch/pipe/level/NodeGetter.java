package net.jcm.vsch.pipe.level;

import net.jcm.vsch.pipe.PipeNode;

import net.minecraft.network.FriendlyByteBuf;

public interface NodeGetter {
	PipeNode getNode(int x, int y, int z, int index);

	PipeNode[] getNodes(int x, int y, int z);

	void setNode(int x, int y, int z, int index, PipeNode node);

	boolean hasAnyNode();

	void writeNodes(FriendlyByteBuf buf);

	void readNodes(FriendlyByteBuf buf);
}
