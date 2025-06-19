package net.jcm.vsch.api.block;

import net.minecraft.world.phys.BlockHitResult;

public interface IVentBlock {
	boolean canThrustPass(final BlockHitResult hitResult);
}
