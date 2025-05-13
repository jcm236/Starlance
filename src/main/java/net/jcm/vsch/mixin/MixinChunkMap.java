package net.jcm.vsch.mixin;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ChunkClaim;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public class MixinChunkMap {
	@Shadow
	@Final
	ServerLevel level;

	@Inject(method = "save", at = @At("HEAD"), cancellable = true)
	private void save(final ChunkAccess chunk, final CallbackInfoReturnable<Boolean> cir) {
		final ChunkPos pos = chunk.getPos();
		final ServerShip ship = VSGameUtilsKt.getShipManagingPos(this.level, pos);
		if (ship == null) {
			return;
		}
		final String slug = ship.getSlug();
		if (slug == null) {
			return;
		}
		if (slug.startsWith("+asteroid+") || slug.startsWith("+idle+")) {
			final ChunkClaim claim = ship.getChunkClaim();
			final ChunkPos center = new ChunkPos(claim.getXMiddle(), claim.getZMiddle());
			if (center.getChessboardDistance(pos) > 1) {
				chunk.setUnsaved(false);
				cir.setReturnValue(false);
			}
		}
	}
}
