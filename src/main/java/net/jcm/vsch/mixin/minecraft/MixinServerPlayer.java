package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.IClientboundLevelChunkWithLightPacketAccessor;
import net.jcm.vsch.network.VSCHNetwork;
import net.jcm.vsch.network.s2c.PipeNodeSyncChunkS2C;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class MixinServerPlayer {
	@Shadow
	public ServerGamePacketListenerImpl connection;

	@Inject(
		method = "trackChunk",
		at = @At("RETURN")
	)
	public void trackChunk(final ChunkPos chunkPos, final Packet<?> packet, final CallbackInfo ci) {
		if (!(packet instanceof IClientboundLevelChunkWithLightPacketAccessor chunkPacket)) {
			return;
		}
		final PipeNodeSyncChunkS2C pipePacket = chunkPacket.vsch$getPipeNodeSyncChunkS2C();
		VSCHNetwork.sendToPlayer(pipePacket, (ServerPlayer) ((Object) (this)));
	}
}
