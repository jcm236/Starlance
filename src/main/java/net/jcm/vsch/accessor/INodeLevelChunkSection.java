package net.jcm.vsch.accessor;

import net.jcm.vsch.api.pipe.PipeNode;
import net.jcm.vsch.pipe.level.NodeLevel;

import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;

public interface INodeLevelChunkSection {
	/**
	 * Do NOT modify the returned array.
	 */
	PipeNode[][] starlance$getAllNodes();

	PipeNode starlance$getNode(int x, int y, int z, int index);

	/**
	 * Do NOT modify the returned array.
	 */
	PipeNode[] starlance$getNodes(int x, int y, int z);

	PipeNode starlance$setNode(int x, int y, int z, int index, PipeNode node);

	boolean starlance$hasAnyNode();

	void starlance$writeNodes(FriendlyByteBuf buf);

	void starlance$readNodes(NodeLevel level, SectionPos sectionPos, FriendlyByteBuf buf);
}

