package net.jcm.vsch.accessor;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

public interface LevelChunkAccessor {
	Int2FloatOpenHashMap getDestroyProgressMap();
}
