package net.jcm.vsch.accessor;

import org.joml.Quaternionf;

public interface EntityRotationPacketAccessor {
	Quaternionf vsch$rotation();
	float vsch$getHeadPitch();
	void vsch$setHeadPitch(float pitch);
	float vsch$getHeadYaw();
	void vsch$setHeadYaw(float yaw);
}
