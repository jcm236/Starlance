package net.jcm.vsch.mixin.valkyrienskies;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.entity.player.MultiPartPlayer;

import net.minecraft.world.entity.Entity;

import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.stream.StreamSupport;

@Mixin(EntityShipCollisionUtils.class)
public class MixinEntityShipCollisionUtils {
	@ModifyExpressionValue(
		method = "getShipPolygonsCollidingWithEntity",
		at = @At(
			value = "INVOKE",
			target = "Lorg/valkyrienskies/core/api/ships/QueryableShipData;getIntersecting(Lorg/joml/primitives/AABBdc;)Ljava/lang/Iterable;"
		),
		remap = false
	)
	private Iterable<LoadedShip> getShipPolygonsCollidingWithEntity$getIntersecting(
		final Iterable<LoadedShip> ships,
		@Local(argsOnly = true) final Entity entity
	) {
		if (entity instanceof final FreeRotatePlayerAccessor frp && frp.vsch$isFreeRotating()) {
			return List.of();
		}
		return ships;
	}
}
