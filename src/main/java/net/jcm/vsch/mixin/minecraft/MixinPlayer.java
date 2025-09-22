package net.jcm.vsch.mixin.minecraft;

import net.jcm.vsch.accessor.EntityAccessor;
import net.jcm.vsch.accessor.FreeRotatePlayerAccessor;
import net.jcm.vsch.client.ClientEvents;
import net.jcm.vsch.client.VSCHKeyBindings;
import net.jcm.vsch.config.VSCHClientConfig;
import net.jcm.vsch.config.VSCHConfig;
import net.jcm.vsch.entity.player.MultiPartPlayer;
import net.jcm.vsch.items.custom.MagnetBootItem;
import net.jcm.vsch.util.BooleanRef;
import net.jcm.vsch.util.CollisionUtil;
import net.jcm.vsch.util.VSCHUtils;
import net.jcm.vsch.util.wapi.LevelData;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Team;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(Player.class)
public abstract class MixinPlayer extends LivingEntity implements FreeRotatePlayerAccessor {
	@Shadow
	@Final
	private static Map<Pose, EntityDimensions> POSES;

	@Unique
	private static final float SPACE_ENTITY_SIZE = 0.6f;
	@Unique
	private static final float HALF_SPACE_ENTITY_SIZE = SPACE_ENTITY_SIZE / 2;
	@Unique
	private static final EntityDimensions SPACE_ENTITY_DIM = EntityDimensions.scalable(SPACE_ENTITY_SIZE, SPACE_ENTITY_SIZE);
	@Unique
	private static final double SUPPORT_CHECK_DISTANCE = 0.1;
	@Unique
	private static final EntityDataAccessor<Boolean> FREE_ROTATION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
	@Unique
	private static final float BODY_ROT_CLAMP = 50 * Mth.DEG_TO_RAD;

	@Unique
	private boolean wasFreeRotating = false;
	@Unique
	private Quaternionf rotation = new Quaternionf();
	@Unique
	private Quaternionf rotationO = new Quaternionf();
	@Unique
	private Quaternionf rotationLerp = new Quaternionf();
	@Unique
	private Quaternionf headRotation = new Quaternionf();
	@Unique
	private Quaternionf headRotationO = new Quaternionf();
	@Unique
	private float headPitch = 0;
	@Unique
	private float headPitchO = 0;
	@Unique
	private float headPitchLerp = 0;
	@Unique
	private float headYaw = 0;
	@Unique
	private float headYawO = 0;
	@Unique
	private float headYawLerp = 0;
	@Unique
	private MultiPartPlayer[] parts;
	@Unique
	private MultiPartPlayer chestPart;
	@Unique
	private MultiPartPlayer feetPart;
	@Unique
	private Pose oldPose;
	@Unique
	private long lastRollTime;
	@Unique
	private int jumpCD = 0;
	@Unique
	private float nextStep = 0;

	protected MixinPlayer() {
		super(null, null);
	}

	@Shadow
	public abstract Abilities getAbilities();

	@Shadow
	protected abstract boolean isStayingOnGroundSurface();

