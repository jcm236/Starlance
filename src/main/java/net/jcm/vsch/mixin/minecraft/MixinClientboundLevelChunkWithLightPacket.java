package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.IClientboundLevelChunkWithLightPacketAccessor;
import net.jcm.vsch.network.s2c.PipeNodeSyncChunkS2C;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;

@Mixin(ClientboundLevelChunkWithLightPacket.class)
public class MixinClientboundLevelChunkWithLightPacket implements IClientboundLevelChunkWithLightPacketAccessor {
	@Shadow
	@Final
	private int x;

	@Shadow
	@Final
	private int z;

	@Unique
	private LevelChunk levelChunk = null;

	@Unique
	private volatile PipeNodeSyncChunkS2C pipeNodeSyncChunkPacket = null;

	@Inject(
		method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/lighting/LevelLightEngine;Ljava/util/BitSet;Ljava/util/BitSet;)V",
		at = @At("RETURN")
	)
	private void init(final LevelChunk levelChunk, final LevelLightEngine lightEngine, final BitSet skyLights, final BitSet blockLights, final CallbackInfo ci) {
		this.levelChunk = levelChunk;
	}

	@Override
	public PipeNodeSyncChunkS2C vsch$getPipeNodeSyncChunkS2C() {
		final LevelChunk levelChunk = this.levelChunk;
		if (this.pipeNodeSyncChunkPacket == null && levelChunk != null) {
			this.pipeNodeSyncChunkPacket = PipeNodeSyncChunkS2C.fromChunk(levelChunk);
			this.levelChunk = null;
		}
		return this.pipeNodeSyncChunkPacket;
	}
}
