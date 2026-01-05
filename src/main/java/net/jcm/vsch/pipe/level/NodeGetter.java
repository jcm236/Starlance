package net.jcm.vsch.pipe.level;

import net.jcm.vsch.api.pipe.PipeNode;

import net.minecraft.network.FriendlyByteBuf;

import java.util.stream.Stream;

public interface NodeGetter {
	PipeNode getNode(int x, int y, int z, int index);

	PipeNode[] getNodes(int x, int y, int z);

	PipeNode setNode(int x, int y, int z, int index, PipeNode node);

	Stream<PipeNode> streamNodes();

	boolean hasAnyNode();

	void writeNodes(FriendlyByteBuf buf);

	void readNodes(FriendlyByteBuf buf);

	boolean isNodesUnsaved();

	void setNodesUnsaved();
}
