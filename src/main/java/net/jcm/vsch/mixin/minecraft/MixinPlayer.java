package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.entity.player.MultiPartPlayer;
import net.jcm.vsch.util.VSCHUtils;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(Player.class)
public abstract class MixinPlayer extends LivingEntity implements FreeRotatePlayerAccessor {
	@Shadow
	@Final
	private static Map<Pose, EntityDimensions> POSES;

	@Unique
	private static final float SPACE_ENTITY_SIZE = 0.6f;
	@Unique
	private static final EntityDimensions SPACE_ENTITY_DIM = EntityDimensions.scalable(SPACE_ENTITY_SIZE, SPACE_ENTITY_SIZE);

	@Unique
	private MultiPartPlayer[] parts;
	@Unique
	private MultiPartPlayer chestPart;
	@Unique
	private MultiPartPlayer feetPart;
	@Unique
	private Pose oldPose;

	protected MixinPlayer() {
		super(null, null);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	public void postInit(final Level level, final BlockPos pos, final float yRot, final GameProfile profile, final CallbackInfo ci) {
		final Player player = (Player)((Object)(this));
		this.chestPart = new MultiPartPlayer(player, SPACE_ENTITY_SIZE);
		this.feetPart = new MultiPartPlayer(player, SPACE_ENTITY_SIZE);
		this.parts = new MultiPartPlayer[]{this.chestPart, this.feetPart};
		this.oldPose = this.getPose();
		this.refreshDimensions();
	}

	@Override
	public boolean isMultipartEntity() {
		return true;
	}

	@Override
	public MultiPartPlayer[] getParts() {
		return this.parts;
	}

	@Override
	public boolean vsch$shouldFreeRotate() {
		return !this.isPassenger() && VSCHUtils.isSpaceLevel(this.level());
	}

	@Override
	public EntityDimensions vsch$getVanillaDimensions(final Pose pose) {
		return POSES.getOrDefault(pose, Player.STANDING_DIMENSIONS);
	}

	@Override
	public void setPose(final Pose newPose) {
		super.setPose(newPose);
		if (!this.vsch$shouldFreeRotate()) {
			return;
		}
		if (!this.level().isClientSide) {
			return;
		}
		final Pose oldPose = this.oldPose;
		if (oldPose == newPose) {
			return;
		}
		this.oldPose = newPose;
		final float oldHeight = this.vsch$getVanillaDimensions(oldPose).height;
		final float newHeight = this.vsch$getVanillaDimensions(newPose).height;
		final Vec3 pos = this.position();
		double dx = 0, dy = newHeight - oldHeight, dz = 0;
		this.setPos(pos.x + dx, pos.y + dy, pos.z + dz);
	}

	@Override
	public boolean startRiding(final Entity vehicle, final boolean force) {
		final boolean ok = super.startRiding(vehicle, force);
		if (ok) {
			this.refreshDimensions();
		}
		return ok;
	}

	@Inject(method = "removeVehicle", at = @At("RETURN"))
	public void removeVehicle(final CallbackInfo ci) {
		this.refreshDimensions();
	}

	@Override
	public void dismountTo(final double x, final double y, final double z) {
		final float oldHeight = this.vsch$getVanillaDimensions(this.getPose()).height;
		final float newHeight = SPACE_ENTITY_SIZE;
		super.dismountTo(x, y + oldHeight - newHeight, z);
	}

	@Override
	public boolean isInLava() {
		return super.isInLava() || this.chestPart.isInLava() || this.feetPart.isInLava();
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return super.getBoundingBoxForCulling().minmax(this.chestPart.getBoundingBoxForCulling()).minmax(this.feetPart.getBoundingBoxForCulling());
	}

	@Inject(method = "getStandingEyeHeight", at = @At("RETURN"), cancellable = true)
	public void getStandingEyeHeight(final Pose pose, final EntityDimensions dimension, final CallbackInfoReturnable<Float> cir) {
		if (!this.vsch$shouldFreeRotate()) {
			return;
		}
		final float height = this.vsch$getVanillaDimensions(pose).height;
		float eyeHeight = cir.getReturnValueF();
		eyeHeight += SPACE_ENTITY_SIZE - height;
		cir.setReturnValue(eyeHeight);
	}

	@Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
	public void getDimensions(final Pose pose, final CallbackInfoReturnable<EntityDimensions> cir) {
		if (this.vsch$shouldFreeRotate()) {
			cir.setReturnValue(SPACE_ENTITY_DIM);
		}
	}

	@Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
	protected void maybeBackOffFromEdge(final Vec3 movement, final MoverType moveType, final CallbackInfoReturnable<Vec3> cir) {
		if (!this.vsch$shouldFreeRotate()) {
			return;
		}
		// TODO: implement edge backoff in space, may respect to delta movement
		cir.setReturnValue(movement);
	}

	@Override
	protected void checkFallDamage(final double dy, final boolean onGround, final BlockState block, final BlockPos pos) {
		// TODO: implement fall damage / collision damage in space
	}

	@Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSpeed(F)V", ordinal = 0))
	public void aiStep(final CallbackInfo ci) {
		final float height = this.vsch$getVanillaDimensions(this.getPose()).height;
		double feetX = 0;
		double feetY = SPACE_ENTITY_SIZE - height;
		double feetZ = 0;
		this.updatePartPos(this.chestPart, feetX / 2, feetY / 2, feetZ / 2);
		this.updatePartPos(this.feetPart, feetX, feetY, feetZ);
	}

	@Unique
	private void updatePartPos(final MultiPartPlayer part, final double dx, final double dy, final double dz) {
		Vec3 pos = this.position();
		if (this.vsch$shouldFreeRotate()) {
			pos = pos.add(dx, dy, dz);
		}
		part.setOldPosAndRot();
		part.setPos(pos);
	}
}
