package net.jcm.vsch.mixin.cosmos;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.lointain.cosmos.procedures.ShipspawnspaceProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ShipspawnspaceProcedure.class)
public class MixinShipspawnspaceProcedure {

    // This does mean we now depend on mixin extras
    @WrapMethod(
            method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            remap = false
    )
    private static void wrapExecute(Event event, LevelAccessor world, double x, double y, double z, Entity entity, Operation<Void> original) {
        try {
            original.call(event, world, x, y, z, entity);
        } catch (Exception cancel) {
            // Seems goofy but it really does stop a crash
            return;
        }
    }
}