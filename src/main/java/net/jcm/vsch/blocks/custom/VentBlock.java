package net.jcm.vsch.blocks.custom;

import net.jcm.vsch.blocks.entity.DockerBlockEntity;
import net.jcm.vsch.api.block.IVentBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class VentBlock extends Block implements IVentBlock {
	public VentBlock(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canThrustPass(final BlockHitResult hitResult) {
		return true;
	}
}
