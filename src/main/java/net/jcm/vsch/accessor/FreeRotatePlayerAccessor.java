package net.jcm.vsch.accessor;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3d;

public interface FreeRotatePlayerAccessor extends LivingEntityAccessor {
	EntityDimensions vsch$getVanillaDimensions(Pose pose);

	boolean vsch$isFreeRotating();

	Vec3 vsch$getHeadCenter();

	Vec3 vsch$getFeetPosition();

	default Vec3 vsch$getDownVector() {
		if (!this.vsch$isFreeRotating()) {
			return new Vec3(0, -1, 0);
		}
		final Vector3d posY = this.vsch$getBodyRotation().transformUnitPositiveY(new Vector3d());
		return new Vec3(-posY.x, -posY.y, -posY.z);
	}

	Quaternionf vsch$getBodyRotation();

	void vsch$setBodyRotation(Quaternionf rotation);

	Quaternionf vsch$getBodyRotationO();

	void vsch$setBodyRotationO(Quaternionf rotation);

	void vsch$setLerpBodyRotation(Quaternionf rotation);

	Quaternionf vsch$getHeadRotation();

	Quaternionf vsch$getHeadRotationO();

	float vsch$getHeadPitch();

	void vsch$setHeadPitch(float pitch);

	float vsch$getHeadPitchO();

	void vsch$setLerpHeadPitch(float pitch);

	float vsch$getHeadYaw();

	void vsch$setHeadYaw(float yaw);

	float vsch$getHeadYawO();

	void vsch$setLerpHeadYaw(float yaw);

	boolean vsch$hasSupportingBlock();

	void vsch$setOldPosAndRot();

	void vsch$stepLerp(int steps);
}
