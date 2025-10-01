package net.jcm.vsch.mixin.cosmos;

import net.lointain.cosmos.procedures.EgraphicProcedure;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EgraphicProcedure.class)
public class MixinEgraphicProcedure {
	@Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;)V", at = @At("HEAD"), cancellable = true, remap = false)
	private static void execute(Event event, LevelAccessor world, CallbackInfo ci) {
		// Never released feature that clogs up the FPS
		ci.cancel();
	}
}
