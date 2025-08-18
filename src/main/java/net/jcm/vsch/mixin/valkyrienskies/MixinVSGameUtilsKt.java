package net.jcm.vsch.mixin.valkyrienskies;

import net.minecraft.resources.ResourceLocation;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.valkyrienskies.core.api.world.LevelYRange;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.jcm.vsch.VSCHMod;
import net.minecraft.world.level.Level;

@Mixin(VSGameUtilsKt.class)
public class MixinVSGameUtilsKt {
	@Unique
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	@Inject(method = "getYRange", remap = false, at = @At("HEAD"), cancellable = true)
	private static void getYRange(@NotNull Level level, CallbackInfoReturnable<LevelYRange> cir) {
		ResourceLocation dimension = level.dimension().location();
		if (dimension.getNamespace().equals("cosmos")) {
			// LOGGER.debug("[starlance]: Found cosmos dimension: {}. Changing its world height!", dimension);
			cir.setReturnValue(new LevelYRange(-64, 319));
			cir.cancel();
		}
	}
}
