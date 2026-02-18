/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.util.EmptyChunkAccess;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public class MixinChunkMap {
	@Shadow
	@Final
	ServerLevel level;

	@Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
	private void schedule(final ChunkHolder holder, final ChunkStatus status, final CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
		final ServerLevel level = this.level;
		final ChunkPos pos = holder.getPos();
		if (EmptyChunkAccess.shouldUseEmptyChunk(level, pos.x, pos.z)) {
			final EmptyChunkAccess emptyAccess = new EmptyChunkAccess(level, pos);
			cir.setReturnValue(CompletableFuture.completedFuture(Either.left(switch (status.getChunkType()) {
				case PROTOCHUNK -> new ImposterProtoChunk(emptyAccess, true);
				case LEVELCHUNK -> emptyAccess;
			})));
		}
	}

	@Inject(method = "save", at = @At("HEAD"), cancellable = true)
	private void save(final ChunkAccess chunk, final CallbackInfoReturnable<Boolean> cir) {
		if (chunk instanceof EmptyChunkAccess) {
			cir.setReturnValue(false);
		}
	}
}
