package net.jcm.vsch.accessor;

import net.jcm.vsch.network.s2c.PipeNodeSyncChunkS2C;

public interface IClientboundLevelChunkWithLightPacketAccessor {
	PipeNodeSyncChunkS2C vsch$getPipeNodeSyncChunkS2C();
}
