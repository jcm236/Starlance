package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.entity.player.MultiPartPlayer;
import net.jcm.vsch.util.BooleanRef;
import net.jcm.vsch.util.VSCHUtils;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
	private static final double SUPPORT_CHECK_DISTANCE = 0.1;
	@Unique
	private static final EntityDataAccessor<Boolean> FREE_ROTATION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

	@Unique
	private boolean wasFreeRotating = false;
	@Unique
	private Quaternionf rotation = new Quaternionf();
	@Unique
	private Quaternionf rotationO = new Quaternionf();
	@Unique
	private Quaternionf rotationLerp = new Quaternionf();
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

	@Shadow
	public abstract Abilities getAbilities();

	@Inject(method = "<init>", at = @At("RETURN"))
	public void postInit(final Level level, final BlockPos pos, final float yRot, final GameProfile profile, final CallbackInfo ci) {
		final Player player = (Player)((Object)(this));

		this.chestPart = new MultiPartPlayer(player, SPACE_ENTITY_SIZE);
		this.feetPart = new MultiPartPlayer(player, SPACE_ENTITY_SIZE);
		this.parts = new MultiPartPlayer[]{this.chestPart, this.feetPart};
		this.oldPose = this.getPose();
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void defineSynchedData(final CallbackInfo ci) {
		this.entityData.define(FREE_ROTATION_ID, false);
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
	public boolean vsch$isFreeRotating() {
		return !this.firstTick && this.entityData.get(FREE_ROTATION_ID);
	}

	@Override
	public Quaternionf vsch$getRotation() {
		return this.rotation;
	}

	@Override
	public void vsch$setRotation(final Quaternionf rotation) {
		if (this.rotation != rotation) {
			this.rotation.set(rotation);
		}
		if (!this.vsch$isFreeRotating()) {
			return;
		}
		final Vector3f angles = this.rotation.getEulerAnglesYXZ(new Vector3f());
		super.setXRot(angles.x * Mth.RAD_TO_DEG);
		final float yRot = -angles.y * Mth.RAD_TO_DEG;
		super.setYRot(yRot);
		this.yHeadRot = this.yBodyRot = yRot;
	}

	@Override
	public Quaternionf vsch$getRotationO() {
		return this.rotationO;
	}

	@Override
	public void vsch$setRotationO(final Quaternionf rotation) {
		if (this.rotationO != rotation) {
			this.rotationO.set(rotation);
		}
		if (!this.vsch$isFreeRotating()) {
			return;
		}
		final Vector3f angles = this.rotationO.getEulerAnglesYXZ(new Vector3f());
		this.xRotO = angles.x * Mth.RAD_TO_DEG;
		this.yHeadRotO = this.yBodyRotO = this.yRotO = -angles.y * Mth.RAD_TO_DEG;
	}

	@Override
	public Quaternionf vsch$getLerpRotation() {
		return this.rotationLerp;
	}

	@Override
	public void vsch$setLerpRotation(final Quaternionf rotation) {
		this.rotationLerp.set(rotation);
	}

	@Override
	public float getYRot() {
		return this.vsch$isFreeRotating() ? -this.rotation.getEulerAnglesYXZ(new Vector3f()).y * Mth.RAD_TO_DEG : super.getYRot();
	}

	@Override
	public void setYRot(final float yRot) {
		if (!this.vsch$isFreeRotating()) {
			super.setYRot(yRot);
		}
	}

	@Override
	public float getXRot() {
		return this.vsch$isFreeRotating() ? this.rotation.getEulerAnglesYXZ(new Vector3f()).x * Mth.RAD_TO_DEG : super.getXRot();
	}

	@Override
	public void setXRot(final float xRot) {
		if (!this.vsch$isFreeRotating()) {
			super.setXRot(xRot);
		}
	}

	@Override
	protected void setRot(final float yRot, final float xRot) {
		super.setYRot(yRot % 360f);
		super.setXRot(xRot % 360f);
	}

	@Unique
	private void reCalcRotation() {
		if (this.firstTick || !this.vsch$isFreeRotating()) {
			return;
		}
		final Quaternionf rotation = this.vsch$getRotation();
		final Vector3f oldAngles = rotation.getEulerAnglesYXZ(new Vector3f());
		this.vsch$setRotation(rotation.rotationYXZ(Mth.DEG_TO_RAD * -super.getYRot(), Mth.DEG_TO_RAD * super.getXRot(), oldAngles.z));
	}

	@Unique
	protected void updateDefaultFreeRotation() {
		final boolean freeRotation = !this.isPassenger() && VSCHUtils.isSpaceLevel(this.level());
		this.entityData.set(FREE_ROTATION_ID, freeRotation);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	public void readAdditionalSaveData(final CompoundTag data, final CallbackInfo ci) {
		if (data.contains("RotationQuat", Tag.TAG_LIST)) {
			final ListTag rotQuat = data.getList("RotationQuat", Tag.TAG_FLOAT);
			this.rotation.set(rotQuat.getFloat(0), rotQuat.getFloat(1), rotQuat.getFloat(2), rotQuat.getFloat(3));
			if (!this.rotation.normalize().isFinite()) {
				this.rotation.set(0, 0, 0, 1);
			}
			this.rotationO.set(this.rotation);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	public void addAdditionalSaveData(final CompoundTag data, final CallbackInfo ci) {
		data.put("RotationQuat", this.newFloatList(this.rotation.x, this.rotation.y, this.rotation.z, this.rotation.w));
	}

	@Override
	public EntityDimensions vsch$getVanillaDimensions(final Pose pose) {
		return POSES.getOrDefault(pose, Player.STANDING_DIMENSIONS);
	}

	@Override
	public void setPose(final Pose newPose) {
		super.setPose(newPose);
		if (!this.vsch$isFreeRotating()) {
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

	@WrapOperation(
		method = "updatePlayerPose",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isShiftKeyDown()Z")
	)
	protected boolean updatePlayerPose$isShiftKeyDown(final Player self, final Operation<Boolean> operation) {
		if (!operation.call(self)) {
			return false;
		}
		if (this.getAbilities().flying || !this.vsch$isFreeRotating()) {
			return true;
		}
		// TODO: fix crouching pose
		// return this.onGround();
		return false;
	}

	@Override
	public void turn(final double x, final double y) {
		if (!this.vsch$isFreeRotating()) {
			super.turn(x, y);
			return;
		}
		if (x == 0 && y == 0) {
			return;
		}
		final float yaw = -(float)(x) * 0.15f * Mth.DEG_TO_RAD;
		final float pitch = (float)(y) * 0.15f * Mth.DEG_TO_RAD;

		final Quaternionf pitchRot = new Quaternionf().rotateX(pitch);
		final Quaternionf yawRot = new Quaternionf().rotateY(yaw);

		this.vsch$setRotation(this.vsch$getRotation().mul(yawRot).mul(pitchRot).normalize());
		this.vsch$setRotationO(this.rotationO.mul(yawRot).mul(pitchRot).normalize());
	}

	@Override
	public void absMoveTo(final double x, final double y, final double z, final float yRot, final float xRot) {
		if (!this.vsch$isFreeRotating()) {
			super.absMoveTo(x, y, z, yRot, xRot);
			return;
		}
		this.absMoveTo(x, y, z);
		super.setYRot(yRot % 360f);
		super.setXRot(xRot % 360f);
		// this.reCalcRotation();
		this.vsch$setRotationO(this.vsch$getRotation());
	}

	@Override
	public void moveTo(final double x, final double y, final double z, final float yRot, final float xRot) {
		if (!this.vsch$isFreeRotating()) {
			super.moveTo(x, y, z, yRot, xRot);
			return;
		}
		this.setPosRaw(x, y, z);
		super.setYRot(yRot);
		super.setXRot(xRot);
		this.reCalcRotation();
		this.setOldPosAndRot();
		this.reapplyPosition();
	}

	@Override
	public boolean shouldDiscardFriction() {
		return super.shouldDiscardFriction() || !this.getAbilities().flying && this.vsch$isFreeRotating();
	}

	@Override
	public boolean vsch$hasSupportingBlock() {
		if (!this.vsch$isFreeRotating()) {
			return false;
		}
		final Level level = this.level();
		final BooleanRef hasSupport = new BooleanRef(false);
		final Entity[] parts = new Entity[]{this, this.chestPart, this.feetPart};
		for (final Entity part : parts) {
			VSGameUtilsKt.transformFromWorldToNearbyShipsAndWorld(level, part.getBoundingBox(), (box) -> {
				if (hasSupport.value) {
					return;
				}
				for (final VoxelShape shape : level.getBlockCollisions(part, box.inflate(SUPPORT_CHECK_DISTANCE))) {
					if (!shape.isEmpty()) {
						hasSupport.value = true;
						return;
					}
				}
			});
			if (hasSupport.value) {
				break;
			}
		}
		return hasSupport.value;
	}

	@Override
	protected void checkInsideBlocks() {
		super.checkInsideBlocks();
		((EntityAccessor)(this.chestPart)).vsch$checkInsideBlocks();
		((EntityAccessor)(this.feetPart)).vsch$checkInsideBlocks();
	}

	@Override
	public boolean isInLava() {
		return super.isInLava() || this.chestPart.isInLava() || this.feetPart.isInLava();
	}

	@Override
	public void moveRelative(final float power, final Vec3 movement) {
		if (!this.vsch$isFreeRotating()) {
			super.moveRelative(power, movement);
			return;
		}
		final double speed = movement.lengthSqr();
		if (speed < 1.0e-7) {
			return;
		}
		final Vector3d move = new Vector3d(movement.x, movement.y, movement.z);
		if (speed > 1) {
			move.normalize();
		}
		this.vsch$getRotation().transform(move.mul(power));
		this.setDeltaMovement(this.getDeltaMovement().add(move.x, move.y, move.z));
	}

	@Override
	protected void checkFallDamage(final double dy, final boolean onGround, final BlockState block, final BlockPos pos) {
		if (!this.vsch$isFreeRotating()) {
			super.checkFallDamage(dy, onGround, block, pos);
			return;
		}
		// TODO: implement fall damage / collision damage in space
	}

	@Override
	public boolean startRiding(final Entity vehicle, final boolean force) {
		final boolean ok = super.startRiding(vehicle, force);
		if (ok) {
			this.updateDefaultFreeRotation();
		}
		return ok;
	}

	@Inject(method = "removeVehicle", at = @At("RETURN"))
	public void removeVehicle(final CallbackInfo ci) {
		this.updateDefaultFreeRotation();
	}

	@Override
	public void dismountTo(final double x, final double y, final double z) {
		if (!this.vsch$isFreeRotating()) {
			super.dismountTo(x, y, z);
			return;
		}
		final float oldHeight = this.vsch$getVanillaDimensions(this.getPose()).height;
		final float newHeight = SPACE_ENTITY_SIZE;
		super.dismountTo(x, y + oldHeight - newHeight, z);
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return super.getBoundingBoxForCulling().minmax(this.chestPart.getBoundingBoxForCulling()).minmax(this.feetPart.getBoundingBoxForCulling());
	}

	@Inject(method = "getStandingEyeHeight", at = @At("RETURN"), cancellable = true)
	public void getStandingEyeHeight(final Pose pose, final EntityDimensions dimension, final CallbackInfoReturnable<Float> cir) {
		if (!this.vsch$isFreeRotating()) {
			return;
		}
		final float height = this.vsch$getVanillaDimensions(pose).height;
		float eyeHeight = cir.getReturnValueF();
		eyeHeight += SPACE_ENTITY_SIZE - height;
		cir.setReturnValue(eyeHeight);
	}

	@Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
	public void getDimensions(final Pose pose, final CallbackInfoReturnable<EntityDimensions> cir) {
		if (this.vsch$isFreeRotating()) {
			cir.setReturnValue(SPACE_ENTITY_DIM);
		}
	}

	@Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
	protected void maybeBackOffFromEdge(final Vec3 movement, final MoverType moveType, final CallbackInfoReturnable<Vec3> cir) {
		if (!this.vsch$isFreeRotating()) {
			return;
		}
		// TODO: implement edge backoff in space, may respect to delta movement
		cir.setReturnValue(movement);
	}

	@Override
	protected void setLevel(final Level level) {
		super.setLevel(level);
		this.updateDefaultFreeRotation();
	}

	@Override
	public void baseTick() {
		if (this.firstTick) {
			this.updateDefaultFreeRotation();
		}
		super.baseTick();
		final boolean freeRotation = this.vsch$isFreeRotating();
		if (freeRotation != this.wasFreeRotating) {
			this.wasFreeRotating = freeRotation;
			this.refreshDimensions();
			if (freeRotation) {
				this.reCalcRotation();
			}
		}
		this.vsch$setRotationO(this.vsch$getRotation());
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;updatePlayerPose()V", ordinal = 0))
	public void tick(final CallbackInfo ci) {
		this.updateParts();
	}

	@Unique
	private void updateParts() {
		this.setYBodyRot(this.getYHeadRot());
		final float height = this.vsch$getVanillaDimensions(this.getPose()).height;
		final Vector3f feetPos = new Vector3f(0, SPACE_ENTITY_SIZE - height, 0);
		feetPos.rotate(this.vsch$getRotation());
		this.updatePartPos(this.feetPart, feetPos.x, feetPos.y, feetPos.z);
		this.updatePartPos(this.chestPart, feetPos.x / 2, feetPos.y / 2, feetPos.z / 2);
	}

	@Unique
	private void updatePartPos(final MultiPartPlayer part, final double dx, final double dy, final double dz) {
		Vec3 pos = this.position();
		if (this.vsch$isFreeRotating()) {
			pos = pos.add(dx, dy, dz);
		}
		part.setOldPosAndRot();
		part.setPos(pos);
	}
}
