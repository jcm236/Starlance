package net.jcm.vsch.accessor;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

import org.joml.Quaternionf;

public interface FreeRotatePlayerAccessor {
	boolean vsch$isFreeRotating();

	Quaternionf vsch$getRotation();

	void vsch$setRotation(Quaternionf rotation);

	Quaternionf vsch$getRotationO();

	void vsch$setRotationO(Quaternionf rotation);

	Quaternionf vsch$getLerpRotation();

	void vsch$setLerpRotation(Quaternionf rotation);

	boolean vsch$hasSupportingBlock();

	EntityDimensions vsch$getVanillaDimensions(Pose pose);
}
