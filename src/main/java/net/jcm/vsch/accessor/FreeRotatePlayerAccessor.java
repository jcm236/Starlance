package net.jcm.vsch.accessor;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

import org.joml.Quaternionf;

public interface FreeRotatePlayerAccessor {
	EntityDimensions vsch$getVanillaDimensions(Pose pose);

	boolean vsch$isFreeRotating();

	Quaternionf vsch$getRotation();

	void vsch$setRotation(Quaternionf rotation);

	Quaternionf vsch$getRotationO();

	void vsch$setRotationO(Quaternionf rotation);

	void vsch$setLerpRotation(Quaternionf rotation);

	float vsch$getHeadPitch();

	void vsch$setLerpHeadPitch(float pitch);

	boolean vsch$hasSupportingBlock();

	void vsch$setOldPosAndRot();

	void vsch$stepLerp(int steps);
}
