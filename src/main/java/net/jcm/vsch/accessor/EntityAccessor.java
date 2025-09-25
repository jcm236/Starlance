package net.jcm.vsch.accessor;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface EntityAccessor {
	Vec3 vsch$collide(Vec3 movement);
	void vsch$checkInsideBlocks();
	void vsch$onInsideBlock(BlockState block);
}
