package net.jcm.vsch.mixin.cosmos;

import net.jcm.vsch.config.VSCHConfig;
import net.lointain.cosmos.procedures.RayrendererProcedure;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(RayrendererProcedure.class)
public class MixinRayrendererProcedure {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DD)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void execute(Event event, LevelAccessor world, double partialTick, double ticks, CallbackInfo ci) {
        if (VSCHConfig.DISABLE_DETONATOR_RENDER.get()) {
            ci.cancel();
        }
    }
}