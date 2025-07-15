package net.jcm.vsch.accessor;

import net.minecraft.server.level.ChunkHolder;

public interface IChunkMapAccessor {
	Iterable<ChunkHolder> vsch$getChunks();
}
