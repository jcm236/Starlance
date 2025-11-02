package net.jcm.vsch.mixin.cosmos;

import net.jcm.vsch.config.VSCHCommonConfig;
import net.lointain.cosmos.procedures.TempoProcedure;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TempoProcedure.class)
public class MixinTempoProcedure {
	@Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, remap = false)
	private static void execute(final Event event, final Entity entity, final CallbackInfo ci) {
		if (VSCHCommonConfig.SUPRESSESS_PRESS_Y_HINT.get()) {
			ci.cancel();
		}
	}
}
