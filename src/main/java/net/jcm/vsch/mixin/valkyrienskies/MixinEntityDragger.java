package net.jcm.vsch.mixin.valkyrienskies;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4d;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.util.EntityDragger;
import org.valkyrienskies.mod.common.util.EntityDraggingInformation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityDragger.class)
public class MixinEntityDragger {
	@Unique
	private static final Vector3dc ZERO_VEC3 = new Vector3d();

	@WrapOperation(
		method = "dragEntitiesWithShips",
		at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/mod/common/util/EntityDraggingInformation;getLastShipStoodOn()Ljava/lang/Long;"),
		remap = false
	)
	public Long dragEntitiesWithShips$getLastShipStoodOn(
		final EntityDraggingInformation dragInfo,
		final Operation<Long> operation,
		@Local final Entity entity
	) {
		if (!(entity instanceof final FreeRotatePlayerAccessor frp) || !frp.vsch$isFreeRotating()) {
			return operation.call(dragInfo);
		}
		final Vector3dc lastMovement = dragInfo.getAddedMovementLastTick();
		final Ship ship = frp.vsch$getSupportingShip();
		if (ship == null) {
			if (!lastMovement.equals(ZERO_VEC3)) {
				entity.setDeltaMovement(entity.getDeltaMovement().add(lastMovement.x(), lastMovement.y(), lastMovement.z()));
				dragInfo.setAddedMovementLastTick(ZERO_VEC3);
			}
			return null;
		}
		final ShipTransform transform = ship.getTransform();
		final ShipTransform prevTransform = ship.getPrevTickTransform();
		final Matrix4d old2new = transform.getShipToWorld().mul(prevTransform.getWorldToShip(), new Matrix4d());
		frp.vsch$setBodyRotation(new Quaternionf().setFromUnnormalized(old2new).mul(frp.vsch$getBodyRotation()).normalize());
		final Vec3 feetPos = frp.vsch$getFeetPosition();
		final Vector3d newFeetPos = old2new.transformPosition(new Vector3d(feetPos.x, feetPos.y, feetPos.z));
		frp.vsch$setFeetPosition(newFeetPos.x, newFeetPos.y, newFeetPos.z);
		final Vector3d movement = newFeetPos.sub(feetPos.x, feetPos.y, feetPos.z);
		dragInfo.setAddedMovementLastTick(movement);
		return null;
	}
}
