package net.jcm.vsch.mixin.minecraft;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import net.minecraft.world.level.chunk.LevelChunk;

import net.jcm.vsch.accessor.LevelChunkAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
public class MixinLevelChunk implements LevelChunkAccessor {
	@Unique
	private final Int2FloatOpenHashMap progress = new Int2FloatOpenHashMap();

	@Override
	public Int2FloatOpenHashMap getDestroyProgressMap() {
		return this.progress;
	}
}
