package net.jcm.vsch.accessor;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3d;

public interface FreeRotatePlayerAccessor {
	EntityDimensions vsch$getVanillaDimensions(Pose pose);

	boolean vsch$isFreeRotating();

	Vec3 vsch$getFeetPosition();

	default Vec3 vsch$getDownVector() {
		if (!this.vsch$isFreeRotating()) {
			return new Vec3(0, -1, 0);
		}
		final Vector3d posY = this.vsch$getRotation().transformUnitPositiveY(new Vector3d());
		return new Vec3(-posY.x, -posY.y, -posY.z);
	}

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
