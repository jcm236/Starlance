package net.jcm.vsch.mixin.cosmos;

import net.lointain.cosmos.procedures.CollisionDetectorProcedure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CollisionDetectorProcedure.class)
public class MixinCollisionDetectorProcedure {
	@Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
	private static void execute(final LevelAccessor world, final double x, final double y, final double z, final Entity entity, final CallbackInfo ci) {
		if (VSGameUtilsKt.isBlockInShipyard((Level) (world), entity.getRootVehicle().blockPosition())) {
			ci.cancel();
		}
	}
}
