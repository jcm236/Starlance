package net.jcm.vsch.ship;

import net.minecraft.core.BlockPos;

import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.world.PhysLevel;

public interface IVSCHForceApplier {
	void applyForces(BlockPos pos, PhysShip ship, PhysLevel physLevel);
}
