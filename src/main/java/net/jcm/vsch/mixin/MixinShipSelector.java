package net.jcm.vsch.mixin;

import net.jcm.vsch.util.ShipAllocator;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.ShipSelector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShipSelector.class)
public class MixinShipSelector {
	@ModifyArg(method = "select", remap = false, at = @At(value = "INVOKE", target = "Lkotlin/collections/CollectionsKt;asSequence(Ljava/lang/Iterable;)Lkotlin/sequences/Sequence;"))
	private Iterable<Ship> selectIterable(Iterable<Ship> ships) {
		return new ShipAllocator.SafeShipIterable(ships);
	}
}