	@Inject(method = "<init>", at = @At("RETURN"))
	public void postInit(final Level level, final BlockPos pos, final float yRot, final GameProfile profile, final CallbackInfo ci) {
		final Player player = (Player)((Object)(this));

		this.chestPart = new MultiPartPlayer(player, SPACE_ENTITY_SIZE, false);
		this.feetPart = new MultiPartPlayer(player, SPACE_ENTITY_SIZE, true);
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
	public Vec3 vsch$getHeadCenter() {
		if (!this.vsch$isFreeRotating()) {
			return this.getEyePosition();
		}
		return this.position().add(0, SPACE_ENTITY_SIZE / 2, 0);
	}

	@Override
	public Vec3 vsch$getFeetPosition() {
		if (!this.vsch$isFreeRotating()) {
			return this.position();
		}
		return this.vsch$getHeadCenter().add(this.vsch$getDownVector().scale(SPACE_ENTITY_SIZE * 2));
	}

	@Override
	public Quaternionf vsch$getBodyRotation() {
		return this.rotation;
	}

	@Override
	public void vsch$setBodyRotation(final Quaternionf rotation) {
		if (this.rotation != rotation) {
			this.rotation.set(rotation);
		}
		if (!this.vsch$isFreeRotating()) {
			return;
		}
		final Vector3f angles = this.rotation.getEulerAnglesYXZ(new Vector3f());
		this.yBodyRot = -this.rotation.getEulerAnglesYXZ(new Vector3f()).y * Mth.RAD_TO_DEG;
		this.reCalcHeadRotation();
	}

	@Override
	public Quaternionf vsch$getBodyRotationO() {
		return this.rotationO;
	}

	@Override
	public void vsch$setBodyRotationO(final Quaternionf rotation) {
		if (this.rotationO != rotation) {
			this.rotationO.set(rotation);
		}
		if (!this.vsch$isFreeRotating()) {
			return;
		}
		final Vector3f angles = this.rotationO.getEulerAnglesYXZ(new Vector3f());
		this.xRotO = angles.x * Mth.RAD_TO_DEG;
		this.yBodyRotO = -angles.y * Mth.RAD_TO_DEG;
		this.reCalcHeadRotationO();
	}

	@Override
	public void vsch$setLerpBodyRotation(final Quaternionf rotation) {
		this.rotationLerp.set(rotation);
	}

	@Override
	public Quaternionf vsch$getHeadRotation() {
		return this.headRotation;
	}

	@Override
	public Quaternionf vsch$getHeadRotationO() {
		return this.headRotationO;
	}

	@Unique
	private void reCalcHeadRotation() {
		this.headRotation.set(this.rotation).rotateY(this.headYaw).rotateX(this.headPitch);
		final Vector3f angles = this.headRotation.getEulerAnglesYXZ(new Vector3f());
		super.setXRot(angles.x * Mth.RAD_TO_DEG);
		final float yRot = -angles.y * Mth.RAD_TO_DEG;
		super.setYRot(yRot);
		this.yHeadRot = yRot;
	}

	@Unique
	private void reCalcHeadRotationO() {
		this.headRotationO.set(this.rotationO).rotateY(this.headYawO).rotateX(this.headPitchO);
		final Vector3f angles = this.headRotationO.getEulerAnglesYXZ(new Vector3f());
		this.xRotO = angles.x * Mth.RAD_TO_DEG;
		this.yHeadRotO = this.yRotO = -angles.y * Mth.RAD_TO_DEG;
	}

	@Override
	public float getXRot() {
		return this.vsch$isFreeRotating()
			? this.vsch$getHeadRotation().getEulerAnglesYXZ(new Vector3f()).x * Mth.RAD_TO_DEG
			: super.getXRot();
	}

	@Override
	public void setXRot(final float xRot) {
		if (!this.vsch$isFreeRotating()) {
			super.setXRot(xRot);
		}
	}

	@Override
	public float getYRot() {
		return this.vsch$isFreeRotating()
			? -this.vsch$getHeadRotation().getEulerAnglesYXZ(new Vector3f()).y * Mth.RAD_TO_DEG
			: super.getYRot();
	}

	@Override
	public void setYRot(final float yRot) {
		if (!this.vsch$isFreeRotating()) {
			super.setYRot(yRot);
		}
	}

	@Override
	public float getViewXRot(final float partialTick) {
		final Vector3f angles = new Vector3f();
		final float xRot = this.vsch$getHeadRotation().getEulerAnglesYXZ(angles).x * Mth.RAD_TO_DEG;
		final float xRotO = this.vsch$getHeadRotationO().getEulerAnglesYXZ(angles).x * Mth.RAD_TO_DEG;
		return Mth.lerp(partialTick, xRotO, xRot);
	}

	@Override
	public float getViewYRot(final float partialTick) {
		final Vector3f angles = new Vector3f();
		final float yRot = -this.vsch$getHeadRotation().getEulerAnglesYXZ(angles).y * Mth.RAD_TO_DEG;
		final float yRotO = -this.vsch$getHeadRotationO().getEulerAnglesYXZ(angles).y * Mth.RAD_TO_DEG;
		return Mth.lerp(partialTick, yRotO, yRot);
	}

	@Override
	public float vsch$getHeadPitch() {
		return this.headPitch;
	}

	@Override
	public void vsch$setHeadPitch(final float pitch) {
		this.headPitch = pitch;
	}

	@Override
	public float vsch$getHeadPitchO() {
		return this.headPitchO;
	}

	@Override
	public void vsch$setLerpHeadPitch(final float pitch) {
		this.headPitchLerp = pitch;
	}

	@Override
	public float vsch$getHeadYaw() {
		return this.headYaw;
	}

	@Override
	public void vsch$setHeadYaw(final float yaw) {
		this.headYaw = yaw;
	}

	@Override
	public float vsch$getHeadYawO() {
		return this.headYawO;
	}

	@Override
	public void vsch$setLerpHeadYaw(final float yaw) {
		this.headYawLerp = yaw;
	}

	@Unique
	private Vector3d vsch$getRelativeDeltaMovement() {
		final Vec3 movement = this.getDeltaMovement();
		return this.vsch$getBodyRotation().transformInverse(new Vector3d(movement.x, movement.y, movement.z));
	}

	@Unique
	private void reCalcRotation() {
		if (this.firstTick || !this.vsch$isFreeRotating()) {
			return;
		}
		final Quaternionf rotation = this.vsch$getBodyRotation();
		final Vector3f oldAngles = rotation.getEulerAnglesYXZ(new Vector3f());
		this.headPitch = 0;
		this.headYaw = 0;
		this.vsch$setBodyRotation(rotation.rotationYXZ(Mth.DEG_TO_RAD * -super.getYRot(), Mth.DEG_TO_RAD * super.getXRot(), oldAngles.z));
	}

	@Unique
	protected void updateDefaultFreeRotation() {
		if (this.level().isClientSide) {
			return;
		}
		final boolean freeRotation = VSCHConfig.PLAYER_FREE_ROTATION_IN_SPACE.get() && !this.isPassenger() && LevelData.get(this.level()).isSpace();
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
		this.headPitchO = this.headPitch = 0;
		this.headYawO = this.headYaw = data.getFloat("HeadYaw");
		this.reCalcHeadRotation();
		this.reCalcHeadRotationO();
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	public void addAdditionalSaveData(final CompoundTag data, final CallbackInfo ci) {
		data.put("RotationQuat", this.newFloatList(this.rotation.x, this.rotation.y, this.rotation.z, this.rotation.w));
		data.putFloat("HeadYaw", this.headYaw);
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

	@Override
	public boolean onGround() {
		if (!this.vsch$isFreeRotating()) {
			return super.onGround();
		}
		// TODO: fix on ground check
		return false;
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
		return this.onGround();
	}

	@Unique
	private boolean isBodyRotationLocked() {
		return MagnetBootItem.isMagnetized(this);
	}

	@Override
	public void turn(final double x, final double y) {
		if (!this.vsch$isFreeRotating()) {
			super.turn(x, y);
			return;
		}

		final boolean isClientSide = this.level().isClientSide;

		float roll = 0;
		boolean lockHeadRotate = false;
		final boolean lockBodyRotate = this.isBodyRotationLocked();
		if (isClientSide) {
			if (VSCHKeyBindings.UNLOCK_HEAD_ROTATION.consumeDoubleClick()) {
				this.headPitch = 0;
				this.reCalcHeadRotation();
				return;
			}
			if (!lockBodyRotate) {
				lockHeadRotate = !VSCHKeyBindings.UNLOCK_HEAD_ROTATION.isDown();
				int rollDir = 0;
				if (VSCHKeyBindings.ROLL_CLOCKWISE.isDown()) {
					rollDir++;
				}
				if (VSCHKeyBindings.ROLL_COUNTER_CLOCKWISE.isDown()) {
					rollDir--;
				}
				long now = System.nanoTime();
				roll = rollDir * VSCHClientConfig.PLAYER_ROLL_SPEED.get().floatValue() * Math.min((float) ((now - this.lastRollTime) / 1.0e9), 0.1f) * Mth.DEG_TO_RAD;
				this.lastRollTime = now;
			}
		}

		if (x == 0 && y == 0 && roll == 0) {
			return;
		}
		final float yaw = -(float)(x) * 0.15f * Mth.DEG_TO_RAD;
		final float pitch = (float)(y) * 0.15f * Mth.DEG_TO_RAD;

		final Quaternionf relRotation = new Quaternionf();
		float newHeadYaw = this.headYaw + yaw;
		if (newHeadYaw > BODY_ROT_CLAMP) {
			relRotation.rotationY(newHeadYaw - BODY_ROT_CLAMP);
			newHeadYaw = BODY_ROT_CLAMP;
		} else if (newHeadYaw < -BODY_ROT_CLAMP) {
			relRotation.rotationY(newHeadYaw + BODY_ROT_CLAMP);
			newHeadYaw = -BODY_ROT_CLAMP;
		}
		this.headYawO += newHeadYaw - this.headYaw;
		this.headYaw = newHeadYaw;
		if (lockHeadRotate) {
			if (!lockBodyRotate) {
				relRotation.rotateX(pitch);
			}
		} else {
			float newHeadPitch = this.headPitch + pitch;
			if (newHeadPitch > Mth.HALF_PI) {
				if (!lockBodyRotate) {
					relRotation.rotateX(newHeadPitch - Mth.HALF_PI);
				}
				newHeadPitch = Mth.HALF_PI;
			} else if (newHeadPitch < -Mth.HALF_PI) {
				if (!lockBodyRotate) {
					relRotation.rotateX(newHeadPitch + Mth.HALF_PI);
				}
				newHeadPitch = -Mth.HALF_PI;
			}
			this.headPitchO += newHeadPitch - this.headPitch;
			this.headPitch = newHeadPitch;
		}
		if (!lockBodyRotate) {
			relRotation.rotateZ(roll);
		}

		this.vsch$setBodyRotation(this.vsch$getBodyRotation().mul(relRotation).normalize());
		this.vsch$setBodyRotationO(this.rotationO.mul(relRotation).normalize());
	}

	@Override
	public void absMoveTo(final double x, final double y, final double z, final float yRot, final float xRot) {
		if (!this.vsch$isFreeRotating()) {
			super.absMoveTo(x, y, z, yRot, xRot);
			return;
		}
		this.absMoveTo(x, y, z);
		// super.setYRot(yRot % 360f);
		// super.setXRot(xRot % 360f);
		// this.reCalcRotation();
		this.vsch$setBodyRotationO(this.vsch$getBodyRotation());
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
	public void vsch$setOldPosAndRot() {
		this.headPitchO = this.headPitch;
		this.headYawO = this.headYaw;
		this.vsch$setBodyRotationO(this.vsch$getBodyRotation());
	}

	@Override
	public void vsch$stepLerp(final int steps) {
		this.headPitch += (this.headPitchLerp - this.headPitch) / steps;
		this.headYaw += (this.headYawLerp - this.headYaw) / steps;
		this.vsch$setBodyRotation(this.vsch$getBodyRotation().nlerp(this.rotationLerp, 1.0f / steps).normalize());
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
		if (!this.vsch$isFreeRotating()) {
			super.checkInsideBlocks();
			return;
		}
	}

	@Override
	protected void checkSupportingBlock(final boolean onGround, final Vec3 movement) {
		if (!onGround) {
			super.checkSupportingBlock(onGround, movement);
			return;
		}
		final AABB bb = this.getBoundingBox();
		final AABB movedBB = movement == null
			? bb
			: bb.expandTowards(Math.signum(movement.x) * 1e-6, Math.signum(movement.y) * 1e-6, Math.signum(movement.z) * 1e-6);
		Optional<BlockPos> supportingBlockPos = this.level().findSupportingBlock(this, movedBB);
		if (!supportingBlockPos.isPresent() && movement != null) {
			supportingBlockPos = this.level().findSupportingBlock(this, movedBB.move(-movement.x, -movement.y, -movement.z));
		}
		this.mainSupportingBlockPos = supportingBlockPos;
	}

	@Override
	protected BlockPos getOnPos(final float dist) {
		final BlockPos mainSupportingBlockPos = this.mainSupportingBlockPos.orElse(null);
		if (mainSupportingBlockPos != null) {
			return mainSupportingBlockPos;
		}
		final Vector3d rel = this.vsch$getBodyRotation().transform(new Vector3d(0, -HALF_SPACE_ENTITY_SIZE / 2 - dist, 0));
		return BlockPos.containing(this.vsch$getFeetPosition().add(rel.x, rel.y, rel.z));
	}

	@Override
	public boolean isInLava() {
		return super.isInLava() || this.chestPart.isInLava() || this.feetPart.isInLava();
	}

	@Override
	public boolean isInWater() {
		return super.isInWater() || this.chestPart.isInWater() || this.feetPart.isInWater();
	}

	@Override
	public boolean isInWaterRainOrBubble() {
		return super.isInWaterRainOrBubble() || this.chestPart.isInWaterRainOrBubble() || this.feetPart.isInWaterRainOrBubble();
	}

	@Override
	public boolean isInWaterOrBubble() {
		return super.isInWaterOrBubble() || this.chestPart.isInWaterOrBubble() || this.feetPart.isInWaterOrBubble();
	}

	@Override
	public boolean isFallFlying() {
		return this.vsch$isFreeRotating() ? false : super.isFallFlying();
	}

	@Override
	public Vec3 handleRelativeFrictionAndCalculateMovement(final Vec3 movement, final float friction) {
		if (!this.vsch$isFreeRotating()) {
			return super.handleRelativeFrictionAndCalculateMovement(movement, friction);
		}
		final float power = this.getFlyingSpeed();
		final double speed = movement.lengthSqr();
		if (speed > 1e-8) {
			final Vector3d move = new Vector3d(movement.x, movement.y, movement.z);
			if (speed > 1) {
				move.normalize();
			}
			final float yawSpeed = this.headYaw * 0.3f;
			if (Math.abs(yawSpeed) < 1e-6) {
				this.headYaw = 0;
			} else {
				this.headYaw -= yawSpeed;
				this.vsch$setBodyRotation(this.vsch$getBodyRotation().rotateY(yawSpeed));
			}
			final boolean bodyLocked = this.isBodyRotationLocked();
			final Quaternionf rotation = bodyLocked
				? (this.headYaw == 0 ? this.vsch$getBodyRotation() : this.vsch$getBodyRotation().rotateY(this.headYaw, new Quaternionf()))
				: this.vsch$getHeadRotation();
			move.mul(power);
			rotation.transform(move);
			this.setDeltaMovement(this.getDeltaMovement().add(move.x, move.y, move.z));
			if (bodyLocked && this.jumping && !this.isStayingOnGroundSurface() /*&& this.onGround()*/ && this.jumpCD == 0) {
				final Vec3 dm0 = this.getDeltaMovement();
				final Vector3d dm = rotation.transformInverse(new Vector3d(dm0.x, dm0.y, dm0.z));
				dm.y = this.getJumpPower();
				rotation.transform(dm);
				this.setDeltaMovement(new Vec3(dm.x, dm.y, dm.z));
				this.jumpCD = 10;
			}
		}
		// this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
		this.move(MoverType.SELF, this.getDeltaMovement());
		return this.getDeltaMovement();
	}

	@Override
	public void move(final MoverType moverType, Vec3 movement) {
		if (!this.vsch$isFreeRotating()) {
			super.move(moverType, movement);
			return;
		}
		if (this.noPhysics) {
			this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
			return;
		}

		this.wasOnFire = this.isOnFire();

		if (moverType == MoverType.PISTON) {
			movement = this.limitPistonMovement(movement);
			if (movement.equals(Vec3.ZERO)) {
				return;
			}
		}

		System.out.println("moving: " + this + ": " + moverType + " : " + movement);
		if (this.stuckSpeedMultiplier.lengthSqr() > 1e-7) {
			movement = movement.multiply(this.stuckSpeedMultiplier);
			this.stuckSpeedMultiplier = Vec3.ZERO;
			this.setDeltaMovement(Vec3.ZERO);
		}
		movement = this.maybeBackOffFromEdge(movement, moverType);

		final Quaternionf rotation = this.vsch$getBodyRotation();
		final Vector3d movementWill = rotation.transformInverse(new Vector3d(movement.x, movement.y, movement.z));
		final Vec3 movementWillVec = new Vec3(movementWill.x, movementWill.y, movementWill.z);
		movement = this.betterCollide(movement);
		final Vector3d movementActual = rotation.transformInverse(new Vector3d(movement.x, movement.y, movement.z));
		final double movedDist = movement.length();
		if (movedDist > 1e-4) {
			// TODO: reset fallDistance
			this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
		}
		final boolean collideX = Math.abs(movementWill.x - movementActual.x) > 1e-5;
		final boolean collideZ = Math.abs(movementWill.z - movementActual.z) > 1e-5;
		final boolean collideY = Math.abs(movementWill.y - movementActual.y) > 1e-6;
		this.horizontalCollision = collideX || collideZ;
		this.verticalCollision = collideY;
		this.verticalCollisionBelow = collideY && movementWill.y < 0;
		// TODO: detect minorHorizontalCollision
		this.minorHorizontalCollision = this.horizontalCollision && false;
		this.setOnGroundWithKnownMovement(this.verticalCollisionBelow, movement);
		final BlockPos groundPos = this.getOnPosLegacy();
		final BlockState groundState = this.level().getBlockState(groundPos);
		this.checkFallDamage(movementActual.y, this.onGround(), groundState, groundPos);
		if (this.isRemoved()) {
			return;
		}
		if (this.horizontalCollision) {
			final Vector3d dm = this.vsch$getRelativeDeltaMovement();
			if (collideX) {
				dm.x = 0;
			}
			if (collideZ) {
				dm.z = 0;
			}
			rotation.transform(dm);
			this.setDeltaMovement(dm.x, dm.y, dm.z);
		}
		if (collideY) {
			// TODO: Block.updateEntityAfterFallOn
		}
		if (this.onGround()) {
			// TODO: Block.stepOn
		}
		final Entity.MovementEmission movementEmission = this.getMovementEmission();
		if (movementEmission.emitsAnything() && !this.isPassenger()) {
			// TODO
			final float scaledMoveDist = (float) (movedDist * 0.6);
			this.flyDist += scaledMoveDist;
			this.walkDist += scaledMoveDist;
			this.moveDist += scaledMoveDist;
			final BlockPos steppingPos = this.getOnPos();
			final BlockState stepping = this.level().getBlockState(steppingPos);
			if (!stepping.isAir() && this.moveDist > this.nextStep) {
				final boolean steppingOnGround = steppingPos.equals(groundPos);
				boolean stepped = this.vibrationAndSoundEffectsFromBlock(groundPos, groundState, movementEmission.emitsSounds(), steppingOnGround, movementWillVec);
				if (!steppingOnGround) {
					stepped |= this.vibrationAndSoundEffectsFromBlock(steppingPos, stepping, false, movementEmission.emitsEvents(), movementWillVec);
				}
				if (stepped) {
					this.nextStep = this.moveDist + 1;
				} else if (this.isInWater()) {
					this.nextStep = this.moveDist + 1;
					if (movementEmission.emitsSounds()) {
						this.waterSwimSound();
					}
					if (movementEmission.emitsEvents()) {
						this.gameEvent(GameEvent.SWIM);
					}
				}
			}
		}
		this.tryCheckInsideBlocks();
		if (
			this.level().getBlockStatesIfLoaded(this.getBoundingBox().deflate(1e-6))
				.noneMatch((state) -> state.is(BlockTags.FIRE) || state.is(Blocks.LAVA))
		) {
			if (this.getRemainingFireTicks() <= 0) {
				this.setRemainingFireTicks(-this.getFireImmuneTicks());
			}
			if (this.wasOnFire && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
				this.playEntityOnFireExtinguishedSound();
			}
		}
		if (this.isOnFire() && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
			this.setRemainingFireTicks(-this.getFireImmuneTicks());
		}
	}

	@Unique
	private Vec3 betterCollide(Vec3 movement) {
		final Level level = this.level();
		System.out.println("colliding: " + movement);
		final Vec3 position = this.vsch$getHeadCenter();
		final EntityDimensions dimensions = this.vsch$getVanillaDimensions(this.getPose());
		final AABBd box = new AABBd(
			-dimensions.width / 2, 0.6 / 2 - dimensions.height, -dimensions.width / 2,
			dimensions.width / 2, 0.6 / 2, dimensions.width / 2
		);
		final Matrix4dc entityToWorld = new Matrix4d()
			.translation(position.x, position.y, position.z)
			.rotate(this.vsch$getBodyRotation());
		final Matrix4dc worldToEntity = entityToWorld.invert(new Matrix4d());
		final Vector3d newMovement = new Vector3d(movement.x, movement.y, movement.z);
		worldToEntity.transformDirection(newMovement);
		final AABBd checkBox = CollisionUtil.expandTowards(new AABBd(box), newMovement).transform(entityToWorld);
		System.out.println("box: " + box + " -> " + checkBox);

		CollisionUtil.checkCollision(newMovement, level, this, box, worldToEntity, entityToWorld);

		final Matrix4d entityToShip = new Matrix4d();
		final Matrix4d shipToEntity = new Matrix4d();
		final String dimId = VSGameUtilsKt.getDimensionId(level);
		for (final LoadedShip ship : VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips()) {
			if (!ship.getChunkClaimDimension().equals(dimId)) {
				continue;
			}
			if (!ship.getWorldAABB().intersectsAABB(checkBox)) {
				continue;
			}
			entityToShip.set(ship.getWorldToShip()).mul(entityToWorld);
			shipToEntity.set(worldToEntity).mul(ship.getShipToWorld());
			CollisionUtil.checkCollision(newMovement, level, this, box, shipToEntity, entityToShip);
		}

		if (Math.abs(newMovement.x) < 1e-7) {
			newMovement.x = 0;
		}
		if (Math.abs(newMovement.y) < 1e-7) {
			newMovement.y = 0;
		}
		if (Math.abs(newMovement.z) < 1e-7) {
			newMovement.z = 0;
		}
		entityToWorld.transformDirection(newMovement);
		if (Math.abs(newMovement.x) < 1e-7) {
			newMovement.x = 0;
		}
		if (Math.abs(newMovement.y) < 1e-7) {
			newMovement.y = 0;
		}
		if (Math.abs(newMovement.z) < 1e-7) {
			newMovement.z = 0;
		}
		movement = new Vec3(newMovement.x, newMovement.y, newMovement.z);
		System.out.println("final movement: " + movement);
		return movement;
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
	protected void moveTowardsClosestSpace(final double x, final double y, final double z) {
		if (!this.vsch$isFreeRotating()) {
			super.moveTowardsClosestSpace(x, y, z);
		}
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
		cir.setReturnValue(SPACE_ENTITY_SIZE / 2);
		// TODO: fix eye height after rotated
		// final float height = this.vsch$getVanillaDimensions(pose).height;
		// float eyeHeight = cir.getReturnValueF();
		// eyeHeight += SPACE_ENTITY_SIZE - height;
		// cir.setReturnValue(eyeHeight);
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
	protected void pushEntities() {
		if (!this.vsch$isFreeRotating()) {
			super.pushEntities();
			return;
		}
		final Level level = this.level();
		final Team selfTeam = this.getTeam();
		final Team.CollisionRule selfTeamRule = selfTeam == null ? Team.CollisionRule.ALWAYS : selfTeam.getCollisionRule();
		if (selfTeamRule == Team.CollisionRule.NEVER) {
			return;
		}
		Predicate<Entity> selector = EntitySelector.NO_SPECTATORS
			.and(Entity::isPushable)
			.and((e) -> {
				final Team team = e.getTeam();
				final Team.CollisionRule teamRule = team == null ? Team.CollisionRule.ALWAYS : team.getCollisionRule();
				if (teamRule == Team.CollisionRule.NEVER) {
					return false;
				}
				final boolean sameTeam = selfTeam != null && selfTeam.isAlliedTo(team);
				if (sameTeam) {
					return selfTeamRule != Team.CollisionRule.PUSH_OWN_TEAM && teamRule != Team.CollisionRule.PUSH_OWN_TEAM;
				}
				return selfTeamRule != Team.CollisionRule.PUSH_OTHER_TEAMS && teamRule != Team.CollisionRule.PUSH_OTHER_TEAMS;
			});
		// if (level.isClientSide()) {
		// 	selector = selector.and((e) -> e instanceof Player || e instanceof MultiPartPlayer);
		// }
		for (final Entity part : new Entity[]{this, this.chestPart, this.feetPart}) {
			level.getEntities(
				part,
				part.getBoundingBox(),
				selector
			)
				.forEach((e) -> push3Dim(part, e));
		}
	}

	@Override
	public void baseTick() {
		if (this.firstTick) {
			this.updateDefaultFreeRotation();
		}
		if (this.jumpCD > 0) {
			this.jumpCD--;
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
		this.vsch$setOldPosAndRot();
	}

	@Inject(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;updatePlayerPose()V", ordinal = 0)
	)
	public void tick$updatePlayerPose(final CallbackInfo ci) {
		this.updateParts();
	}

	@WrapOperation(
		method = "aiStep",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z")
		),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
			ordinal = 0
		)
	)
	public List<Entity> aiStep$getEntities$touch(
		final Level level,
		final Entity self,
		AABB box,
		final Operation<List<Entity>> operation
	) {
		for (final MultiPartPlayer part : this.parts) {
			box = box.minmax(part.getBoundingBox().inflate(0.5, 0.5, 0.5));
		}
		return operation.call(level, self, box);
	}

	@Unique
	private void updateParts() {
		final float height = this.vsch$getVanillaDimensions(this.getPose()).height;
		final Vector3f feetPos = new Vector3f(0, SPACE_ENTITY_SIZE - height, 0);
		feetPos.rotate(this.vsch$getBodyRotation());
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

	@Unique
	private static void push3Dim(final Entity e1, final Entity e2) {
		if (e1.noPhysics || e2.noPhysics || e1.isPassengerOfSameVehicle(e2)) {
			return;
		}
		final AABB e1Box = e1.getBoundingBox();
		final AABB e2Box = e2.getBoundingBox();
		final Vector3d movement = new Vector3d(
			e1.getX() - e2.getX(),
			(e1.getY() + e1Box.getYsize() / 2) - (e2.getY() + e2Box.getYsize() / 2),
			e1.getZ() - e2.getZ()
		);
		final double lengthSqr = movement.lengthSquared();
		if (lengthSqr < 1e-4) {
			return;
		}
		movement.normalize(Math.min(1 / Math.sqrt(lengthSqr), 0.2)).mul(1.0 / 20);
		if (!e1.isVehicle() && e1.isPushable()) {
			e1.push(movement.x, movement.y, movement.z);
		}
		if (!e2.isVehicle() && e2.isPushable()) {
			e2.push(-movement.x, -movement.y, -movement.z);
		}
	}

	@Unique
	private static boolean isStateClimbable(final BlockState state) {
		return state.is(BlockTags.CLIMBABLE) || state.is(Blocks.POWDER_SNOW);
	}

	@Unique
	private boolean vibrationAndSoundEffectsFromBlock(
		final BlockPos pos, final BlockState state,
		final boolean playSound, final boolean emitEvent,
		final Vec3 movement
	) {
		if (state.isAir()) {
			return false;
		}
		if (this.onGround() || isStateClimbable(state) || this.isCrouching() && Math.abs(movement.y) < 1e-6 || this.isOnRails()) {
			if (playSound) {
				this.playStepSound(pos, state);
			}
			if (emitEvent) {
				this.level().gameEvent(GameEvent.STEP, this.position(), GameEvent.Context.of(this, state));
			}
			return true;
		}
		return false;
	}
}
