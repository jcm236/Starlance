package net.jcm.vsch.blocks.entity;

import net.jcm.vsch.blocks.custom.template.BlockEntityWithEntity;
import net.jcm.vsch.entity.MagnetEntity;
import net.jcm.vsch.entity.VSCHEntities;
import net.jcm.vsch.ship.VSCHForceInducedShips;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MagnetBlockEntity extends BlockEntityWithEntity<MagnetEntity> {

	public MagnetBlockEntity(BlockPos pos, BlockState state) {
		super(VSCHBlockEntities.DRAG_INDUCER_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	public void tickForce(ServerLevel level, BlockPos pos, BlockState state) {
		this.spawnLinkedEntityIfNeeded();
		super.tickForce(level, pos, state);

		// ----- Add this block to the force appliers for the current level ----- //

		int signal = level.getBestNeighborSignal(pos);
		VSCHForceInducedShips ships = VSCHForceInducedShips.get(level, pos);

		if (ships != null) {
			/*if (ships.getDraggerAtPos(pos) == null) {
				ships.addDragger(pos, new DraggerData(signal > 0));
			}*/
		}
	}

	@Override
	public MagnetEntity createLinkedEntity(ServerLevel level, BlockPos pos) {
		return new MagnetEntity(VSCHEntities.MAGNET_ENTITY.get(), level, pos);
	}
}
