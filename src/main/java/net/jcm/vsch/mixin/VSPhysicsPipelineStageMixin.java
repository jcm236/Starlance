package net.jcm.vsch.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.jcm.vsch.event.PhysTick;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.core.impl.shadow.Ao;
import org.valkyrienskies.core.impl.shadow.Aq;

import java.util.Map;

/**@deprecated really, really sus vscore reference*/
@Deprecated
@Mixin(value = Aq.class, remap = false)
public class VSPhysicsPipelineStageMixin {
    @Shadow @Final private Map<String, Long2ObjectMap<PhysShipImpl>> h;

    @Inject(method = "a(Lorg/joml/Vector3dc;DZ)Lorg/valkyrienskies/core/impl/shadow/Ao;", at = @At("TAIL"))
    public void physTickHook(Vector3dc par1, double par2, boolean par3, CallbackInfoReturnable<Ao> cir) {
        PhysTick.apply(h);
    }
}
