package net.jcm.vsch.accessor;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

public interface FreeRotatePlayerAccessor {
	boolean vsch$shouldFreeRotate();

	boolean vsch$hasSupportingBlock();

	EntityDimensions vsch$getVanillaDimensions(Pose pose);
}
