package net.jcm.vsch.mixin.cosmos;

import net.jcm.vsch.VSCHMod;

import net.lointain.cosmos.procedures.ShipspawnspaceProcedure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mixin(ShipspawnspaceProcedure.class)
public class MixinShipspawnspaceProcedure {
	@Unique
	private static final Logger LOGGER = LogManager.getLogger(VSCHMod.MODID);

	@WrapMethod(
		method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
		remap = false
	)
	private static void wrapExecute(Event event, LevelAccessor world, double x, double y, double z, Entity entity, Operation<Void> original) {
		try {
			original.call(event, world, x, y, z, entity);
		} catch (Exception e) {
			// Seems goofy but it really does stop a crash
			LOGGER.error("Caught exception in ShipspawnspaceProcedure:", e);
		}
	}
}
