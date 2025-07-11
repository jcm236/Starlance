package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.IChunkMapAccessor;
import net.jcm.vsch.util.EmptyChunkAccess;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMap implements IChunkMapAccessor {
	@Shadow
	@Final
	ServerLevel level;

	@Shadow
	protected abstract Iterable<ChunkHolder> getChunks();

	@Override
	public Iterable<ChunkHolder> vsch$getChunks() {
		return this.getChunks();
	}

	@Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
	private void schedule(final ChunkHolder holder, final ChunkStatus status, final CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
		final ServerLevel level = this.level;
		final ChunkPos pos = holder.getPos();
		if (EmptyChunkAccess.shouldUseEmptyChunk(level, pos.x, pos.z)) {
			cir.setReturnValue(CompletableFuture.completedFuture(Either.left(new EmptyChunkAccess(level, pos))));
		}
	}

	@Inject(method = "save", at = @At("HEAD"), cancellable = true)
	private void save(final ChunkAccess chunk, final CallbackInfoReturnable<Boolean> cir) {
		if (chunk instanceof EmptyChunkAccess) {
			cir.setReturnValue(false);
		}
	}
}
