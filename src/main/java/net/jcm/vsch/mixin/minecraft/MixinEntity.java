package net.jcm.vsch.mixin.minecraft;

import com.llamalad7.mixinextras.sugar.Local;
import net.jcm.vsch.ducks.IEntityDuck;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntityDuck {
    @Shadow public abstract void moveTo(double pX, double pY, double pZ, float pYRot, float pXRot);

    @Shadow protected abstract void teleportPassengers();

    @Shadow public abstract void setYHeadRot(float pYHeadRot);

    @Shadow public abstract void unRide();

    @Shadow public abstract EntityType<?> getType();

    @Shadow public abstract void setRemoved(Entity.RemovalReason pRemovalReason);

    @Shadow @Nullable private Entity vehicle;

    @Unique
    private Entity vsch$weirdEntity;

    public Entity vsch$getNewEntity() {
        return vsch$weirdEntity;
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addDuringTeleport(Lnet/minecraft/world/entity/Entity;)V"))
    private void getNewEntity(ServerLevel pLevel, double pX, double pY, double pZ, Set<RelativeMovement> pRelativeMovements, float pYRot, float pXRot, CallbackInfoReturnable<Boolean> cir, @Local(name = "entity") Entity entity) {
        vsch$weirdEntity = entity;
    }
}
