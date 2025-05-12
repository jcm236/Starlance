package net.jcm.vsch.mixin;

import net.minecraft.resources.ResourceLocation;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.*;

import org.valkyrienskies.core.api.world.LevelYRange;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.jcm.vsch.VSCHMod;
import net.minecraft.world.level.Level;

@Mixin(VSGameUtilsKt.class)
public class MixinVSGameUtilsKt {
	private static final Logger logger = LogManager.getLogger(VSCHMod.MODID);
	@Inject(method = "getYRange", remap = false, at = @At("HEAD"), cancellable = true)
	@NotNull
	private static void getYRange(@NotNull Level level, CallbackInfoReturnable<LevelYRange> cir) {
		ResourceLocation dimension = level.dimension().location();
		if (dimension.getNamespace().equals("cosmos")) {
			// logger.info("[VSCH]: Found cosmos dimension: {}. Changing its world height!", dimension.toString());
			cir.setReturnValue(new LevelYRange(-64, 319));
			cir.cancel();
		}
	}
}
