package net.jcm.vsch.ship;

import net.minecraft.core.BlockPos;

import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

public interface IVSCHForceApplier {
    void applyForces(BlockPos pos, PhysShipImpl physShip);
}
