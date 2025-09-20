package net.jcm.vsch.accessor;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

import org.joml.Quaternionf;

public interface FreeRotatePlayerAccessor {
	EntityDimensions vsch$getVanillaDimensions(Pose pose);

	boolean vsch$isFreeRotating();

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
